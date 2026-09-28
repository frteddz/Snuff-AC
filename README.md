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

**Status: development release, `1.0.1-dev`.** It is tested on Paper 1.21.11. It is not
yet recommended for production use on a public server, and it ships no automatic
bans on purpose. See [Known limitations](#known-limitations).

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

All commands require `snuffac.admin`.

| Command | Description |
| --- | --- |
| `/snuff info` | Version, platform, check count, enabled state, TPS, tracked players |
| `/snuff version` | Version, platform and check count in one line |
| `/snuff reload` | Reload `config.yml` and `checks.yml` |
| `/snuff checks` | Every check grouped by category with its on/off state |
| `/snuff toggle <check>` | Enable or disable a check and persist it to `checks.yml` |
| `/snuff debug [player]` | Toggle live debug output for a player, prints current state |
| `/snuff alerts <player>` | Toggle alerting for a specific player |
| `/snuff violations [player]` | Recent violation history for a player |
| `/snuff profile [player]` | Protocol version, latency, reach statistics, packet counters |
| `/snuff setback [player]` | Apply a manual setback |
| `/snuff stats` | Tracked player count, total violation level, current and worst TPS |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `snuffac.admin` | op | Access to every command. Inherits the two below. |
| `snuffac.debug` | op | Receive violation alerts, enable debug output |
| `snuffac.alerts` | op | Receive violation alerts |
| `snuffac.bypass` | false | Exempt from all checks |

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

## Configuration

Two files are generated in `plugins/SnuffAC/`:

- **`config.yml`** holds global settings: alerting, tolerance behaviour, setback
  behaviour, permissions, and the shared combat and packet tuning values.
- **`checks.yml`** holds per check settings, grouped by category. Every check exposes
  `enabled`, `buffer-threshold`, `buffer-decay`, `violation-increment`,
  `setback-threshold`, `alert-threshold`, `action` and an optional `command`.

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

### Punishments

`action` accepts `NONE`, `ALERT`, `LOG`, `COMMAND`, `SETBACK` and `KICK`. `COMMAND`
requires `command` and honours `command-threshold` and `command-cooldown-ms`, with
`%player%` substitution.

**This release ships no automatic bans.** Alerts, logging and setbacks are the only
default actions, and every check defaults to `ALERT`. Automatic punishment will only be
added once the detection system has been validated further in production.

## Checks

31 checks, all enabled by default.

**Movement**: `fly`, `speed`, `nofall`, `airmovement`, `groundspoof`, `step`, `highjump`,
`longjump`, `impossiblemovement`, `velocity`

**Combat**: `reach`, `autoclicker`, `aim`, `killaura`, `impossibleattack`,
`invalidattackstate`

**World**: `fastbreak`, `fastplace`, `scaffold`, `nuker`

**Packet**: `badpackets`, `packetspam`, `timer`

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

Snuff AC has **91 passing unit tests** covering:

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
│   ├── config/                  configuration model and loader SPI
│   ├── log/                     logging abstraction and file violation log
│   ├── packet/                  normalised packet model, no platform types
│   ├── physics/                 vanilla kinematics, attributes, environment
│   ├── player/                  per player state, composed by concern
│   ├── prediction/              input enumeration and prediction
│   ├── server/                  server tick health and TPS
│   ├── tolerance/               the forgiveness model
│   ├── util/                    math, positions, boxes, block classification
│   └── violation/               buffers, violation levels, setback hooks
├── snuffac-platform-paper/      Paper and Purpur bootstrap
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
snuffac-1.21.x+paper/purpur/velocity-v1.0.1-dev.jar
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
