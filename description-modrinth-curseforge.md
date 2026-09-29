# Snuff AC

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Donate-ff5e5b?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/majdsafi)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/frteddz/Snuff-AC?tab=readme-ov-file)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](https://github.com/frteddz/Snuff-AC?tab=GPL-3.0-1-ov-file)
[![Stars](https://img.shields.io/badge/GitHub-Stars-yellow?style=for-the-badge&logo=github&logoColor=white)](https://github.com/frteddz/Snuff-AC/stargazers)
[![Releases](https://img.shields.io/badge/GitHub-Releases-orange?style=for-the-badge&logo=github&logoColor=white)](https://github.com/frteddz/Snuff-AC/releases)
[![Issues](https://img.shields.io/badge/GitHub-Report_Bug-2da44e?style=for-the-badge&logo=github&logoColor=white)](https://github.com/frteddz/Snuff-AC/issues)

Modern, lightweight Minecraft anti-cheat designed for high performance and low server overhead.

## What is Snuff AC?

Snuff AC is a server-side anti-cheat built from the ground up to prevent packet-level exploits, modern cheat clients, and combat cheats without ruining server performance.

Instead of running heavy checks on the main tick thread like older solutions, Snuff AC handles packet tracking asynchronously. This keeps your server running at 20 TPS even when handling high player counts and heavy combat.

Whether you run a competitive PvP server, a Survival network, or an Anarchy instance, Snuff AC stops cheaters without causing TPS drops or rubberbanding legitimate players.

## Why Snuff AC?

Most public anti-cheats struggle with two main problems: lag and false positives. Snuff AC focuses on solving both.

* **Asynchronous Check Loop:** Calculations run off the main server thread so heavy physics simulations never lag the main loop.
* **Packet Level Tracking:** Listens to raw network packets instead of relying solely on Bukkit events, catching closet cheaters and reach abuses that normal plugins miss.
* **Lag Compensation:** Handles high-latency players, tick desyncs, and ping spikes gracefully to minimize false flags.
* **Prevention, not just reports:** Illegal attacks and block placements are cancelled before the server acts on them, rather than logged after the fact.
* **Evidence before punishment:** Violation levels, per-check buffers, and confidence accumulation. A single check never punishes on its own.
* **Fully Configurable:** Modular checks allow you to tweak thresholds, alert formats, ping limits, and punishment actions in `config.yml`.
* **Staff Tools:** Real-time notifications, player logs, player reports, and interactive menus for moderators.

## Supported Checks

### Combat
* **KillAura:** Angle verification, heuristics, multi-target switching, mouse constant analysis
* **Reach:** Eye-to-hitbox measurement with ping aware tolerance, and the attack is cancelled when it is out of range
* **AttackAngle:** The look vector is cast against the true vanilla hitbox, catching expanded hitbox cheats
* **AutoClicker:** Frequency analysis, consistency checks, CPS spikes
* **Velocity:** Horizontal and vertical modification checks
* **Criticals & AimAssist:** Aim lock dynamics and packet timing

### Movement
* **Fly:** Motion, hover, and creative fly checks
* **Speed:** Ground, air, and friction logic
* **HighJump / LongJump / Step:** Vertical gain beyond what the jump allows
* **NoFall:** Fall damage reset by claiming ground contact while airborne
* **GroundSpoof / GroundFlag:** Claiming ground with no supporting block

### World
* **FastPlace / FastBreak:** Interaction frequency checks
* **Scaffold:** Pitch and yaw placement dynamics
* **Nuker:** Raw dig packet rate rather than distinct block counting
* **MiningBeyondView:** Digging a block further away than the view reaches

### Packets
* **BadPackets:** Structurally impossible packets only
* **PacketSpam / PacketRate:** Flood and timer manipulation, measured only while the player is actually moving
* **Timer:** Game tick speed alteration
* **ExtraPackets:** Position packets beyond what the client should send

## Visual Cheats

Render-time cheats cannot be detected, because nothing tells the server that a player looked through a wall. They can be **prevented**, by withholding data the client is not entitled to:

* **X-Ray and Block ESP:** valuable ores are replaced with decoy blocks in the chunk data sent to the client
* **Storage ESP:** container data is not volunteered for players who cannot legally see it
* **Player ESP and tracers:** players and mobs with no legal line of sight are not sent to the client at all, and are revealed a few blocks early so nothing pops in
* **Sound radar:** sounds carrying a position are nudged when the emitter is behind cover

Fullbright remains impossible to affect, because it never leaves the client.

## Commands and Permissions

### Commands
* `/snuff` - Open the staff menu
* `/snuff version` - Version, platform and check count
* `/snuff report <player>` - Report a player, pick from seven categories
* `/snuff reports` - Open the report admin view, claim and resolve
* `/snuff violations [player]` - Browse flagged players, with their last known location
* `/snuff tp <player>` - Teleport to a flagged player, or their last known position
* `/snuff bypass <player> [on|off]` - Grant or revoke the anticheat bypass
* `/snuff clearflags <player>` - Clear a player's violation history
* `/snuff clearwarns <player>` - Reset the warning ladder count
* `/snuff clearpunishments <player>` - Clear every active punishment
* `/snuff reload` - Reload plugin configurations and check settings

Destructive commands ask for confirmation first, and log who ran them.

### Permissions

Available to every player by default:

* `snuffac.use` - run the command at all
* `snuffac.version` - view the version
* `snuffac.report` - file a report
* `snuffac.report.status` - check your own report status

Staff tier, default op:

* `snuffac.alerts` - receive staff notifications
* `snuffac.menu` - open the staff menus
* `snuffac.debug` - live debug output
* `snuffac.sounds` - menu and command sounds
* `snuffac.violations` - browse flagged players
* `snuffac.teleport` - teleport to a flagged player
* `snuffac.bypass.give` - grant the bypass to another player
* `snuffac.clear.flags`, `snuffac.clear.warns`, `snuffac.clear.punishments` - destructive actions
* `snuffac.escalation.manage` - configure the automatic warning ladder

Punishments are split per action (`snuffac.punish.ban`, `tempban`, `kick`, `mute`, `warn`, `unban`) with `snuffac.punish.maxduration.*` capping what junior staff may issue.

All of these work with LuckPerms and any other permission manager, since they are plain node strings. A node that is not declared in `plugin.yml` cannot be granted, so every node the plugin checks is declared.

## Configuration

* `config.yml` - General behaviour, alerts, tolerance, tuning profile, anti-xray, visual concealment, reports
* `checks.yml` - Every check, its thresholds, and whether it may prevent
* `GUI/*.yml` - One file per menu. Material, name, lore, slot, action, permission, amount and glint are all owner editable

`tuning.profile` ships **strict**, so a fresh install reports a clear detection. `balanced` and `lenient` are available if you want to widen the margins.

## Links and Support

* **Source Code:** [GitHub Repository](https://github.com/frteddz/Snuff-AC?tab=readme-ov-file)
* **License:** [GPL-3.0 License](https://github.com/frteddz/Snuff-AC?tab=GPL-3.0-1-ov-file)
* **Releases:** [Download Latest Releases](https://github.com/frteddz/Snuff-AC/releases)
* **Bug Reports:** Open an issue on [GitHub Issues](https://github.com/frteddz/Snuff-AC/issues) if you spot a false positive or bug.

## Donate

Developing and maintaining an anti-cheat takes significant time and testing. If Snuff AC helps protect your server, consider supporting the project on [Ko-fi](https://ko-fi.com/majdsafi) or through the GitHub repository.

[![Donate on Ko-fi](https://img.shields.io/badge/Ko--fi-Donate-ff5e5b?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/majdsafi)

## Ad Space / Sponsorship

Interested in placing a banner or link for your hosting service, plugin, or server network? Ad space is available on this Modrinth page and inside the repository README.

* **Contact:** `teddzfr@gmail.com`
