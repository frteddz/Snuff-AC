# Changelog

All notable changes to Snuff AC are documented here.

The format is based on Keep a Changelog, and this project adheres to Semantic
Versioning.

## [1.1.0-dev] - 2026-09-30

Enforcement release. Nine defects and four features, most of them found by the user
playing on a live server rather than by the test suite. The headline item is that a
tempban did not prevent anything, and that is the kind of bug that makes staff stop
trusting a tool.

### Fixed

**A tempban did not stop a banned player rejoining** (entry 008)

- There was no login listener at all. `isBanned` was called in exactly one place, when
  lifting a ban, so a player who reconnected was never checked. `applyOnline` only fires
  for a player who is already online, which is why the kick worked and the reconnect did
  not. Enforcement was structurally incapable of stopping a returning player.
- An `AsyncPlayerPreLoginEvent` listener now disallows the connection on a live BAN or
  TEMPBAN, reading the same store the kick path uses, so the two cannot disagree.
- Reproduced twice by the user, banning an alt and then banning their own main account.
  Both rejoined while the ban was still running.

**`packetrate` flagged legitimate play and set the player back** (entry 015)

- The check compared a client's movement packet rate against a flat 20, which is the
  server tick rate. Those are not the same quantity. A vanilla client does not send a
  position packet every tick, so a player standing still sends well under the 0.80 lower
  bound, and three four second windows of not moving is about twelve seconds of standing
  still.
- A window is now discarded unless the player actually moved during it, the bands are
  wider, five consecutive windows are required instead of three, and a teleport or a
  recent low TPS resets the measurement rather than being averaged through.
- This is the second rate based check to fail this way after `packetspam` in v1.0.9. Both
  were measuring a client quantity against a fixed constant rather than against what the
  server actually did.

**Spear attribute swapping was never detected** (entry 011)

- `observedReach` read `Attribute.ATTACK_DAMAGE`, which is damage, not reach. The reach
  attribute on 1.21.11 is `ENTITY_INTERACTION_RANGE`. The code was asking how much damage
  a player did, storing it in a field called `observedAttackReach`, and comparing that
  damage number against a distance, so it would flag a weak weapon and miss a reach cheat.
- The expected reach was also never set. `attackReach` initialised to 3.0 and nothing ever
  wrote it, so even with the right attribute it compared every weapon against a hardcoded
  sword reach.
- Now reads `ENTITY_INTERACTION_RANGE`, derives the expected reach from the held weapon,
  compares in both directions rather than only looking for a value that is too low, and
  logs a debug line for a weapon whose reach is not observable instead of guessing.
- The v1.0.6 release notes claimed this was fixed. It was not, and it never had been.

**`/snuff reports` threw, and `/snuff report` opened a menu with dead buttons** (entry 014)

- `render()` called `Bukkit.getPlayer(target == null ? null : target.getUniqueId())`.
  The admin view is constructed with a null target on purpose, so the guard produced null
  and handed it to a method that rejects null. `IllegalArgumentException: UUID id cannot
  be null`, every time.
- In the picker, the same line set `renderPlayer` to the report target rather than to the
  person looking at the menu. `renderPlayer` is what the per button permission check
  consults, so every category button was checked against the wrong player, failed, and was
  never placed in the inventory. A menu that renders with no buttons looks like a working
  menu with unresponsive items.
- `openAdminReports` already called `forViewer(player)` before `build()`. `build()` then
  called `render()`, which overwrote the correct value. The `forViewer` call was dead.
  `renderPlayer` is now written in exactly one place.

**`/snuff menu` was advertised and did nothing** (entry 013)

- `menu` was in the tab completion list and in the documentation, and had no `case`
  anywhere, so it produced "unknown subcommand" three keystrokes after the server offered
  it. The bare `/snuff` worked, which is why it survived.
- Added the case, and a structural test asserting every advertised subcommand maps to a
  permission, so the three lists cannot silently disagree again.

**Player facing commands were unreachable** (entry 012)

- `plugin.yml` declared the command with `permission: snuffac.admin`, and Bukkit enforces
  that before the executor runs. So `/snuff report` and `/snuff version` did not work for a
  normal player at all, even though `snuffac.report` defaults to true. The two disagreed:
  a player was told they could report, opened a menu where nothing was clickable, and had
  no command to fall back on.
- The command-level permission is gone and each subcommand is gated individually.
- The permission tree is now three tiers and declares every node the code checks, so
  LuckPerms and other managers can actually grant them. A node that is not declared cannot
  be granted, and the failure is silent.
- Added `snuffac.teleport`, `snuffac.bypass.give`, `snuffac.reports.manage`,
  `snuffac.escalation.manage` and the four `snuffac.clear.*` nodes, none of which inherit
  from the punishment permissions.

### Added

**Punishment screens with real detail** (entry 009)

- Every ban, temporary ban, mute and kick screen now names the staff member who issued it,
  the exact expiry as a date and a time, the remaining duration, the reason, and how to
  appeal. A one minute ban tells the player when they may rejoin instead of telling them
  to run a command they cannot run while banned.
- Staff names and reasons are stripped of markup, since a reason is free text written by a
  person.

**Bypass, clear commands, and staff location** (entries 010, 016, 017)

- `/snuff bypass <player> [on|off]` grants or revokes the anticheat bypass, persists to
  `bypass.tsv`, and is now a real input to the exempt computation rather than a value that
  would be overwritten on the next refresh. Every grant and revoke is announced to staff.
- `/snuff clearflags`, `/snuff clearwarns` and `/snuff clearpunishments`, each with its own
  permission, each logging what was destroyed and by whom. Destructive, so they require
  confirmation, and clearing flags also resets the live session state so a cleared player
  is not still in violation a second later.
- `ViolationInfo` now carries world and position, so a flag records where it happened.
  `FlagsMenu` shows last seen world and coordinates per player, and `/snuff tp` teleports
  to a player or to their last known position, refusing if the chunk is not loaded. This is
  a breaking change to the public API, which is why it happens on a dev release.

**Reference plugin** (entry 017)

- `DonutSus-1.0.jar` was inspected for metadata only. It has no licence file and declares
  no licence in its bundled pom, so nothing was decompiled and no code was taken from it.
  Its dependency list is worth one note: it reads flags from Vulcan and Grim through their
  public APIs. Snuff should never do that. Two anticheats deciding about the same player
  without knowing what the other decided is a bad outcome for a server, and Snuff
  generating its own evidence and owning it is the correct architecture.

### Numbers

- 321 passing unit tests, up from 308
- 32 checks, unchanged
- 36 permission nodes, up from 19

### Not yet verified

- Nobody has clicked through the menus by hand. This is the fifth consecutive release to
  say so. The automated coverage now builds a menu and asserts items are placed, which
  would have caught tonight's two defects, but it is still not a human opening each tab.
- Anti-X-Ray and entity concealment still need a live check against a real cheat client.

## [1.0.9-dev] - 2026-09-30

Correctness release. v1.0.8 made detection real for the first time, and a legitimately
cheating player immediately found the three places where that exposed an arithmetic error
and two design mistakes. All five are fixed, prevention can no longer fire on a single
derived flag, and player reports plus fully owner-editable menus are in.

### Fixed

**packetspam flagged every player, including a stationary one**

- The rate was computed as `count * 1000 / (elapsed + 1)`. On the first packet of a
  window that reports 1000 packets per second, on the second 666, on the third 500. The
  window also started at an arbitrary packet rather than a clock boundary, so the bad
  arithmetic repeated forever. Against a 400 per second threshold, a player standing still
  was flagged every two seconds.
- The live log proved it: `packetsPerSecond=1000.0, threshold=400.0, count=1`.
- A window is now only evaluated once it has been open for at least 250ms, so the
  division always means something. The rate is a real count over a real interval, the
  window closes on the tick rather than on whichever packet arrived, a per-type breakdown
  is recorded as evidence, and the rate must hold across three consecutive windows before
  it alerts. This was broken for every player on every version since it was written.

**badpackets reset the player's position while they bridged**

- The check flagged any block interaction whose position did not match an "active dig".
  `digActive` stays true for the whole mining duration, so placing a block or breaking the
  next one while mining tripped it. Nobody had to be cheating, mining and building does it.
- The rule has been removed entirely. Dig behaviour belongs to FastBreak and Nuker, and a
  check called badpackets should only reject packets that cannot be decoded.
- The client attack cursor is no longer validated as if it were authoritative. Reach and
  AttackAngle already validate against the server-resolved hitbox.
- Only non-finite coordinates are still flagged immediately, because a NaN genuinely
  cannot come from a vanilla client. Out-of-bounds positions are buffered.
- The world border now matches the vanilla maximum of 29999984 rather than 30000000.

**Prevention could fire on a single flag from a check that guesses**

- v1.0.8 set `setback-threshold: 1.0` on 23 checks, so the first flag from a behavioural
  check teleported the player. For BadPackets that turned a chat message into a rubber
  band the player felt every two seconds while bridging.
- Every check now declares an `evidence` kind. `STRUCTURAL` checks are things that cannot
  legitimately happen and may act at their own threshold. `DERIVED` checks, which is
  everything that measures a rate, a ratio, an average or a window, are structurally
  unable to request a setback until one violation level past their alert threshold,
  regardless of what the config says.
- BadPackets, ImpossibleMovement, ImpossibleAttack, InvalidAttackState and GroundSpoof are
  structural. The other 27 are derived.

**Alerts did not use the Snuff prefix**

- Alerts were built by a completely separate formatter that took a plain `Snuff` from
  `general.alert-prefix` and wrapped it in hardcoded square brackets. `LegacyColour` was
  never involved, so the gradient never appeared. The brackets were in the format string,
  not in the prefix, which is why adding the real prefix would have double-bracketed it.
- Alerts now carry the same gradient prefix as `/snuff`, configured as
  `general.chat-prefix`, and converted through `LegacyColour` before MiniMessage sees it.
- The console line is rendered separately and is plain text, because a terminal cannot
  show a gradient. Hex codes are resolved rather than left as literal text.

**Anti-X-Ray was still guessed at**

- The v1.0.8 reflection chain through `world.getUnsafe().getWorldConfiguration()` does not
  exist. `org.bukkit.World` has no `getUnsafe`, `UnsafeValues` has no world-config
  access, and paper-api ships no anti-xray classes at all.
- `AntiXrayBridge` now tries four documented strategies in order and reports every one it
  attempted. The engine mode resolves against whatever enum constants the running server
  actually has. Setters are matched by signature and a missing one is reported by name
  rather than thrown.
- The diagnostic now logs the exception class, its message and the first four stack
  frames, instead of the literal text `null`.

**Entity concealment was one-way and never fired**

- The visible set was only populated in the legal branch, so a hide was only ever sent for
  something already known-visible, and on a player's first frame that set was empty.
- There was no `showEntity` call anywhere, so reveal-radius and reveal-padding could not
  work. A one-way hide is worse than no concealment.
- The pass now tracks an explicit visible or hidden state per entity per viewer, seeds it
  on the first pass, has a real reveal path, and drops state for entities that leave the
  world. It iterates the world's actual entities rather than the combat environment. When
  concealment is off or the viewer is exempt, everything is revealed.

### Added

**Player reports**

- `/snuff report <player>` opens a seven category picker: cheating, exploiting, explicit
  language, offensive behaviour, griefing, inappropriate name, and staff impersonation.
- `/snuff reports` opens the admin view, listing reports newest first with claim and
  resolve actions so two admins cannot both act on the same report and none is silently
  dropped.
- A cheating report attaches the target's flag count and most recent check from Snuff's own
  history, so the admin sees the evidence before the reporter's note.
- Reporter notes are stripped of markup and length limited, self reports are refused, staff
  holding `snuffac.exempt.punish` cannot be reported, and a reporter is rate limited to
  five reports per ten minutes. Reports persist to `plugins/SnuffAC/reports.tsv` with
  configurable retention, defaulting to 30 days.

**Every menu is now owner editable**

- Six menu files are written to `plugins/SnuffAC/GUI` on first run: `main-gui.yml`,
  `settings-gui.yml`, `flags-gui.yml`, `warned-gui.yml`, `reports-gui.yml` and
  `reports-admin-gui.yml`.
- Per item: material, display name, lore, slot, action, enabled, permission, stack amount
  and glint. Layout is configurable too, with rows, title and filler.
- A missing file falls back to built-in defaults, a malformed file is reported and ignored,
  and every slot, row count and stack amount is bounds checked so a bad file cannot index
  outside an inventory.
- Actions are an allowlist of identifiers, never a command string. An owner-supplied
  command in a menu is remote code execution on a shared server.
- Cancellation still does not depend on the registry lookup succeeding, so a future failure
  degrades to a cancelled click rather than a free item, which is the v1.0.5 duplication
  bug.

### Numbers

- 308 passing unit tests, up from 258
- 32 checks, unchanged

### Not yet verified

- Nobody has clicked through the menus by hand. This is the fourth consecutive release
  that says so. The menus are now data driven, which is different code from the version
  that was believed fixed three times.
- Anti-X-Ray and entity concealment still need a live check against a real cheat client
  before either can be claimed to work. v1.0.8 claimed both and delivered neither.

## [1.0.8-dev] - 2026-09-30

Strictness release. Detection was effectively inert because the shipped thresholds were
leftover test tuning, and prevention was off entirely. Both are fixed, attacks are now
cancelled rather than only reported, and rendering cheats are neutralised by withholding
data the client is not entitled to.

### Fixed

**Detection was muted by the shipped thresholds**

- Every one of the 31 checks shipped with a `buffer-threshold` of 20 to 30 while checks
  only add 5 to 10 buffer per flag, a `buffer-decay` of 0.5 to 0.6 applied every tick, and
  an `alert-threshold` of 4 to 5 on top. Reaching a single alert needed roughly 3 to 6
  consecutive flags to cross the buffer and then 4 to 5 more crossings to reach the alert
  threshold, with decay eating the buffer between bursts. In practice tens of consecutive
  cheat actions were needed, and burst cheating never alerted at all.
- Hard checks now ship with a buffer threshold of 1.0, a decay of 0.1 and an alert
  threshold of 1.0, so one clear detection reports. Statistical checks use an alert
  threshold of 2.0.
- The hardcoded fallback in `CheckConfig.defaults` carried the same muted values and is now
  strict as well, so a check with no shipped config still reports.
- `setback-threshold` was 0.0 on all 31 checks, which made `setbacksEnabled()` false
  everywhere, so no check could prevent anything even after flagging. Prevention thresholds
  are now 1.0 for hard checks.

**Anti-xray never ran on modern Paper**

- Obfuscation was applied through the removed `com.destroystokyo.paper` config classes, so
  on Paper 1.21 the server logged a `ClassNotFoundException` at every startup and shipped
  raw ore data to every client. X-Ray and Block ESP had full data to work with.
- It now resolves the current Paper `AntiXrayConfiguration` through `getUnsafe` and applies
  `OBFUSCATE` with proper `BlockData` block lists.
- When no world can be configured the plugin now says so plainly, naming X-Ray and storage
  ESP as still unblocked, rather than logging a reflection stack trace.

**The permission tree was invalid**

- `plugin.yml` had a stray `snuffac.sounds` key with no value inside the `snuffac.admin`
  children block, plus a malformed `snuffac.bypass` and a duplicated `snuffac.menu`. Paper
  rejected the whole `snuffac.admin` node on every load, so the intended inheritance never
  applied. The tree now parses cleanly with eighteen nodes and twelve valid children.

**Prevention routing did nothing**

- `CANCEL_ATTACK` called `packetModificationEnabled(false)`, a flag that was written but
  never read anywhere, so enforcement silently accomplished nothing. Attacks, placements
  and interactions now route to a real prevention signal.

### Added

**Prevention that actually prevents**

- A prevention signal per player that checks request through `preventAttack`,
  `preventPlacement`, `preventInteraction` and `requestSetback`. The packet gate runs at
  `LOWEST` priority so a cancelled attack never reaches the server.
- Attack packets are now dispatched synchronously on arrival rather than queued, because a
  decision made a tick later cannot cancel the packet that has already landed.
- `EntityDamageByEntityEvent` cancellation as a second line of defence, so an illegal hit
  is stopped even if the packet was already in flight.
- Speed, Fly and HighJump now request a setback to the last legal position instead of only
  reporting.

**Hitbox verification**

- `HitboxVerifier` casts the attacker's actual look vector against the true vanilla
  hitbox, 0.6 by 1.8 with a 1.5 sneaking height, using a slab method against the box
  rather than a distance check.
- `AttackAngleCheck` rejects hits that landed only on an expanded hitbox, tracking a streak
  so a single odd frame is not punished, and cancels the attack. This catches the
  Hitboxes cheat that Reach alone cannot see.
- Reach now cancels the attack instead of only flagging, and reports the ray result,
  angle, and reject reason as evidence.

**Aim analysis**

- `GcdAnalysis` learns the player's own mouse constant from a rolling window of pitch and
  yaw deltas, then flags rotation deltas that are not a multiple of it. Human mouse input
  always lands on the grid; synthetic aim usually does not.
- KillAura now detects rapid target switching between entities far apart in angle, and
  cancels the attack.

**Visual cheats are neutralised, not just logged**

- Entity hiding. Players and mobs with no legal line of sight are hidden from the client
  entirely, so Player ESP and tracers have nothing to reveal. The pass runs every 4 ticks,
  reveals anything within 16 blocks so close fights never break, and reveals early inside
  24 blocks once a raycast confirms the view is about to open, so nothing pops in.
- Sound fuzzing. Sounds carrying a position, footsteps, eating, drinking, bow draws, attacks
  and armour, are nudged when the emitter is behind cover, so sound radar and sound ESP
  cannot be used to find players.
- `tuning.profile` in `config.yml` with `strict` as the default. Strict scales movement
  tolerance to 0.5 and reach tolerance to 0.6, so the margins narrow without editing
  thirty one check blocks by hand.
- The plugin logs its effective profile and counts on startup, and warns when any check is
  tuned so loosely that ordinary cheating will not alert.

### Changed

- The prefix now closes with a reset, so the gradient colour and the bold and italic
  decorations stop at the bracket instead of bleeding into the message text.
- Added `tuning.profile` and a `visual` section to `config.yml`.

### Known

- Rendering cheats cannot be detected, only prevented, because the client decides what to
  draw. Anti-xray needs the server to obfuscate, entity hiding needs a line of sight pass,
  and both depend on `anti-xray.mode` being on.
- Folia is still unsupported.
- The warning ladder is still the only automatic action and is still off by default.

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

## [1.0.1-dev] - 2026-09-30

Major detection and prevention phase, driven by a defensive study of seven open source
Minecraft cheat clients and by false positives observed during live play.

### Research

- Studied LiquidBounce, Wurst, Meteor, Lambda, ThunderHack Recode, BleachHack and
  3arthh4ck. Licences read before use: six GPLv3, one MIT.
- Four requested repositories (Aoba, Phobos, Aristois, Inertia) are no longer publicly
  available. Each was verified as HTTP 404 and absent from the owner listing, so none
  contributed.
- No cheat client source code, comments, identifiers, configuration or strings were copied.
  Every system below was implemented independently. Documented in `credits.md`.

### Added

**Combat**

- Entity resolution. Attacks are now measured against the server's own hitboxes instead of
  the client supplied cursor, with correct survival (3.0), creative (5.0) and vehicle (5.0
  and 8.0) reach limits, crouch aware eye height, and a vertical padding.
- Line of sight testing between the eye and the target box, sampled along the segment.
- `criticals` check. Detects forced criticals produced by emitting extra position packets
  with a vertical lift too small for gravity, immediately before an attack.
- `rotationsnapback` check. Detects a large aim rotation immediately before an attack
  followed by a reverse rotation immediately after, which human input does not produce.

**Movement**

- `groundflag` check. Detects sustained ground contact claims that contradict the server
  block view, the signature of air walk and ground flag no fall spoofing.
- `pitchlock` check. Detects pitch pinned to an exact constant (straight down, or a fixed
  glide angle), which placement and glide modules use to defeat server heuristics.
- `drift` check. Detects a sustained constant per tick offset between the prediction and
  the reported position, which is how several cheats disguise position edits. The
  discriminator is the drift rate, not its magnitude, so ordinary jitter does not trigger it.

**Packets**

- `extrapackets` check. Detects more than one position packet per server tick. A vanilla
  client sends exactly one per game tick, so this is the most universal signature available
  and it catches packet replay generically rather than per client.
- `packetrate` check. Detects a sustained deviation of the movement packet rate from the
  server tick rate, gated on low ping and healthy tick rate.

**World and information cheats**

- Block obfuscation service. Valuable ores can be replaced with a decoy state before being
  written to the client, across a configurable hidden vertical band, with a stricter mode
  for deepslate and an optional container hiding mode.
- Mining analyser. Records every dig target against the region the server actually sent to
  that client.
- `miningbeyondview` check. Detects targeting valuable ores in a region the server never
  sent, which is knowledge the client could not legitimately have.

**Prevention**

- A configurable enforcement pipeline supporting set back position, teleport
  synchronisation, attack cancellation, block placement cancellation, block break
  cancellation and interaction cancellation.
- Every preventive action is gated on accumulated confidence and can be disabled globally.
  Disabling prevention never suppresses flagging or evidence recording.
- New `prevention.enabled` and `prevention.min-confidence` settings.

**Confidence**

- A confidence model that accumulates weighted signals from independent checks, retains
  the peak over a bounded window, and decays. Several weak signals can now contribute to a
  decision rather than one check firing once.

### Changed

- The world cache now carries block material names, not only physical classification, so
  ore identification is real rather than inferred.
- Reach prefers resolved hitbox distance and falls back to the cursor only when the target
  is unknown, and records which basis was used in the evidence.

### Honest limitations

- X-Ray, block ESP, ore search, entity ESP and storage ESP require nothing extra from the
  server. They are render time predicates over data a vanilla client already receives, so
  no protocol level detection is possible. Snuff AC reduces the information volunteered and
  detects the consequences, and `credits.md` records this rather than implying coverage.
- Fullbright and other purely local rendering changes are not detectable and are not
  checked. No placeholder check was added for them.
- ViaVersion and Geyser are not yet modelled. Bedrock clients move differently and older
  protocol versions have different movement semantics, so false positives are likely on
  either.
- The movement predictor still does not model the post 1.8.2 skipped tick behaviour, and
  still does not simulate collisions, so speed related thresholds remain untuned.

### Verified

- 130 unit tests pass, up from 94.
- Clean build with 31 checks registered.
- Zero code comments and zero em dash characters across the project.

## [1.0.2-dev] - 2026-09-30

First release driven by live bug reports rather than by feature ideas. 31 checks,
138 unit tests, up from 130.

### Verified

- 138 unit tests pass, up from 130.
- Clean build with 31 checks registered.

### Fixed

- Alerts never reached staff in game. The alert path ran on the check thread, where the
  online player lookup returns nothing, so the console worked while chat silently
  produced no recipients. Delivery now runs on the main thread, and each recipient is
  isolated so one failure cannot abort the rest. The Velocity messenger had empty
  broadcast and console bodies, so alerts reached nobody on Velocity at all.
- Flag history was discarded the moment a player quit, which made reconnecting the
  cheapest way to wipe a record. History is now persisted per UUID with retention, a per
  player cap and an async writer, and staff are notified when a player returns carrying
  flags.
- The enforcement pipeline was unreachable. Nothing called it, so no confidence ever
  accumulated and no setback or cancel ever ran. It is now routed from the violation
  path and gated on confidence.
- The auto punishment path was deleted rather than guarded. The command execution
  interface and its call site were removed along with the dead state they used. All 31
  checks still ship with alert only.
- MiningBeyondView could never fire. The world cache is a three block box, so the
  material at any real dig target was always null. Dig positions are now probed on the
  main thread, and the chunk distance maths compares relative chunk coordinates instead
  of the difference of two radii.
- Reach read creative mode as a hardcoded false and never evaluated line of sight. The
  combat environment now carries the game mode, and the check samples the segment from
  eye to target.
- Three false positive sources were removed. PitchLock required only pitch near ninety
  for six ticks, which flags anyone glancing down; it now requires bit constant pitch
  while placing, mining, gliding or airborne. Critical counted ordinary airborne movement
  and now requires genuine extra packets in the same tick. Drift read an offset another
  check happened to leave behind and now reads a centrally tracked delta.

### Added

- A staff menu with a flagged players list, opened with `/snuff`. Commands are unchanged
  and still work from console. Inventory events are cancelled and permissions are
  rechecked at click time.
- A staff permission for the menu, a per staff verbose alert toggle, and an on and off
  form for the alerts command.
- Eight regression tests, and a fix so the artifact name derives from the project
  version instead of drifting behind it.

## [1.0.3-dev] - 2026-09-30

Staff tooling release, driven by a second round of live bug reports.

### Fixed

**Menus could be looted**

- Inventory events were handled at HIGH priority with `ignoreCancelled = true`. If
  any other plugin cancelled a click first, Snuff skipped its own handler and the
  button items could be picked up, which is an item duplication vector.
  Handling now runs at HIGHEST priority and never skips. Number key swaps,
  offhand swaps, middle clicks and double click collect are additionally
  short circuited, and drag events are cancelled regardless of prior state.

**Nuker was not detected**

- The check counted *distinct* block positions per second. A nuker that repeatedly
  dug the same position, or burst many digs inside a short window, never tripped
  it. Detection is now on dig packet rate: three dig packets inside 700ms, or the
  distinct position signal as a second condition.

**Wind charge flagged legitimate players**

- A wind charge gives a large horizontal impulse and being hit by one gives
  knockback. Neither was modelled, so Fly, Speed, AirMovement, HighJump,
  LongJump, Drift, Step, NoFall, Velocity and PitchLock all flagged normal play.
  A wind charge now grants three seconds of grace after the player uses one and
  one second after the player is hit by one, applied centrally so every movement
  check inherits it. Detection is driven from real item use, projectile launch
  and damage events.

**Spear and mace attributes were ignored**

- Equipment tracked only mining attributes and could not tell a spear, mace or
  trident from any other item, so attribute changes from those weapons were
  invisible to the model. The held weapon type and whether it carries an
  attribute modifier are now tracked per tick on the main thread.

### Added

**Manual punishments**

- `ban`, `timeout`, `tempban`, `ipban`, `tempipban`, `mute`, `tempmute` and
  `warn`, each with a reverse: `unban`, `untimeout`, `untempban`, `unipban`,
  `untempipban`, `unmute`, `untempmute` and `unwarn`.
- Duration syntax accepts `m`, `h` and `d` in any order and any combination, so
  `/snuff tempban frteddz 1h 10s cheating` works. Zero, negative, unitless and
  absurd durations are rejected.
- A reason is mandatory on every punish command.
- `/snuff punishments [player]` lists active punishments, online or offline.
- `/snuff warns <player>` lists warnings.
- Bans are enforced at pre-login, mutes block chat and commands, and everything
  persists per UUID across restarts.
- Staff caps via `snuffac.punish.maxduration.*`, and `snuffac.exempt.punish`
  protects staff from being punished.
- Snuff still never punishes on its own. Every one of these is a staff action
  and no check or report can reach it.

**Settings menu**

- `/snuff settings` opens an admin menu for log retention, history retention,
  the prevention switch and the alert cooldown, with in place reload.

**Simplified command surface**

- `/snuff` with no arguments now opens the menu instead of printing a command
  list. `/snuff violations [player]` opens the flagged players menu or that
  player's history rather than printing text.
- Flagged players and warned players are shown with their real skin when they
  are online.

### Verification

- 157 unit tests pass, up from 138.

## [1.0.4-dev] - 2026-09-30

Detection and false positive pass, built against a written study of how
established anticheats actually discriminate. Every threshold and rule below
comes from that study rather than from guesswork. No code was copied.

### False positive fixes

- A join grace of five seconds now applies to every movement check. The join
  timestamp was recorded but never read, so a player's first five seconds of
  movement, which is exactly when the world cache is still filling and the
  predictor has no history, were checked against a cold model.
- A global lag gate now suppresses movement checks below 18 TPS and above
  300ms ping. Timing and position checks are meaningless when the server or
  the connection is struggling, and this was only applied to the Timer check
  before. An unmeasured TPS is not treated as lag.
- Fly now exempts the full documented list: riptiding, slow falling, levitation,
  recent knockback and a recent block change, in addition to the water,
  elytra, vehicle and climbable cases it already handled. Each is now driven
  by real potion, item and damage events.
- ExtraPackets was flagging at three position packets in a tick. A vanilla
  client sends one, but 1.8 clients, Bedrock and any client under packet
  aggregation legitimately send two to three, and four is still within normal
  variance. The threshold is now four per tick and it must be sustained across
  six consecutive ticks before flagging, which is what the study describes as
  the signal rather than a single busy tick.

### Detection additions

- Post attack timing. An attack sent immediately after a movement packet, which
  is what a targetting assist does and a human does not, is now recorded and
  compared against the normal 2 to 60ms gap.
- Multi target analysis. Attacking three or more distinct entities inside one
  second, each immediately after a movement packet, is flagged. Rotation is now
  also compared against the angle to the target, and an attack on an entity more
  than 90 degrees from the facing direction is flagged as hitting outside the
  normal view.
- Dig reach validation. Starting a dig on a block more than six blocks away, in
  a chunk the server has actually loaded, is flagged. This is the ghost dig
  class of abuse, where the client targets a position the server never told it
  about.

### Verification

- 165 unit tests pass, up from 157, with dedicated coverage for the join grace,
  the lag gate and both wind charge windows.

## [1.0.5-dev] - 2026-09-30

Escalation release. The engine could prove a player was cheating but had
nothing to do about it beyond alerting, so detection never turned into a
consequence.

### Fixed

- Menu back buttons were dead. They opened the parent inventory directly
  instead of going through the menu open path, which left the stale child
  menu registered as the player's open menu. Every click after going back
  resolved against the wrong inventory, so the whole tab set appeared inert.
  Navigation now re-registers correctly, which fixes the flagged players
  list, the warnings list and the settings menu at the same time.

### Added

- A warning ladder. Each confident flag records one warning against the
  player. When the warning count reaches the configured limit, one timed ban
  is applied. The ban screen states which check it was for, how many warnings
  led to it, and that a false detection can be appealed with the admins once
  it expires.
- The ladder is keyed by UUID, so disconnecting does not clear it, and it has
  a cooldown so one burst of packets cannot consume the entire warning
  allowance. A manual staff punishment or unban resets it, so staff always
  have the final say.
- It only acts on confident detections, and a warning limit of one is the
  floor, so a misconfigured server cannot ban on the first flag.
- Configurable under `escalation`, including a warn only mode that stops at
  the limit and never bans, and a settings menu control to toggle the ladder
  and change the warning limit without editing the file.
- Escalation is off by default. It should only be enabled after thresholds
  are tuned on the target server, since no automatic ban can be risk free.

### Corrected

- plugin.yml carried a hardcoded version instead of the build template, so
  every build since v1.0.2 reported 1.0.2-dev at runtime regardless of the
  real version. The template is restored and the version is verified by
  reading it back out of the built artifact.

### Verification

- 176 unit tests pass, up from 165, including coverage that the first flag
  never bans, that the ban lands exactly on the configured limit, that a
  reconnect cannot clear the ladder, that a burst of packets cannot burn
  warnings, and that the ban reason names the check and the appeal path.

## [1.0.6-dev] - 2026-09-30

Closes the whole v1.0.5 bug report. 31 checks, 193 unit tests, up from 176.

### Fixed, urgent

- Menus no longer hand out items. Every button in every tab was inert and the
  button items could be taken into the player's own inventory, which is an item
  duplication vector with real items. The cause was a lifecycle race: opening a
  menu registered it, then Bukkit fired the close event for the previous screen,
  and that handler deleted the registration of the new one. The registry now
  stores the menu together with its inventory, and a close event only clears the
  entry when the menu and the inventory both match what is actually closing.
  Cancellation no longer depends on that lookup succeeding, so a future
  failure degrades to a cancelled click rather than a free item.
- The warning ladder can be enabled again. The settings toggle set the value and
  then immediately re-read the config file, which ships the ladder disabled, and
  overwrote it. Settings now persist to the file, and the same method was adding
  the violation listener on every click, which leaked a listener per click.
- Log and history retention take a typed value in chat instead of a stepper.
  Stepping from 1 to 365 one click at a time was not usable. Typing cancel keeps
  the current value. The prompt times out so an ignored prompt cannot lock
  someone out of the menu, validates the number, sanitises the input, allows one
  pending prompt per player, re-checks permission when it completes, and always
  answers with success or failure. The alert cooldown keeps its stepper.

### Fixed

- Every placeholder in the punishment path now substitutes. Call sites passed the
  angle brackets as part of the key while the renderer added its own, so the
  lookup was for a doubled string that appears nowhere. This affected the
  usage, player, duration and input placeholders, and it would have become more
  visible once the tags rendered, because a real player name would have been
  replaced by the literal word player.
- Messages render properly and every Snuff line carries the supplied gradient
  prefix. There were two message senders and only one understood the markup
  language, which is why the admin commands looked right and the punishment
  commands did not. There is now one shared sender used by the command path, the
  punishment path, the menu path, the mute and warn notices and the rejoin
  notice. The legacy hex form is accepted as input, so text generated by the
  RGBirdflop tool works verbatim, with no runtime dependency on that service.
- Punishments print a readable sequential id instead of the first eight
  characters of a UUID, which was an unreadable hex fragment such as 000001a0.
- The ban screen now names the appeal route explicitly.
- The rejoin notice reads as a line rather than a sentence with a bracketed
  prefix glued to it, and announces the count once.

### Added

- Attribute swapping to a spear is now detected. The weapon fields existed and
  were written every tick, but no check read them, so the engine learned the
  held item every tick and never looked at it. The server side attack
  attribute is now observed and compared against what the held weapon should
  give, and reach consumes it. This keys off the attribute value rather than the
  item, so a legitimate spear user is never flagged simply for holding one.

### Verification

- 193 unit tests pass, up from 176. New coverage includes the exact prefix
  string supplied, hex and decoration conversion, malformed input, and the menu
  registry lifecycle, including the case that caused the duplication bug.
- Two of the new registry tests failed on the first run and caught a real
  mistake in the fix, where the stored menu was being compared against the
  closing inventory so the entry could never clear. The registry now stores both
  and compares like with like.

## [1.0.7-dev] - 2026-09-30

Detection accuracy and usability release. 31 checks, 201 unit tests, up from 193.

### Fixed, the most important one in the project so far

- **Ping was never measured.** The engine had a working ping recorder that also feeds the
  min, max and smoothed values the tolerance model uses, and nothing on Paper ever called
  it. Every alert reported 0ms. The user, who actually has 50 to 60ms, was told 0ms.
  Paper now reads the player ping every tick and feeds the model.
- This is not a display bug. The tolerance model is documented as widening its allowance
  as latency rises so that nobody is punished for their ping, and that entire term had
  been inert since the project began. Every tolerance decision has been made as if every
  player had zero latency. It is a plausible contributing cause of the false flagging
  reported throughout testing.
- Ping now reports as unknown rather than a fake 0 when it has not been measured, so a
  missing measurement can never again be read as a real zero.

### Fixed

- **The gradient prefix is now rendered.** The legacy converter was applied to the message
  body but never to the prefix, so the prefix reached players as literal `&` and `#`
  characters. The prefix and the join now live in `LegacyColour`, which has no
  dependencies and is directly testable.
- The warnings listing was still building its own plain `[Snuff]` string instead of using
  the shared sender, so that one path would have stayed wrong even after the prefix fix.
  Both paths are routed now, and the audit is for every literal prefix, not one.
- Flag history no longer prints a raw epoch integer. It shows a real date and time plus a
  relative value, which is what was originally asked for back in v1.0.2.
- Tab completion covers the punishment commands and every subcommand added since the
  completer was written. The previous list was hardcoded and contained none of them, so
  `/snuff mute` was not special, every new subcommand was missing. Player names, offline
  known names, and duration suggestions are all offered now.
- The retention prompt accepts the same duration syntax the punishment commands teach, so
  `5d` works instead of being rejected as not a number. It no longer consumes the first
  chat message it sees, so unrelated chatter passes through to chat and leaves the prompt
  standing. A message that is not a value attempt is not cancelled at all.
- Command feedback rewritten to be calmer. Errors are red and short, hints are grey,
  usage lines no longer shout.

### Added

- **Mute command allowlist.** Muted players previously could not run a single command.
  A new `mute.allowed-commands` list lets an owner grant specific commands. The default
  is an empty list, so an owner who configures nothing keeps today's strict behaviour.
  Names are matched case insensitively, with any leading slash stripped and plugin
  namespaces supported, because a client sends the bare name and a slash would silently
  never match.
- **Sound effects** on menus and command outcomes, limited to meaningful moments rather
  than every button: opening a menu, a successful action, a rejected action, a punishment
  applied. Individual staff can silence their own with `/snuff sounds off` without
  changing it for everyone, and a new `snuffac.sounds` permission covers it.
- The flag history line now reads as a sentence, with the check, the violation level, the
  ping and the time, rather than a run of values with a raw timestamp.

### Verification

- 201 unit tests pass, up from 193.
- New coverage is deliberately aimed at the integration rather than the mechanism, since
  that is how the previous three releases each slipped through: the prefix is now asserted
  through the join that was actually broken, and the body is checked for surviving legacy
  codes, the presence of all seven gradient stops, and safety on a null or malformed body.
