# Changelog

All notable changes to Snuff AC are documented here.

The format is based on Keep a Changelog, and this project adheres to Semantic
Versioning.

## [1.0.0-dev] - 2026-09-29

First development release. Built from an empty repository, tested on Paper 1.21.11.

### Added

**Engine**

- Multi module Gradle build targeting Java 21, split into a stable public API module, a
  platform neutral core, separate Paper and Velocity platform modules, and a shading and
  packaging module.
- A normalised, immutable packet model. The engine never sees a platform type.
- A dedicated single threaded check executor with deterministic ordering, fed by a queue
  from the network thread. Packet arrival times are captured in nanoseconds at the edge
  so timing information survives the hand off.
- A platform service layer covering scheduling, messaging, permissions and world access.
- Immutable per player world caching, built on the main thread each tick and read without
  locking by the check thread, so no server API call ever happens off thread.

**Movement model**

- A vanilla kinematics model reproducing the documented speeds: 0.2806 blocks per tick
  sprinting, 0.2159 walking, 0.0648 sneaking, terminal velocity 3.92, jump velocity 0.42.
  All of these are asserted as unit tests.
- Attribute driven modelling of movement speed, gravity, jump strength, step height and
  safe fall distance, read from the server each tick.
- Block classification into a platform neutral taxonomy carrying slipperiness,
  passability, climbability and liquid state, so the physics never depends on Bukkit
  materials.
- Input space enumeration with best fit selection, because the server cannot observe which
  keys a player is holding.

**False positive control**

- A tolerance model that accumulates per axis forgiveness from named sources, decays it
  over time, and caps the total. Sources include external pushes, pistons, bouncy blocks,
  item use slowdown, attack slowdown, vehicles, server knockback, explosions, riptide,
  block changes, chunk loads, teleports, setbacks, high latency, low TPS and game mode.
- Leniency carry over, granting a capped fraction of the previous offset as extra
  tolerance on the tick after a flag, which prevents a flag followed by unrestricted
  movement.
- Latency and tick rate adaptive tolerance, both capped, so high ping and low TPS are
  never punished.
- An applicability gate per check so a check is skipped entirely when its assumptions do
  not hold, rather than being allowed to produce misleading evidence.

**Violation system**

- Evidence buffers with proportional adds, decay and capping, kept separate from
  violation levels so short term confidence and accumulated history are distinct.
- Separate alert and violation thresholds, where the alert threshold gates only the
  alert and never the recording of the violation.
- Configurable setback with a required payload and player opt out, dispatched on the
  main thread.
- Configurable punishment actions: none, alert, log, command, setback, kick. Command
  actions honour a threshold and a cooldown.

**Checks, 23 total, all enabled by default**

- Movement: `fly`, `speed`, `nofall`, `airmovement`, `groundspoof`, `step`, `highjump`,
  `longjump`, `impossiblemovement`, `velocity`
- Combat: `reach`, `autoclicker`, `aim`, `killaura`, `impossibleattack`,
  `invalidattackstate`
- World: `fastbreak`, `fastplace`, `scaffold`, `nuker`
- Packet: `badpackets`, `packetspam`, `timer`

**Operations**

- `/snuff` with `info`, `version`, `reload`, `checks`, `toggle`, `debug`, `alerts`,
  `violations`, `profile`, `setback` and `stats`, with tab completion.
- Template driven alerting with placeholders, per player rate limiting, configurable
  verbosity, and per player alert toggles.
- Daily violation log files with retention based pruning.
- Debug output exposing position, velocity, ground state, air time, ping, tick rate,
  tolerance and per check state.
- Four permissions: `snuffac.admin`, `snuffac.debug`, `snuffac.alerts`, `snuffac.bypass`.
- Configuration split into `config.yml` for global settings and `checks.yml` for per
  check settings, with toggle changes persisted.

**Platform support**

- Paper 1.21.11: full check set, tested end to end against a real server.
- Purpur: same build, detected at runtime, no Purpur specific code.
- Velocity 3.4.0: packet category and network timing, with world and combat geometry
  checks reported inactive because a proxy has neither block data nor player positions.

**Developer API**

- A separate `snuffac-api` artifact exposing the engine handle, a read only check list,
  violation information and a listener interface.

**Testing**

- 91 unit tests covering kinematics against documented vanilla speeds, position packing,
  angle wrapping, buffers, the tolerance model, configuration, the check registry,
  prediction accuracy and input recovery, and end to end engine behaviour including
  exemptions, disabled checks and immediate flags.

**Documentation**

- `README.md`, `credits.md`, `docs/architecture.md`, this changelog, and configuration
  guidance.

### Verified on Paper 1.21.11

Tested against Paper build 132 on Java 21, with a scripted client driving real movement
packets.

- Plugin enables cleanly, PacketEvents 2.14.0 loads, 23 checks register, zero errors.
- Player tracking, latency measurement, alerts, the violation log and the file logger all
  work.
- Legitimate walking, sprinting and sprint jumping produced **zero** violations, which is
  the false positive result that matters most.
- Simulated flight was detected by `fly` with evidence including air time, delta and
  velocity, and the violation level escalated 4.0 to 8.0 to 12.0, at which point the
  setback threshold was reached and the level reset. The full alert to evidence to
  setback to reset path is confirmed working.
- Commands, tab completion, configuration reload, check toggling and persistence verified.

### Known limitations

- Detection was validated against a scripted client, not against a range of real cheat
  clients, and not against high latency, packet loss or low tick rate conditions. Thresholds
  are conservative starting points and will need tuning against real traffic.
- The world cache reads the server's world rather than a replica of the client's
  believed world, so client side block prediction is forgiven through the tolerance model
  rather than modelled directly.
- Break time uses a coarse material hardness and tool speed table, not full per block
  tool tier accuracy.
- The predictor does not yet model the post 1.8.2 skipped tick behaviour, which is a
  known source of both false negatives and false positives.
- No automatic bans. Alerts, logging and setbacks only, by deliberate choice until the
  detection system is further validated.
- Velocity support is limited to packet level checks, as a proxy has no world data and no
  player position API.
- Folia is not supported.

### Licence

Licensed under the GNU General Public License v3.0, required because the bundled
PacketEvents library is GPLv3. The reasoning and the licence of every dependency and every
researched project are documented in `credits.md`.

### Research

All 12 requested anticheat projects were inspected and their licences read before use.
Five are GPLv3, two are MIT, four carry no licence at all and were deliberately not read
because no permission to reuse their work exists, and one (Hades) is no longer publicly
available. No source code was copied from any project.
