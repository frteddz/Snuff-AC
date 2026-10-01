# Snuff AC

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Donate-ff5e5b?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/majdsafi)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/frteddz/Snuff-AC)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](LICENSE)
[![Releases](https://img.shields.io/badge/GitHub-Releases-orange?style=for-the-badge&logo=github&logoColor=white)](https://github.com/frteddz/Snuff-AC/releases)
[![Website](https://img.shields.io/badge/Website-snuff.ac-ff5e5b?style=for-the-badge&logo=googlechrome&logoColor=white)](https://frteddz.github.io/Snuff-AC/)

Free, open-source anticheat for Minecraft Java Edition servers. 38 checks across
movement, combat, world interaction and packet behaviour, on Paper and Purpur,
licensed GPL-3.0 with no premium tier.

Current release: **1.2.4-dev**. Requires **Java 21**.

> Every release on this page is a pre-release. Nothing here is recommended for a
> production public server yet, and the full release history is on
> [the changelog page](https://frteddz.github.io/Snuff-AC/changelog.html).

## What Snuff AC is

A server-side anticheat built around one idea: a single anomaly is not evidence.
Each check measures a deviation, the deviation is added to a per-player buffer,
the buffer decays while the player is clean, and only accumulated confidence
crosses a threshold. No single check punishes on its own, and the responses are
separate stages, so flagging, alerting, logging, setbacks and configured actions
never fire together by accident.

Packet handling is asynchronous. A dedicated thread drains the queue and runs
detection, the main thread only refreshes immutable world data and applies
setbacks, so the tick cost is a cache read rather than a physics simulation.

## The 38 checks

All 32 are enabled by default. Five are **structural** (a packet that should be
impossible) and 27 are **derived** (evidence accumulated from observation).

### Movement (16)

* **Fly**: unsupported flight from the server's own block view, plus an air time budget that only legitimate support refills
* **Speed**: prediction based, with 36 discretised input candidates and the best fit kept, plus an accumulator for a client that is only slightly fast for a long time
* **NoFall**: fall damage suppressed by claiming ground contact while airborne, and the server side fall distance tracked across the whole descent
* **AirMovement**: airborne acceleration against the predicted state
* **GroundSpoof** *(structural)*: claimed ground with no supporting block
* **GroundFlag**: sustained ground contact contradicting the server block view, the signature of air walk and no-fall spoofing
* **Step**: vertical gain beyond the modelled step height
* **HighJump**: launch velocity beyond what the jump strength attribute permits, and vertical gain past the step height in one tick
* **LongJump**: horizontal distance inconsistent with current momentum
* **ImpossibleMovement** *(structural)*: sequences the predictor cannot reconcile
* **Drift**: sustained per-tick offset between prediction and reported position
* **Phase**: the path between two positions swept against the server block view, rejecting movement through solid blocks, which is the NoClip, Phase, VClip and HClip family
* **Jesus**: standing on a liquid surface with no valid block beneath, which is the WaterWalk cheat
* **Spider**: sustained upward movement against a wall with no climbable block beside it
* **PitchLock**: pitch pinned to an exact constant, used by placement and glide modules
* **Velocity**: response to server-applied knockback, with damped displacement predicted and cobweb, water and ladder causes treated as absorbing it

### Combat (11)

* **Reach**: eye-to-hitbox measurement for attacks and to the nearest block face for interactions, with ping-aware tolerance; out of range attacks and interactions are cancelled
* **AttackAngle**: the real look vector cast against the true vanilla hitbox, rejecting hits that only landed on an expanded box
* **AutoClicker**: attack timing patterns, frequency analysis, a perfectly even interval spread, and the same delay repeated down the sample
* **Aim**: rotation behaviour around attacks
* **KillAura**: target sequencing, rapid multi-target switching, attacks on targets outside the field of view or behind the player, and rotation with no mouse jitter
* **ImpossibleAttack** *(structural)*: attack sequences that do not fit observable state
* **Critical**: forced criticals produced by emitting extra position packets with a lift too small for gravity immediately before an attack, and a critical landed while the server knows the player is not actually falling
* **RotationSnapBack**: a large aim rotation before an attack followed by a reverse rotation after it, which human input does not produce
* **Hitbox**: every attack raytraced against a vanilla sized hitbox, reporting a run of hits that only landed because the client used a larger box
* **TriggerBot**: attacks landing an identical short time after the crosshair reaches a target, with no aim movement in between
* **InvalidAttackState** *(structural)*: attacks that are invalid for the current player state

### World (5)

* **FastBreak**: break timing against material and tool context
* **FastPlace**: placement and interaction rate
* **Scaffold**: tower scaffolding while airborne, placements the player is not facing, and placements with nothing in hand to place
* **Nuker**: distinct blocks started per burst and per second, so retrying one block is never counted as several, plus dig targets the server block view says are out of sight
* **MiningBeyondView**: targeting valuable ores in a region the server never sent, which is knowledge the client could not legitimately have

### Packet (6)

* **BadPackets** *(structural)*: structurally impossible packets only
* **PacketSpam**: flood, measured rather than assumed
* **ExtraPackets**: position packets beyond what a client should send
* **PacketRate**: movement packet rate, evaluated only while the player is actually moving
* **Timer**: game tick speed alteration, with a movement gate, a window floor, and an accumulating balance so a client only a little ahead of the tick rate is still found
* **Blink**: movement packets held back and then released in a burst, which is the Blink and LagSwitch cheat

## Prevention, not just reporting

23 of the 38 checks can act on the packet **before** the server acts on it.
Detection that only tells you afterwards is a report.

* Setback to the last accepted position
* Attack cancellation through the damage event
* Placement and break cancellation
* Position resynchronisation when the client drifts from authority

Every action is gated behind accumulated confidence and can be disabled globally
without suppressing flagging or evidence. Prevention is on by default; the
optional warning ladder is off and is a staff decision to enable.

## Visual cheats

Render-time cheats cannot be detected, because nothing about them reaches the
server. They can be prevented, by withholding data the client is not entitled to.

* **X-Ray and ore ESP**: valuable ores are rewritten into decoy blocks in the chunk data that is actually sent. Enforced through the server engine, and verified on Paper 1.21.11 build 132 across the overworld, nether and end.
* **Player ESP and tracers**: players and mobs with no legal line of sight are hidden from the client, and revealed a few blocks early so nothing pops in.
* **Sound radar**: sounds carrying a position are nudged when the emitter is behind cover.

**Not implemented: storage ESP.** Container contents are sent to the client
exactly as vanilla sends them. Suppressing them means rewriting block entity
payloads on the wire, which is a considerably larger job than the ore rewrite and
has not been done. Do not buy this expecting a working storage viewer.

**Unverified:** entity concealment, tracers and sound fuzzing are written but have
not been seen working. Ore obfuscation is verified because chunk data can be
inspected directly.

**Impossible:** fullbright and other purely local rendering changes. Nothing
leaves the client, so nothing arrives at the server.

## Commands

Available to players:

* `/snuff report <player>`: report a player, pick a category, add a description
* `/snuff version`: version, platform and check count

Staff:

* `/snuff menu`: open the staff menu
* `/snuff reports`: report admin view, with claim and resolve
* `/snuff violations [player]`: flagged players, with their last known location
* `/snuff tp <player>`: teleport to a flagged player or their last known position
* `/snuff bypass <player> [on|off]`: grant or revoke the anticheat bypass
* `/snuff clearflags <player> confirm`: clear a violation history
* `/snuff clearwarns <player> confirm`: reset the warning ladder count
* `/snuff clearpunishments <player> confirm`: clear every active punishment
* `/snuff punishments [player]`: what someone is currently serving
* `/snuff settings`: retention, prevention and the warning ladder
* `/snuff reload`: re-read config, checks, GUI files and report options
* `/snuff toggle <check>`: enable or disable a check
* `/snuff checks`, `/snuff info`, `/snuff stats`, `/snuff profile`, `/snuff alerts`, `/snuff debug`, `/snuff sounds`

Punishments: `ban`, `tempban`, `ipban`, `tempipban`, `timeout`, `mute`, `tempmute`,
`warn`, each with its exact reverse. Durations accept `m`, `h` and `d` in any
order, so `1h 10s` is valid, and a reason is required on every one.

Destructive commands ask for confirmation, expire after 20 seconds, and log who
ran them. They work from the console, which has to type `confirm`.

## Permissions

37 declared nodes, so everything the plugin checks can actually be granted.

Default to every player:

* `snuffac.use`, `snuffac.version`, `snuffac.report`, `snuffac.report.status`

Default to op:

* `snuffac.admin`, `snuffac.alerts`, `snuffac.menu`, `snuffac.debug`, `snuffac.sounds`
* `snuffac.violations`, `snuffac.checks`, `snuffac.teleport`, `snuffac.stats`
* `snuffac.profile`, `snuffac.setback`, `snuffac.reload`
* `snuffac.bypass`, `snuffac.bypass.give`, `snuffac.reports.manage`
* `snuffac.clear.flags`, `snuffac.clear.warns`, `snuffac.clear.punishments`, `snuffac.clear.all`
* `snuffac.escalation.manage`, `snuffac.exempt.punish`

Punishments are split per action, with `snuffac.punish.maxduration.1h`, `.1d`,
`.7d` and `.30d` capping what junior staff may issue.

A permission node that is not declared in `plugin.yml` cannot be granted by
LuckPerms, so every node the code checks is declared.

## Configuration

* `config.yml`: general behaviour, alerts, prevention, tolerance, tuning profile, anti-xray, visual concealment, reports
* `checks.yml`: every check, its thresholds, its evidence kind, and its action
* `GUI/*.yml`: six menu files. Material, name, lore, slot, action, permission, amount and glint are all owner editable
* `GUI/report-options.yml`: the report categories. Add one and it appears in the picker

`tuning.profile` ships **strict**. `balanced` and `lenient` widen the margins if
you would rather have fewer flags.

**False positives are treated as the worst kind of bug in this project.** The
tolerance model names every source of forgiveness instead of hiding it in one
epsilon: external pushes, pistons, slime, ice, item use slowdown, vehicles,
teleports, latency and low server tick rate. Each contribution decays every tick
and the total is clamped. Every check has an applicability gate, so a check skips
itself when its assumptions do not hold instead of producing misleading evidence.

Legitimate movement is verified against real client connections, not only
asserted in unit tests. All eight movement scenarios run clean.

## Support

* Paper 1.21.11 and Purpur, tested on Paper build 132
* Java 21
* Velocity 3.4.0 is supported for packet and network timing checks only. A proxy
  has no world data or player position API, so world and combat geometry checks
  are inactive there.
* Folia is not supported.

Anti-Xray is driven through private Paper internals, so it is confirmed on
Paper 1.21.11 build 132 and unknown on other forks. On an unrecognised build it
logs a failure and leaves protection off rather than throwing.

## Links

* **Website:** https://frteddz.github.io/Snuff-AC/
* **Changelog:** https://frteddz.github.io/Snuff-AC/changelog.html
* **Source:** https://github.com/frteddz/Snuff-AC
* **Releases:** https://github.com/frteddz/Snuff-AC/releases
* **Bugs:** https://github.com/frteddz/Snuff-AC/issues

## Donate

Developing and testing an anticheat takes a lot of time. If Snuff AC protects your
server, consider supporting it on [Ko-fi](https://ko-fi.com/majdsafi).

## Ad space

Banner or link placement is available on this page and in the repository README.

* **Contact:** `teddzfr@gmail.com`
