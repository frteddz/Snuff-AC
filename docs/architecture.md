# Architecture

This document explains how Snuff AC is put together and, more importantly, why it is
put together that way.

## The central design constraint

`snuffac-core` must not know that Bukkit exists.

This is not architecture for its own sake. An anticheat is a system that has to be
correct, has to be testable, and has to keep working when the server software underneath
it changes. Three things follow directly from keeping the core free of platform types:

1. **Checks can be unit tested with no server.** Every test in this project runs in
   milliseconds because the core has no startup cost and no external dependency.
2. **The Velocity port is possible at all.** Velocity and Paper share nothing except the
   Minecraft protocol, so anything shared must be expressed in terms neither owns.
3. **Version upgrades touch one module.** When the protocol changes, the change lands in
   the translator, not in twenty checks.

So the boundary is not an interface with a `Platform` suffix. It is a **data** boundary:
the platform converts foreign packet wrappers into Snuff AC's own immutable records at
the edge, and the engine only ever sees those records.

```
   Paper / Velocity
        |
        |  PacketEvents wrappers (version specific, platform specific)
        v
   DefaultPacketTranslator  --->  MovementPacket, AttackPacket, BlockBreakPacket ...
        |                              (immutable, platform neutral)
        v
   SnuffCore.enqueue(playerId, packet)
        |
        v
   dedicated check thread  --->  CheckDispatcher  --->  Check instances
                                                            |
                                                            v
                                                   ViolationHandler
                                                            |
                          +-------------------+-------------+-------------+
                          |                   |             |             |
                       alerts              logs         setbacks       API
```

## Threading

Three execution contexts, with a strict rule about which may touch what.

**Packet thread (Netty).** The platform listener runs here. It does exactly three things:
decode the wrapper, build an immutable Snuff AC record, stamp the arrival time in
nanoseconds, and enqueue. It performs no detection, calls no check, and never touches
the server API. This keeps the highest frequency path in the system as small and as
predictable as possible.

**Check thread (one dedicated daemon thread).** Drains the queue and runs all detection.
Single threaded and therefore deterministic: a packet is always processed in arrival
order against a consistent view of player state, which makes the whole engine testable
by calling `processQueue()` and reading the result.

**Main thread (server tick).** Two responsibilities only: refreshing the cached world and
attribute data each tick, and applying setbacks. It is the only context allowed to call
into the server API.

The reason detection is not run on the Netty thread, as some anticheats do, is
testability and reasoning. Running on a single controlled thread means check execution
order is fixed, state transitions are inspectable, and a bug is reproducible. The
nanosecond arrival timestamp captured on the Netty thread preserves the timing
information that lag compensation actually needs, so nothing important is lost by
deferring the work.

### Why the world is cached instead of queried

Checks need to know what is under the player's feet. Asking Bukkit that from the check
thread would be a thread safety violation and would be slow. So each tick, on the main
thread, the platform builds an immutable `PlayerWorldCache`: a radius of blocks around
the player, each with its physical classification and hardness, plus the slipperiness of
the block below and whether it is a support block.

The check thread reads a reference to a finished, immutable snapshot. Reading it is a
single volatile read. There is no locking, no Bukkit call off thread, and no chance of
observing a half built cache.

The deliberate trade-off is that this reads the *server's* world rather than a
packet derived replica of the world the *client* believes in. A client side block
prediction means the two can disagree for a tick or two. The tolerance model absorbs
this, via the `BLOCK_CHANGE` and `CHUNK_LOAD` sources. A full replica is the right
long-term answer and is the single biggest planned improvement.

## The tolerance model

`dev.snuffac.core.tolerance.ToleranceModel` answers one question: given everything that
has happened to this player, how much movement deviation should we forgive right now?

It accumulates a `Vec3d` per named `ToleranceSource`, sums them, and clamps the total to
a configured maximum. Each tick every contribution decays by a configurable factor, so
forgiveness is temporary. Sources cover external pushes, pistons, bouncy blocks, item use
slowdown, attack slowdown, vehicles, server knockback, explosions, riptide, block
changes, chunk loads, teleports, setbacks, high latency, low TPS and game mode changes.

The important property is that this is **inspectable**. When a check flags, the
tolerance model can say exactly which source widened the allowance and by how much,
because each source is a separate named entry rather than a number that got baked into a
threshold. That is what makes false positives debuggable rather than merely rare.

**Leniency carry over** is a second, smaller mechanism. When a check flags, the next
prediction gets a fraction of the observed offset granted as extra tolerance, capped so
a cheater cannot bank a large allowance by triggering one big flag and then moving
freely. This directly prevents the "flag once, then cheat" pattern.

## Prediction

`PredictionEngine` models one client movement tick.

For a given starting velocity, environment and attribute set, it enumerates the
discretised input space (36 candidates across forward, strafe, jump and descend), applies
the vanilla kinematic formula to each, and keeps the candidate whose predicted delta is
closest to what the client actually sent. The winning candidate becomes the new velocity.

Two things are worth calling out.

**The physics is verified, not guessed.** The model resolves to 0.2806 blocks per tick
when sprinting on normal ground, 0.2159 walking, 0.0648 sneaking, matching the documented
5.612, 4.317 and 1.295 metres per second exactly. These are asserted as unit tests, so a
future change to the constants cannot silently degrade accuracy.

**The model must not punish legitimate players.** Flat sprinting on ice has a *lower*
terminal speed than on normal ground, because ice trades acceleration for drag; the speed
advantage of ice comes from repeatedly sprint jumping, which is not a terminal speed at
all. The predictor handles this naturally, because the block friction and the environment
are inputs to the simulation rather than hard coded assumptions.

## The violation system

Three independent pieces, deliberately not collapsed into one number.

**Buffer** (`ViolationBuffer`) is short term confidence. Checks add to it, sized by how far
a threshold was exceeded, so a marginal excess costs less than a gross one. It bleeds down
every tick and drains completely on a flag. Crossing its threshold is what turns evidence
into a violation.

**Violation level** (`ViolationLevel`) is accumulated history. It rises by a configured
increment per violation and decays slowly. It is what thresholds like alert and setback
are measured against.

Separating them means a player who is briefly suspicious is not punished, and a player
who is persistently suspicious accumulates history even if each individual moment is
small. This is also why the alert threshold gates only the *alert*: a violation is always
recorded, otherwise the level could never climb past the alert threshold in the first
place. That was a real bug caught by the test suite during development.

**Tolerance** forgives. See above.

## Checks

A check is a stateless singleton implementing `Check`. It declares the packet types it
cares about, and optionally a per player state object created through `createState()`.

Keeping checks stateless and putting per player data behind `createState()` means a
configuration reload can toggle a check without discarding evidence, and it means a check
instance is never accidentally shared between players. `CheckRegistry` builds dispatch
arrays once at startup, so the per packet cost is an array walk, not a listener lookup.

Checks receive a `CheckContext` giving them the player, the configuration, the current
server tick rate, and a `flag` method. A check's job is to decide what is suspicious and
say why. Deciding what to do about it is entirely `ViolationHandler`'s problem.

## Violation handling

`ViolationHandler` is the single place where a detection becomes an action. In order:
record the violation, add to history, notify API listeners, alert if above the alert
threshold, apply a setback if above the setback threshold, run a configured command if
above its threshold, and log. Adding a punishment action later means adding a step here
and nowhere else.

## Developer API

`snuffac-api` is a separate artifact with a deliberately small surface: the engine
handle, a read only check list, violation information, and a listener interface. Third
party plugins can observe violations and read check state without touching internals.

## Testing strategy

The core is testable because the platform is behind an interface and the packet model is
plain data. `EngineIntegrationTest` boots a real `SnuffCore`, registers a player, and
feeds it synthetic packet sequences, which is how the buffer threshold behaviour, the
exemption path and the disabled check path are all covered.

Movement prediction is tested against the documented vanilla speeds rather than against
recorded traces of a real client. Recorded traces remain the right long-term addition and
are not yet present.

## Planned work, in priority order

1. A packet derived world replica, so client side block prediction is modelled rather
   than forgiven.
2. Recorded movement traces from a real client as regression fixtures.
3. Per block tool tier accuracy for break time, replacing the coarse material table.
4. More prediction candidates covering jump plus strafe combinations and the post 1.8.2
   tick skip behaviour.
5. Expanded Velocity capability, including network wide alert delivery and server
   switching awareness.
6. Automatic punishments, only once the detection system is further validated.
