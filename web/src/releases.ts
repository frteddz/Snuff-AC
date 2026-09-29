type ReleaseItem = { title: string; copy: string }
type Release = { version: string; stamp: string; items: ReleaseItem[] }


export const releases: Release[] = [
  {
    version: '1.1.1-dev',
    stamp: '2026-09-30',
    items: [
      {
        title: 'Reports were unusable in every released version',
        copy: 'The report flow had never once been completed end to end. Filing a report, listing it, claiming it, and resolving it were all separate code paths and none of them had been exercised together. The picker\'s submit button was declared at slot 49 inside a 5 row menu, which only has 45 slots, so the button was silently relocated and the report could not be filed. The picker is now 6 rows, and a test asserts that every declared slot is inside its own inventory. `/snuff reports` opened an empty view because the admin command dispatched to a handler that took no player. Resolved, claim, and reject now exist and were clicked through with two real clients.',
      },
      {
        title: 'Anti-Xray reported itself as active on a live server when it was not',
        copy: 'The bridge called `apply` before worlds existed, caught every failure, and logged a success line anyway. It now defers until the world is loaded and logs the result it actually got. Verified on all three dimensions: `engineMode=OBFUSCATE hidden=10/10 replacement=3`.',
      },
      {
        title: 'HighJump flagged every normal jump',
        copy: 'Root cause was a units mistake. The server attribute `JUMP_STRENGTH` already returns 0.42, and the code multiplied that by its own hardcoded 0.42, so any jump above 0.17 was treated as a violation. Normal vanilla first launch is 0.33, so every jump tripped it. The attribute is now normalised once and the threshold is a multiplier.',
      },
      {
        title: 'Timer flagged a player standing still',
        copy: 'The check measured packet rate over an unbounded window. A stationary player sends 0 packets, which looked identical to a player whose packets were being dropped. It now only measures while the player is actually moving, and has a floor for the window it inspects.',
      },
      {
        title: 'Violations lost their location and history never cleared',
        copy: 'The history file was written with 11 fields but read expecting 15, so world and coordinates were dropped on every restart and `/snuff tp` aimed at 0,0,0. The length guards were also off by one, so z was never read at all. `clearflags` deleted a file named after the player, but the file was written with the dashes removed from the UUID, so the file was never actually deleted. Staff saw "cleared", and every flag came back on the next join. `total` and `history` only read the in memory cache, so anything asked about an offline player reported zero. Both now read from disk on a cold cache. Records written by older builds still load.',
      },
      {
        title: 'A bypass grant was not actually persisted',
        copy: '`setBypass` returned null for anyone who was not online, which made a grant for an offline staff action impossible, and the name was written lower cased so it came back as `tester2` in staff messages.',
      },
      {
        title: 'Console could not use the staff commands',
        copy: 'The new commands all required a `Player`, so the console, which is exactly where server owners run them, was rejected. The console now works and still has to type `confirm` for anything destructive.',
      },
      {
        title: 'Filler colour could not be configured',
        copy: 'The `filler` key was written twice per file, first as a material name and then as a boolean, so the material was always lost and every menu fell back to black. There is now a separate `filler-material` key.',
      },
    ],
  },
  {
    version: '1.1.0-dev',
    stamp: '2026-09-30',
    items: [
      {
        title: 'A tempban did not stop a banned player rejoining',
        copy: 'There was no login listener at all. `isBanned` was called in exactly one place, when lifting a ban, so a player who reconnected was never checked. `applyOnline` only fires for a player who is already online, which is why the kick worked and the reconnect did not. Enforcement was structurally incapable of stopping a returning player. An `AsyncPlayerPreLoginEvent` listener now disallows the connection on a live BAN or TEMPBAN, reading the same store the kick path uses, so the two cannot disagree. Reproduced twice by the user, banning an alt and then banning their own main account. Both rejoined while the ban was still running.',
      },
      {
        title: '`packetrate` flagged legitimate play and set the player back',
        copy: 'The check compared a client\'s movement packet rate against a flat 20, which is the server tick rate. Those are not the same quantity. A vanilla client does not send a position packet every tick, so a player standing still sends well under the 0.80 lower bound, and three four second windows of not moving is about twelve seconds of standing still. A window is now discarded unless the player actually moved during it, the bands are wider, five consecutive windows are required instead of three, and a teleport or a recent low TPS resets the measurement rather than being averaged through. This is the second rate based check to fail this way after `packetspam` in v1.0.9. Both were measuring a client quantity against a fixed constant rather than against what the server actually did.',
      },
      {
        title: 'Spear attribute swapping was never detected',
        copy: '`observedReach` read `Attribute.ATTACK_DAMAGE`, which is damage, not reach. The reach attribute on 1.21.11 is `ENTITY_INTERACTION_RANGE`. The code was asking how much damage a player did, storing it in a field called `observedAttackReach`, and comparing that damage number against a distance, so it would flag a weak weapon and miss a reach cheat. The expected reach was also never set. `attackReach` initialised to 3.0 and nothing ever wrote it, so even with the right attribute it compared every weapon against a hardcoded sword reach. Now reads `ENTITY_INTERACTION_RANGE`, derives the expected reach from the held weapon, compares in both directions rather than only looking for a value that is too low, and logs a debug line for a weapon whose reach is not observable instead of guessing. The v1.0.6 release notes claimed this was fixed. It was not, and it never had been.',
      },
      {
        title: '`/snuff reports` threw, and `/snuff report` opened a menu with dead buttons',
        copy: '`render()` called `Bukkit.getPlayer(target == null ? null : target.getUniqueId())`. The admin view is constructed with a null target on purpose, so the guard produced null and handed it to a method that rejects null. `IllegalArgumentException: UUID id cannot be null`, every time. In the picker, the same line set `renderPlayer` to the report target rather than to the person looking at the menu. `renderPlayer` is what the per button permission check consults, so every category button was checked against the wrong player, failed, and was never placed in the inventory. A menu that renders with no buttons looks like a working menu with unresponsive items. `openAdminReports` already called `forViewer(player)` before `build()`. `build()` then called `render()`, which overwrote the correct value. The `forViewer` call was dead. `renderPlayer` is now written in exactly one place.',
      },
      {
        title: '`/snuff menu` was advertised and did nothing',
        copy: '`menu` was in the tab completion list and in the documentation, and had no `case` anywhere, so it produced "unknown subcommand" three keystrokes after the server offered it. The bare `/snuff` worked, which is why it survived. Added the case, and a structural test asserting every advertised subcommand maps to a permission, so the three lists cannot silently disagree again.',
      },
      {
        title: 'Player facing commands were unreachable',
        copy: '`plugin.yml` declared the command with `permission: snuffac.admin`, and Bukkit enforces that before the executor runs. So `/snuff report` and `/snuff version` did not work for a normal player at all, even though `snuffac.report` defaults to true. The two disagreed: a player was told they could report, opened a menu where nothing was clickable, and had no command to fall back on. The command-level permission is gone and each subcommand is gated individually. The permission tree is now three tiers and declares every node the code checks, so LuckPerms and other managers can actually grant them. A node that is not declared cannot be granted, and the failure is silent. Added `snuffac.teleport`, `snuffac.bypass.give`, `snuffac.reports.manage`, `snuffac.escalation.manage` and the four `snuffac.clear.*` nodes, none of which inherit from the punishment permissions.',
      },
      {
        title: 'Punishment screens with real detail',
        copy: 'Every ban, temporary ban, mute and kick screen now names the staff member who issued it, the exact expiry as a date and a time, the remaining duration, the reason, and how to appeal. A one minute ban tells the player when they may rejoin instead of telling them to run a command they cannot run while banned. Staff names and reasons are stripped of markup, since a reason is free text written by a person.',
      },
      {
        title: 'Bypass, clear commands, and staff location',
        copy: '`/snuff bypass <player> [on|off]` grants or revokes the anticheat bypass, persists to `bypass.tsv`, and is now a real input to the exempt computation rather than a value that would be overwritten on the next refresh. Every grant and revoke is announced to staff. `/snuff clearflags`, `/snuff clearwarns` and `/snuff clearpunishments`, each with its own permission, each logging what was destroyed and by whom. Destructive, so they require confirmation, and clearing flags also resets the live session state so a cleared player is not still in violation a second later. `ViolationInfo` now carries world and position, so a flag records where it happened. `FlagsMenu` shows last seen world and coordinates per player, and `/snuff tp` teleports to a player or to their last known position, refusing if the chunk is not loaded. This is a breaking change to the public API, which is why it happens on a dev release.',
      },
      {
        title: 'Reference plugin',
        copy: '`DonutSus-1.0.jar` was inspected for metadata only. It has no licence file and declares no licence in its bundled pom, so nothing was decompiled and no code was taken from it. Its dependency list is worth one note: it reads flags from Vulcan and Grim through their public APIs. Snuff should never do that. Two anticheats deciding about the same player without knowing what the other decided is a bad outcome for a server, and Snuff generating its own evidence and owning it is the correct architecture.',
      },
    ],
  },
  {
    version: '1.0.9-dev',
    stamp: '2026-09-30',
    items: [
      {
        title: 'packetspam flagged every player, including a stationary one',
        copy: 'The rate was computed as `count * 1000 / (elapsed + 1)`. On the first packet of a window that reports 1000 packets per second, on the second 666, on the third 500. The window also started at an arbitrary packet rather than a clock boundary, so the bad arithmetic repeated forever. Against a 400 per second threshold, a player standing still was flagged every two seconds. The live log proved it: `packetsPerSecond=1000.0, threshold=400.0, count=1`. A window is now only evaluated once it has been open for at least 250ms, so the division always means something. The rate is a real count over a real interval, the window closes on the tick rather than on whichever packet arrived, a per-type breakdown is recorded as evidence, and the rate must hold across three consecutive windows before it alerts. This was broken for every player on every version since it was written.',
      },
      {
        title: 'badpackets reset the player\'s position while they bridged',
        copy: 'The check flagged any block interaction whose position did not match an "active dig". `digActive` stays true for the whole mining duration, so placing a block or breaking the next one while mining tripped it. Nobody had to be cheating, mining and building does it. The rule has been removed entirely. Dig behaviour belongs to FastBreak and Nuker, and a check called badpackets should only reject packets that cannot be decoded. The client attack cursor is no longer validated as if it were authoritative. Reach and AttackAngle already validate against the server-resolved hitbox. Only non-finite coordinates are still flagged immediately, because a NaN genuinely cannot come from a vanilla client. Out-of-bounds positions are buffered. The world border now matches the vanilla maximum of 29999984 rather than 30000000.',
      },
      {
        title: 'Prevention could fire on a single flag from a check that guesses',
        copy: 'v1.0.8 set `setback-threshold: 1.0` on 23 checks, so the first flag from a behavioural check teleported the player. For BadPackets that turned a chat message into a rubber band the player felt every two seconds while bridging. Every check now declares an `evidence` kind. `STRUCTURAL` checks are things that cannot legitimately happen and may act at their own threshold. `DERIVED` checks, which is everything that measures a rate, a ratio, an average or a window, are structurally unable to request a setback until one violation level past their alert threshold, regardless of what the config says. BadPackets, ImpossibleMovement, ImpossibleAttack, InvalidAttackState and GroundSpoof are structural. The other 27 are derived.',
      },
      {
        title: 'Alerts did not use the Snuff prefix',
        copy: 'Alerts were built by a completely separate formatter that took a plain `Snuff` from `general.alert-prefix` and wrapped it in hardcoded square brackets. `LegacyColour` was never involved, so the gradient never appeared. The brackets were in the format string, not in the prefix, which is why adding the real prefix would have double-bracketed it. Alerts now carry the same gradient prefix as `/snuff`, configured as `general.chat-prefix`, and converted through `LegacyColour` before MiniMessage sees it. The console line is rendered separately and is plain text, because a terminal cannot show a gradient. Hex codes are resolved rather than left as literal text.',
      },
      {
        title: 'Anti-X-Ray was still guessed at',
        copy: 'The v1.0.8 reflection chain through `world.getUnsafe().getWorldConfiguration()` does not exist. `org.bukkit.World` has no `getUnsafe`, `UnsafeValues` has no world-config access, and paper-api ships no anti-xray classes at all. `AntiXrayBridge` now tries four documented strategies in order and reports every one it attempted. The engine mode resolves against whatever enum constants the running server actually has. Setters are matched by signature and a missing one is reported by name rather than thrown. The diagnostic now logs the exception class, its message and the first four stack frames, instead of the literal text `null`.',
      },
      {
        title: 'Entity concealment was one-way and never fired',
        copy: 'The visible set was only populated in the legal branch, so a hide was only ever sent for something already known-visible, and on a player\'s first frame that set was empty. There was no `showEntity` call anywhere, so reveal-radius and reveal-padding could not work. A one-way hide is worse than no concealment. The pass now tracks an explicit visible or hidden state per entity per viewer, seeds it on the first pass, has a real reveal path, and drops state for entities that leave the world. It iterates the world\'s actual entities rather than the combat environment. When concealment is off or the viewer is exempt, everything is revealed.',
      },
      {
        title: 'Player reports',
        copy: '`/snuff report <player>` opens a seven category picker: cheating, exploiting, explicit language, offensive behaviour, griefing, inappropriate name, and staff impersonation. `/snuff reports` opens the admin view, listing reports newest first with claim and resolve actions so two admins cannot both act on the same report and none is silently dropped. A cheating report attaches the target\'s flag count and most recent check from Snuff\'s own history, so the admin sees the evidence before the reporter\'s note. Reporter notes are stripped of markup and length limited, self reports are refused, staff holding `snuffac.exempt.punish` cannot be reported, and a reporter is rate limited to five reports per ten minutes. Reports persist to `plugins/SnuffAC/reports.tsv` with configurable retention, defaulting to 30 days.',
      },
      {
        title: 'Every menu is now owner editable',
        copy: 'Six menu files are written to `plugins/SnuffAC/GUI` on first run: `main-gui.yml`, `settings-gui.yml`, `flags-gui.yml`, `warned-gui.yml`, `reports-gui.yml` and `reports-admin-gui.yml`. Per item: material, display name, lore, slot, action, enabled, permission, stack amount and glint. Layout is configurable too, with rows, title and filler. A missing file falls back to built-in defaults, a malformed file is reported and ignored, and every slot, row count and stack amount is bounds checked so a bad file cannot index outside an inventory. Actions are an allowlist of identifiers, never a command string. An owner-supplied command in a menu is remote code execution on a shared server. Cancellation still does not depend on the registry lookup succeeding, so a future failure degrades to a cancelled click rather than a free item, which is the v1.0.5 duplication bug.',
      },
    ],
  },
  {
    version: '1.0.8-dev',
    stamp: '2026-09-30',
    items: [
      {
        title: 'Detection was muted by the shipped thresholds',
        copy: 'Every one of the 31 checks shipped with a `buffer-threshold` of 20 to 30 while checks only add 5 to 10 buffer per flag, a `buffer-decay` of 0.5 to 0.6 applied every tick, and an `alert-threshold` of 4 to 5 on top. Reaching a single alert needed roughly 3 to 6 consecutive flags to cross the buffer and then 4 to 5 more crossings to reach the alert threshold, with decay eating the buffer between bursts. In practice tens of consecutive cheat actions were needed, and burst cheating never alerted at all. Hard checks now ship with a buffer threshold of 1.0, a decay of 0.1 and an alert threshold of 1.0, so one clear detection reports. Statistical checks use an alert threshold of 2.0. The hardcoded fallback in `CheckConfig.defaults` carried the same muted values and is now strict as well, so a check with no shipped config still reports. `setback-threshold` was 0.0 on all 31 checks, which made `setbacksEnabled()` false everywhere, so no check could prevent anything even after flagging. Prevention thresholds are now 1.0 for hard checks.',
      },
      {
        title: 'Anti-xray never ran on modern Paper',
        copy: 'Obfuscation was applied through the removed `com.destroystokyo.paper` config classes, so on Paper 1.21 the server logged a `ClassNotFoundException` at every startup and shipped raw ore data to every client. X-Ray and Block ESP had full data to work with. It now resolves the current Paper `AntiXrayConfiguration` through `getUnsafe` and applies `OBFUSCATE` with proper `BlockData` block lists. When no world can be configured the plugin now says so plainly, naming X-Ray and storage ESP as still unblocked, rather than logging a reflection stack trace.',
      },
      {
        title: 'The permission tree was invalid',
        copy: '`plugin.yml` had a stray `snuffac.sounds` key with no value inside the `snuffac.admin` children block, plus a malformed `snuffac.bypass` and a duplicated `snuffac.menu`. Paper rejected the whole `snuffac.admin` node on every load, so the intended inheritance never applied. The tree now parses cleanly with eighteen nodes and twelve valid children.',
      },
      {
        title: 'Prevention routing did nothing',
        copy: '`CANCEL_ATTACK` called `packetModificationEnabled(false)`, a flag that was written but never read anywhere, so enforcement silently accomplished nothing. Attacks, placements and interactions now route to a real prevention signal.',
      },
      {
        title: 'Prevention that actually prevents',
        copy: 'A prevention signal per player that checks request through `preventAttack`, `preventPlacement`, `preventInteraction` and `requestSetback`. The packet gate runs at `LOWEST` priority so a cancelled attack never reaches the server. Attack packets are now dispatched synchronously on arrival rather than queued, because a decision made a tick later cannot cancel the packet that has already landed. `EntityDamageByEntityEvent` cancellation as a second line of defence, so an illegal hit is stopped even if the packet was already in flight. Speed, Fly and HighJump now request a setback to the last legal position instead of only reporting.',
      },
      {
        title: 'Hitbox verification',
        copy: '`HitboxVerifier` casts the attacker\'s actual look vector against the true vanilla hitbox, 0.6 by 1.8 with a 1.5 sneaking height, using a slab method against the box rather than a distance check. `AttackAngleCheck` rejects hits that landed only on an expanded hitbox, tracking a streak so a single odd frame is not punished, and cancels the attack. This catches the Hitboxes cheat that Reach alone cannot see. Reach now cancels the attack instead of only flagging, and reports the ray result, angle, and reject reason as evidence.',
      },
      {
        title: 'Aim analysis',
        copy: '`GcdAnalysis` learns the player\'s own mouse constant from a rolling window of pitch and yaw deltas, then flags rotation deltas that are not a multiple of it. Human mouse input always lands on the grid; synthetic aim usually does not. KillAura now detects rapid target switching between entities far apart in angle, and cancels the attack.',
      },
      {
        title: 'Visual cheats are neutralised, not just logged',
        copy: 'Entity hiding. Players and mobs with no legal line of sight are hidden from the client entirely, so Player ESP and tracers have nothing to reveal. The pass runs every 4 ticks, reveals anything within 16 blocks so close fights never break, and reveals early inside 24 blocks once a raycast confirms the view is about to open, so nothing pops in. Sound fuzzing. Sounds carrying a position, footsteps, eating, drinking, bow draws, attacks and armour, are nudged when the emitter is behind cover, so sound radar and sound ESP cannot be used to find players. `tuning.profile` in `config.yml` with `strict` as the default. Strict scales movement tolerance to 0.5 and reach tolerance to 0.6, so the margins narrow without editing thirty one check blocks by hand. The plugin logs its effective profile and counts on startup, and warns when any check is tuned so loosely that ordinary cheating will not alert.',
      },
    ],
  },
  {
    version: '1.0.0-dev',
    stamp: '2026-09-29',
    items: [
      {
        title: 'Engine',
        copy: 'Multi module Gradle build targeting Java 21, split into a stable public API module, a platform neutral core, separate Paper and Velocity platform modules, and a shading and packaging module. A normalised, immutable packet model. The engine never sees a platform type. A dedicated single threaded check executor with deterministic ordering, fed by a queue from the network thread. Packet arrival times are captured in nanoseconds at the edge so timing information survives the hand off. A platform service layer covering scheduling, messaging, permissions and world access. Immutable per player world caching, built on the main thread each tick and read without locking by the check thread, so no server API call ever happens off thread.',
      },
      {
        title: 'Movement model',
        copy: 'A vanilla kinematics model reproducing the documented speeds: 0.2806 blocks per tick sprinting, 0.2159 walking, 0.0648 sneaking, terminal velocity 3.92, jump velocity 0.42. All of these are asserted as unit tests. Attribute driven modelling of movement speed, gravity, jump strength, step height and safe fall distance, read from the server each tick. Block classification into a platform neutral taxonomy carrying slipperiness, passability, climbability and liquid state, so the physics never depends on Bukkit materials. Input space enumeration with best fit selection, because the server cannot observe which keys a player is holding.',
      },
      {
        title: 'False positive control',
        copy: 'A tolerance model that accumulates per axis forgiveness from named sources, decays it over time, and caps the total. Sources include external pushes, pistons, bouncy blocks, item use slowdown, attack slowdown, vehicles, server knockback, explosions, riptide, block changes, chunk loads, teleports, setbacks, high latency, low TPS and game mode. Leniency carry over, granting a capped fraction of the previous offset as extra tolerance on the tick after a flag, which prevents a flag followed by unrestricted movement. Latency and tick rate adaptive tolerance, both capped, so high ping and low TPS are never punished. An applicability gate per check so a check is skipped entirely when its assumptions do not hold, rather than being allowed to produce misleading evidence.',
      },
      {
        title: 'Violation system',
        copy: 'Evidence buffers with proportional adds, decay and capping, kept separate from violation levels so short term confidence and accumulated history are distinct. Separate alert and violation thresholds, where the alert threshold gates only the alert and never the recording of the violation. Configurable setback with a required payload and player opt out, dispatched on the main thread. Configurable punishment actions: none, alert, log, command, setback, kick. Command actions honour a threshold and a cooldown.',
      },
      {
        title: 'Checks, 23 total, all enabled by default',
        copy: 'Movement: `fly`, `speed`, `nofall`, `airmovement`, `groundspoof`, `step`, `highjump`, `longjump`, `impossiblemovement`, `velocity` Combat: `reach`, `autoclicker`, `aim`, `killaura`, `impossibleattack`, `invalidattackstate` World: `fastbreak`, `fastplace`, `scaffold`, `nuker` Packet: `badpackets`, `packetspam`, `timer`',
      },
      {
        title: 'Operations',
        copy: '`/snuff` with `info`, `version`, `reload`, `checks`, `toggle`, `debug`, `alerts`, `violations`, `profile`, `setback` and `stats`, with tab completion. Template driven alerting with placeholders, per player rate limiting, configurable verbosity, and per player alert toggles. Daily violation log files with retention based pruning. Debug output exposing position, velocity, ground state, air time, ping, tick rate, tolerance and per check state. Four permissions: `snuffac.admin`, `snuffac.debug`, `snuffac.alerts`, `snuffac.bypass`. Configuration split into `config.yml` for global settings and `checks.yml` for per check settings, with toggle changes persisted.',
      },
      {
        title: 'Platform support',
        copy: 'Paper 1.21.11: full check set, tested end to end against a real server. Purpur: same build, detected at runtime, no Purpur specific code. Velocity 3.4.0: packet category and network timing, with world and combat geometry checks reported inactive because a proxy has neither block data nor player positions.',
      },
      {
        title: 'Developer API',
        copy: 'A separate `snuffac-api` artifact exposing the engine handle, a read only check list, violation information and a listener interface.',
      },
      {
        title: 'Testing',
        copy: '91 unit tests covering kinematics against documented vanilla speeds, position packing, angle wrapping, buffers, the tolerance model, configuration, the check registry, prediction accuracy and input recovery, and end to end engine behaviour including exemptions, disabled checks and immediate flags.',
      },
      {
        title: 'Documentation',
        copy: '`README.md`, `credits.md`, `docs/architecture.md`, this changelog, and configuration guidance.',
      },
    ],
  },
  {
    version: '1.0.1-dev',
    stamp: '2026-09-30',
    items: [
      {
        title: 'Combat',
        copy: 'Entity resolution. Attacks are now measured against the server\'s own hitboxes instead of the client supplied cursor, with correct survival (3.0), creative (5.0) and vehicle (5.0 and 8.0) reach limits, crouch aware eye height, and a vertical padding. Line of sight testing between the eye and the target box, sampled along the segment. `criticals` check. Detects forced criticals produced by emitting extra position packets with a vertical lift too small for gravity, immediately before an attack. `rotationsnapback` check. Detects a large aim rotation immediately before an attack followed by a reverse rotation immediately after, which human input does not produce.',
      },
      {
        title: 'Movement',
        copy: '`groundflag` check. Detects sustained ground contact claims that contradict the server block view, the signature of air walk and ground flag no fall spoofing. `pitchlock` check. Detects pitch pinned to an exact constant (straight down, or a fixed glide angle), which placement and glide modules use to defeat server heuristics. `drift` check. Detects a sustained constant per tick offset between the prediction and the reported position, which is how several cheats disguise position edits. The discriminator is the drift rate, not its magnitude, so ordinary jitter does not trigger it.',
      },
      {
        title: 'Packets',
        copy: '`extrapackets` check. Detects more than one position packet per server tick. A vanilla client sends exactly one per game tick, so this is the most universal signature available and it catches packet replay generically rather than per client. `packetrate` check. Detects a sustained deviation of the movement packet rate from the server tick rate, gated on low ping and healthy tick rate.',
      },
      {
        title: 'World and information cheats',
        copy: 'Block obfuscation service. Valuable ores can be replaced with a decoy state before being written to the client, across a configurable hidden vertical band, with a stricter mode for deepslate and an optional container hiding mode. Mining analyser. Records every dig target against the region the server actually sent to that client. `miningbeyondview` check. Detects targeting valuable ores in a region the server never sent, which is knowledge the client could not legitimately have.',
      },
      {
        title: 'Prevention',
        copy: 'A configurable enforcement pipeline supporting set back position, teleport synchronisation, attack cancellation, block placement cancellation, block break cancellation and interaction cancellation. Every preventive action is gated on accumulated confidence and can be disabled globally. Disabling prevention never suppresses flagging or evidence recording. New `prevention.enabled` and `prevention.min-confidence` settings.',
      },
      {
        title: 'Confidence',
        copy: 'A confidence model that accumulates weighted signals from independent checks, retains the peak over a bounded window, and decays. Several weak signals can now contribute to a decision rather than one check firing once.',
      },
    ],
  },
  {
    version: '1.0.3-dev',
    stamp: '2026-09-30',
    items: [
      {
        title: 'Menus could be looted',
        copy: 'Inventory events were handled at HIGH priority with `ignoreCancelled = true`. If any other plugin cancelled a click first, Snuff skipped its own handler and the button items could be picked up, which is an item duplication vector. Handling now runs at HIGHEST priority and never skips. Number key swaps, offhand swaps, middle clicks and double click collect are additionally short circuited, and drag events are cancelled regardless of prior state.',
      },
      {
        title: 'Nuker was not detected',
        copy: 'The check counted *distinct* block positions per second. A nuker that repeatedly dug the same position, or burst many digs inside a short window, never tripped it. Detection is now on dig packet rate: three dig packets inside 700ms, or the distinct position signal as a second condition.',
      },
      {
        title: 'Wind charge flagged legitimate players',
        copy: 'A wind charge gives a large horizontal impulse and being hit by one gives knockback. Neither was modelled, so Fly, Speed, AirMovement, HighJump, LongJump, Drift, Step, NoFall, Velocity and PitchLock all flagged normal play. A wind charge now grants three seconds of grace after the player uses one and one second after the player is hit by one, applied centrally so every movement check inherits it. Detection is driven from real item use, projectile launch and damage events.',
      },
      {
        title: 'Spear and mace attributes were ignored',
        copy: 'Equipment tracked only mining attributes and could not tell a spear, mace or trident from any other item, so attribute changes from those weapons were invisible to the model. The held weapon type and whether it carries an attribute modifier are now tracked per tick on the main thread.',
      },
      {
        title: 'Manual punishments',
        copy: '`ban`, `timeout`, `tempban`, `ipban`, `tempipban`, `mute`, `tempmute` and `warn`, each with a reverse: `unban`, `untimeout`, `untempban`, `unipban`, `untempipban`, `unmute`, `untempmute` and `unwarn`. Duration syntax accepts `m`, `h` and `d` in any order and any combination, so `/snuff tempban frteddz 1h 10s cheating` works. Zero, negative, unitless and absurd durations are rejected. A reason is mandatory on every punish command. `/snuff punishments [player]` lists active punishments, online or offline. `/snuff warns <player>` lists warnings. Bans are enforced at pre-login, mutes block chat and commands, and everything persists per UUID across restarts. Staff caps via `snuffac.punish.maxduration.*`, and `snuffac.exempt.punish` protects staff from being punished. Snuff still never punishes on its own. Every one of these is a staff action and no check or report can reach it.',
      },
      {
        title: 'Settings menu',
        copy: '`/snuff settings` opens an admin menu for log retention, history retention, the prevention switch and the alert cooldown, with in place reload.',
      },
      {
        title: 'Simplified command surface',
        copy: '`/snuff` with no arguments now opens the menu instead of printing a command list. `/snuff violations [player]` opens the flagged players menu or that player\'s history rather than printing text. Flagged players and warned players are shown with their real skin when they are online.',
      },
    ],
  },
]
