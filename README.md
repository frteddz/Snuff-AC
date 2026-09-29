<div align="center">
  <img src="docs/header.gif" alt="Snuff AC" width="400">
</div>

<br>

# Snuff AC

Snuff AC is a free and open source anticheat for Minecraft Java Edition, targeting
Paper, Purpur and Velocity.

It is an independent implementation. It was developed by studying the architecture of
existing anticheats (documented in full in [`credits.md`](credits.md)) and writing
Snuff AC from scratch. No source code was copied from any other project.

**Status: development release, `1.0.7-dev`.** It is tested on Paper 1.21.11. It is not
yet recommended for production use on a public server. See
[Known limitations](#known-limitations).

## What it is

Snuff AC is built around one idea: a check should produce **evidence**, and several
independent pieces of evidence should be allowed to accumulate before the server acts.

That leads to the two systems that define the project:

- **The tolerance model.** Rather than a single tuned epsilon, Snuff AC tracks *why* a
  player's movement might legitimately differ from the server's expectation. External
  pushes, pistons, slime and ice, item use slowdown, vehicles, knockback, teleports,
  high latency and low server tick rate are all named sources that widen a per axis
  allowance which then decays. A check that cannot explain a discrepancy does not get
  silently ignored, it produces evidence.
- **The evidence buffer.** Checks add to a buffer sized by *how far* a threshold was
  exceeded, rather than a flat amount. The buffer bleeds down while the player is clean.
  Only when it crosses a threshold does the violation level rise, and only when the
  violation level crosses a second threshold does the anticheat alert or set the player
  back.

## Supported platforms

| Platform | Status | Notes |
| --- | --- | --- |
| Paper 1.21.11 | Tested | Primary target. Full check set. |
| Purpur | Compatible | Same build, detected at runtime. No Purpur specific code. |
| Velocity | Compiles, limited | Packet based checks only. See below. |

### Velocity limitations, stated plainly

Velocity is a proxy. It does not have the block data, the entity list or the world state
that movement detection fundamentally needs, and its API exposes no player position at
all. Snuff AC on Velocity therefore runs the **packet** category and the network timing
subsystem, and reports honestly that world and combat geometry checks are inactive. The
architecture supports proxy side work (shared player data, network wide alerts, server
switching awareness) but those are not implemented in this release.

## Requirements

- Java 21
- Paper or Purpur 1.21.11 (or a Velocity 3.4.0 proxy for the packet checks)

## Installation

1. Download the release jar.
2. Drop it into `plugins/` on your Paper or Purpur server.
3. Start the server. `config.yml` and `checks.yml` are generated on first run.
4. Give `snuffac.admin` to your staff.

There is no external dependency to install. The packet library is bundled and relocated
inside the jar.

To run on Velocity, place the same jar in `plugins/` of the proxy. Only the packet
checks will be active.

## Commands

The surface is deliberately small. Bare `/snuff` opens a menu, and the commands below are
entry points, shortcuts and console tools.

| Command | Description |
| --- | --- |
| `/snuff` | Open the staff menu |
| `/snuff version` | Version, platform and check count in one line |
| `/snuff violations [player]` | Open the flagged players menu, or a player's history |
| `/snuff settings` | Open the settings menu: retention, prevention, alert cooldown, warning ladder |
| `/snuff punishments [player]` | Active punishments, online or offline |
| `/snuff warns <player>` | Warning history for a player |
| `/snuff sounds` | Toggle your own menu and command sounds |
| `/snuff alerts` | Toggle your own alerts |
| `/snuff alerts verbose` | Toggle your own verbose alert line |
| `/snuff info` | Version, platform, check count, enabled state, TPS, tracked players |
| `/snuff reload` | Reload `config.yml` and `checks.yml` |
| `/snuff checks` | Every check grouped by category with its on/off state |
| `/snuff toggle <check>` | Enable or disable a check and persist it to `checks.yml` |
| `/snuff debug [player]` | Toggle live debug output for a player, prints current state |
| `/snuff profile [player]` | Protocol version, latency, reach statistics, packet counters |
| `/snuff setback [player]` | Apply a manual setback |
| `/snuff stats` | Tracked player count, total violation level, current and worst TPS |

### Manual punishment

Staff only. No check and no report can reach any of these.

| Command | Description |
| --- | --- |
| `/snuff ban <player> <reason>` | Permanent ban |
| `/snuff tempban <player> <duration> <reason>` | Timed ban |
| `/snuff timeout <player> <duration> <reason>` | Temporary kick |
| `/snuff ipban <player> <reason>` | Ban by address |
| `/snuff tempipban <player> <duration> <reason>` | Timed address ban |
| `/snuff mute <player> <reason>` | Mute chat and commands |
| `/snuff tempmute <player> <duration> <reason>` | Timed mute |
| `/snuff warn <player> <reason>` | Record a warning |
| `/snuff unban` `untempban` `untimeout` `unipban` `untempipban` `unmute` `untempmute` `unwarn` | Exact reverse of each, logging who lifted it |

Durations accept `m`, `h` and `d` in any order and combination, so `1h 10s` and `10s 1h`
are both valid. A reason is mandatory on every punish command. Bans are enforced at pre
login, and everything persists per UUID across restarts.

A mute blocks chat and, by default, every command. `mute.allowed-commands` grants
specific commands back; the default is an empty list, so an owner who configures nothing
keeps the strict behaviour. Names are matched case insensitively with any leading slash
stripped, and a plugin namespace may be given, for example `essentials:home`.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `snuffac.admin` | op | Access to every command. Inherits the rest. |
| `snuffac.menu` | op | Open the staff menus |
| `snuffac.debug` | op | Receive violation alerts, enable debug output |
| `snuffac.alerts` | op | Receive violation alerts |
| `snuffac.bypass` | false | Exempt from all checks |
| `snuffac.exempt.punish` | false | Cannot be punished by staff |
| `snuffac.punish.ban` `tempban` `ipban` `kick` `mute` `warn` | op | The matching manual punishment |
| `snuffac.punish.unban` | op | Lift any punishment |
| `snuffac.punish.maxduration.1h` `1d` `7d` `30d` | false | Cap what a staff member may issue. Junior staff get shorter ones. |

## Alerts

Alerts are produced from a configurable template so they stay useful rather than noisy:

```
[Snuff] PlayerName failed Speed A | VL: 4.7 | ping: 42ms
```

The shipped default is:

```yaml
console-alert-format: '[{prefix}] {player} failed {check} | VL: {vl} | ping: {ping}ms'
```

Available placeholders: `{prefix}`, `{player}`, `{check}`, `{checkKey}`, `{category}`,
`{vl}`, `{buffer}`, `{confidence}`, `{ping}`, `{tps}`, `{detail}`, `{platform}`, plus any
key present in the check's evidence map. Set `console-alert-format-verbose` to a second
template if you want evidence appended to alerts.

Alerts are rate limited per player by `general.alert-cooldown-ms` to avoid flooding.

Every violation is also appended to `plugins/SnuffAC/logs/violations-<date>.log` with
the full evidence map, and is written to the server log at INFO level.

## Known limitations

These are stated plainly rather than hidden, because an anticheat that claims to cover
everything is lying to its users.

**Not detectable from the server, at all**

- X-Ray, block ESP, ore search, entity ESP and storage ESP. These are render time
  predicates over block data a vanilla client already receives. The server sends the
  chunk and the client decides what to draw, so nothing tells the server that a player
  looked through a wall. There is no packet, field or timing signature.
- Fullbright and other purely local rendering changes. Nothing leaves the client.

Snuff AC reduces what the server volunteers through ore obfuscation, and detects some of
the consequences, but detection is not possible and no placeholder check was added to
pretend otherwise.

**Not yet modelled**

- Collision resolution and post 1.8.2 skipped ticks. The predictor does not simulate
  either, so speed related thresholds remain untuned.
- Acknowledged velocity. A client can delay or drop its transaction responses, so
  `VelocityCheck` does not yet know whether it actually received a knockback.
- Per block tool tier accuracy, so break time uses a coarse material table.
- Folia. Not supported. The project targets Paper and Velocity with the plain Bukkit
  scheduler.

**Exempted rather than handled**

- Bedrock players through Geyser and Floodgate, and pre 1.8 protocols through
  ViaVersion. Both are exempted by default because modelling them properly is better
  than risk false positives on them.

**Fixed in `1.0.6-dev`**

The three items listed as broken in `1.0.5-dev` are resolved: menu items can no longer
be taken and the buttons work, the warning ladder can be enabled and persists across a
restart, and spear attribute swapping is detected.

## Configuration

Two files are generated in `plugins/SnuffAC/`:

- **`config.yml`** holds global settings: alerting, tolerance behaviour, setback
  behaviour, permissions, and the shared combat and packet tuning values.
- **`checks.yml`** holds per check settings, grouped by category. Every check exposes
  `enabled`, `buffer-threshold`, `buffer-decay`, `violation-increment`,
  `setback-threshold`, `alert-threshold` and `action`.

`action` accepts `NONE`, `ALERT`, `LOG`, `SETBACK` and `KICK`. An earlier version also
accepted `COMMAND`, and the parser still reads the value for backwards compatibility, but
the code that would have run it has been deleted. Setting `action: COMMAND` today does
nothing at all. Do not use it.

### Key tolerance settings

```yaml
tolerance:
  decay-per-tick: 0.35        # how fast forgiven allowance bleeds away
  maximum: 0.45               # hard cap on total forgiven allowance
  carry-over-cap: 1.0         # after a flag, grant a little extra next tick
  carry-over-retention: 0.4
  base: 0.001
  ping-per-ms: 0.00002        # widen tolerance as latency rises
  ping-maximum: 0.06
  tps-per-miss: 0.004         # widen tolerance when the server is below safe TPS
  tps-maximum: 0.05
  safe-tps: 19.0
```

The latency and tick rate terms exist so that a player is never punished because of
their ping or because the server is struggling. Both are capped.

Player ping is measured every tick on the platform and fed into the model. This was
inert until v1.0.7, so earlier releases made every tolerance decision as if every player
had zero latency.

### The warning ladder

`escalation` controls the only automatic action in the project. It ships **disabled**.

```yaml
escalation:
  enabled: false                 # off until you have tuned your own thresholds
  max-warnings: 5                # floor of 1, so it cannot ban on the first flag
  ban-duration-millis: 3600000
  min-confidence: 0.75           # only act on confident detections
  warn-only: false               # true stops at the limit and never bans
```

Each confident flag records one warning. Reaching the limit applies one timed ban whose
screen names the check, says how many warnings led to it, and tells a falsely detected
player to contact the admins afterwards.

The ladder is keyed by UUID so reconnecting does not clear it, has a cooldown so one
burst of packets cannot spend the whole allowance, and is reset by any manual staff
punishment or unban. It can also be toggled and reconfigured from `/snuff settings`.

**Automatic punishment is off by default.** Every check defaults to `ALERT`. The one
automatic action, the warning ladder under `escalation`, ships disabled and should only
be turned on after thresholds are tuned on your own server, because a false ban is
worse than a missed detection. It can be set to warn without ever banning, and any
manual staff punishment resets it.

Manual punishments (`ban`, `tempban`, `timeout`, `ipban`, `mute`, `warn` and their
reverses) are staff only. No check and no report can reach them.

## Checks

31 checks, all enabled by default.

**Movement**: `fly`, `speed`, `nofall`, `airmovement`, `groundspoof`, `groundflag`,
`pitchlock`, `drift`, `step`, `highjump`, `longjump`, `impossiblemovement`, `velocity`

**Combat**: `reach`, `autoclicker`, `aim`, `killaura`, `critical`,
`rotationsnapback`, `impossibleattack`, `invalidattackstate`

**World**: `fastbreak`, `fastplace`, `scaffold`, `miningbeyondview`, `nuker`

**Packet**: `badpackets`, `packetspam`, `extrapackets`, `packetrate`, `timer`

Movement detection uses a real physics model rather than a speed cap. The model
reproduces the documented vanilla speeds and is unit tested against them:

| Movement | Blocks per tick | Metres per second | Documented value |
| --- | --- | --- | --- |
| Walking | 0.2159 | 4.317 | 4.317 m/s |
| Sprinting | 0.2806 | 5.612 | 5.612 m/s |
| Sneaking | 0.0648 | 1.295 | 1.295 m/s |
| Terminal velocity | 3.92 | 78.4 | 3.92 b/t |

Because the server cannot observe which keys a player is holding, the predictor
enumerates the full input space and selects the best fitting candidate. It never assumes
a particular input.

## Testing

Snuff AC has **201 passing unit tests** covering:

- Kinematics against the documented vanilla speeds, terminal velocity, jump behaviour,
  friction on ice, and speed and slowness effects
- Position packing round trips, angle wrapping, greatest common divisor analysis
- Violation buffer accumulation, decay, capping and proportional adds
- Tolerance model accumulation, decay, clamping and leniency carry over
- Configuration loading, coercion, and latency and tick rate tolerance growth
- Check registry uniqueness, dispatch tables, category coverage and metadata
- Prediction accuracy, input recovery, and rejection of impossibly fast steps
- End to end engine behaviour: exemptions, disabled checks, immediate flags and
  realistic packet sequences

Run them with:

```bash
./gradlew test
```

## Project layout

```
snuffac/
├── snuffac-api/                 stable public surface for other plugins
├── snuffac-core/                platform neutral detection engine
│   ├── alert/                   alert formatting and delivery
│   ├── check/                   check API, registry, dispatcher
│   │   └── impl/                the 31 checks, by category
│   ├── combat/                  entity snapshots, hitbox and reach resolution
│   ├── confidence/              weighted cross check confidence model
│   ├── config/                  configuration model and loader SPI
│   ├── enforcement/             prevention types and confidence gating
│   ├── log/                     logging abstraction and file violation log
│   ├── mining/                  dig analysis against what was actually sent
│   ├── packet/                  normalised packet model, no platform types
│   ├── physics/                 vanilla kinematics, attributes, environment
│   ├── platform/                platform interfaces the core depends on
│   ├── player/                  per player state, composed by concern
│   ├── prediction/              input enumeration, prediction, movement graces
│   ├── punish/                  durations, manual punishments, warning ladder
│   ├── server/                  server tick health and TPS
│   ├── tolerance/               the forgiveness model
│   ├── util/                    math, positions, boxes, block classification
│   ├── violation/               buffers, levels, setback, persistent history
│   └── world/                   block classification, ores, obfuscation policy
├── snuffac-platform-paper/      Paper and Purpur bootstrap, menus, commands
│   └── gui/                     inventory menu framework and staff screens
├── snuffac-platform-velocity/   Velocity bootstrap
└── snuffac-dist/                shading, relocation and packaging
```

`snuffac-core` contains no reference to Bukkit, Paper, Velocity or the packet library.
Platform code translates into Snuff AC's own packet records at the edge and nothing
more. That is what keeps the engine testable and what makes the Velocity port possible.

More detail is in [`docs/architecture.md`](docs/architecture.md).

## Building

```bash
./gradlew build
```

Produces:

```
snuffac-1.21.x+paper/purpur/velocity-v1.0.7-dev.jar
```

The packet library is shaded and relocated to `dev.snuffac.libs.packetevents`, so
Snuff AC will not conflict with any other plugin that also uses it.

## Licence

Snuff AC is licensed under the **GNU General Public License v3.0**. See
[`LICENSE`](LICENSE).

This is not a stylistic choice. Snuff AC bundles PacketEvents, which is GPLv3, as a
linked dependency. Distributing a jar that contains a GPLv3 library requires the
combined work to be distributed under GPLv3. Licensing Snuff AC as MIT or Apache while
shipping PacketEvents in the same jar would be legally incoherent.

The full reasoning, the licence of every library used, and the licence of every
researched project are documented in [`credits.md`](credits.md).
