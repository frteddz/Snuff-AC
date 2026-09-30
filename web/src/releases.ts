export type ReleaseItem = { group: string | null; title: string; copy: string }
export type Release = {
  version: string
  tag: string
  stamp: string
  pre: boolean
  count: number
  items: ReleaseItem[]
}


export const releases: Release[] = [
  {
    version: '1.0.0-dev',
    tag: 'v1.0.0-dev',
    stamp: '2026-09-29',
    pre: true,
    count: 41,
    items: [
      {
        group: 'Engine',
        title: 'Multi module Gradle build targeting Java 21, split into a stable public API module, a',
        copy: 'platform neutral core, separate Paper and Velocity platform modules, and a shading and packaging module.',
      },
      {
        group: 'Engine',
        title: 'A normalised, immutable packet model.',
        copy: 'The engine never sees a platform type.',
      },
      {
        group: 'Engine',
        title: 'A dedicated single threaded check executor with deterministic ordering, fed by a queue',
        copy: 'from the network thread. Packet arrival times are captured in nanoseconds at the edge so timing information survives the hand off.',
      },
      {
        group: 'Engine',
        title: 'A platform service layer covering scheduling, messaging, permissions and world access.',
        copy: '',
      },
      {
        group: 'Engine',
        title: 'Immutable per player world caching, built on the main thread each tick and read without',
        copy: 'locking by the check thread, so no server API call ever happens off thread.',
      },
      {
        group: 'Movement model',
        title: 'A vanilla kinematics model reproducing the documented speeds: 0.2806 blocks per tick',
        copy: 'sprinting, 0.2159 walking, 0.0648 sneaking, terminal velocity 3.92, jump velocity 0.42. All of these are asserted as unit tests.',
      },
      {
        group: 'Movement model',
        title: 'Attribute driven modelling of movement speed, gravity, jump strength, step height and',
        copy: 'safe fall distance, read from the server each tick.',
      },
      {
        group: 'Movement model',
        title: 'Block classification into a platform neutral taxonomy carrying slipperiness,',
        copy: 'passability, climbability and liquid state, so the physics never depends on Bukkit materials.',
      },
      {
        group: 'Movement model',
        title: 'Input space enumeration with best fit selection, because the server cannot observe which',
        copy: 'keys a player is holding.',
      },
      {
        group: 'False positive control',
        title: 'A tolerance model that accumulates per axis forgiveness from named sources, decays it',
        copy: 'over time, and caps the total. Sources include external pushes, pistons, bouncy blocks, item use slowdown, attack slowdown, vehicles, server knockback, explosions, riptide, block changes, chunk loads, teleports, setbacks, high latency, low TPS and game mode.',
      },
      {
        group: 'False positive control',
        title: 'Leniency carry over, granting a capped fraction of the previous offset as extra',
        copy: 'tolerance on the tick after a flag, which prevents a flag followed by unrestricted movement.',
      },
      {
        group: 'False positive control',
        title: 'Latency and tick rate adaptive tolerance, both capped, so high ping and low TPS are',
        copy: 'never punished.',
      },
      {
        group: 'False positive control',
        title: 'An applicability gate per check so a check is skipped entirely when its assumptions do',
        copy: 'not hold, rather than being allowed to produce misleading evidence.',
      },
      {
        group: 'Violation system',
        title: 'Evidence buffers with proportional adds, decay and capping, kept separate from',
        copy: 'violation levels so short term confidence and accumulated history are distinct.',
      },
      {
        group: 'Violation system',
        title: 'Separate alert and violation thresholds, where the alert threshold gates only the',
        copy: 'alert and never the recording of the violation.',
      },
      {
        group: 'Violation system',
        title: 'Configurable setback with a required payload and player opt out, dispatched on the',
        copy: 'main thread.',
      },
      {
        group: 'Violation system',
        title: 'Configurable punishment actions: none, alert, log, command, setback, kick.',
        copy: 'Command actions honour a threshold and a cooldown.',
      },
      {
        group: null,
        title: 'Checks, 23 total, all enabled by default',
        copy: 'Movement: `fly`, `speed`, `nofall`, `airmovement`, `groundspoof`, `step`, `highjump`,.`longjump`, `impossiblemovement`, `velocity` Combat: `reach`, `autoclicker`, `aim`, `killaura`, `impossibleattack`,.`invalidattackstate` World: `fastbreak`, `fastplace`, `scaffold`, `nuker` Packet: `badpackets`, `packetspam`, `timer`',
      },
      {
        group: 'Operations',
        title: '`/snuff` with `info`, `version`, `reload`, `checks`, `toggle`, `debug`, `alerts`,',
        copy: '`violations`, `profile`, `setback` and `stats`, with tab completion.',
      },
      {
        group: 'Operations',
        title: 'Template driven alerting with placeholders, per player rate limiting, configurable',
        copy: 'verbosity, and per player alert toggles.',
      },
      {
        group: 'Operations',
        title: 'Daily violation log files with retention based pruning.',
        copy: '',
      },
      {
        group: 'Operations',
        title: 'Debug output exposing position, velocity, ground state, air time, ping, tick rate,',
        copy: 'tolerance and per check state.',
      },
      {
        group: 'Operations',
        title: 'Four permissions: `snuffac.admin`, `snuffac.debug`, `snuffac.alerts`, `snuffac.bypass`.',
        copy: '',
      },
      {
        group: 'Operations',
        title: 'Configuration split into `config.yml` for global settings and `checks.yml` for per',
        copy: 'check settings, with toggle changes persisted.',
      },
      {
        group: 'Platform support',
        title: 'Paper 1.21.11: full check set, tested end to end against a real server.',
        copy: '',
      },
      {
        group: 'Platform support',
        title: 'Purpur: same build, detected at runtime, no Purpur specific code.',
        copy: '',
      },
      {
        group: 'Platform support',
        title: 'Velocity 3.4.0: packet category and network timing, with world and combat geometry',
        copy: 'checks reported inactive because a proxy has neither block data nor player positions.',
      },
      {
        group: null,
        title: 'Developer API',
        copy: 'A separate `snuffac-api` artifact exposing the engine handle, a read only check list, violation information and a listener interface.',
      },
      {
        group: null,
        title: 'Testing',
        copy: '91 unit tests covering kinematics against documented vanilla speeds, position packing, angle wrapping, buffers, the tolerance model, configuration, the check registry, prediction accuracy and input recovery, and end to end engine behaviour including exemptions, disabled checks and immediate flags. Tested against Paper build 132 on Java 21, with a scripted client driving real movement packets.',
      },
      {
        group: null,
        title: 'Plugin enables cleanly, PacketEvents 2.14.0 loads, 23 checks register, zero errors.',
        copy: '',
      },
      {
        group: null,
        title: 'Player tracking, latency measurement, alerts, the violation log and the file logger all',
        copy: 'work.',
      },
      {
        group: null,
        title: 'Legitimate walking, sprinting and sprint jumping produced **zero** violations, which is',
        copy: 'the false positive result that matters most.',
      },
      {
        group: null,
        title: 'Simulated flight was detected by `fly` with evidence including air time, delta and',
        copy: 'velocity, and the violation level escalated 4.0 to 8.0 to 12.0, at which point the setback threshold was reached and the level reset. The full alert to evidence to setback to reset path is confirmed working.',
      },
      {
        group: null,
        title: 'Commands, tab completion, configuration reload, check toggling and persistence verified.',
        copy: '',
      },
      {
        group: null,
        title: 'Detection was validated against a scripted client, not against a range of real cheat',
        copy: 'clients, and not against high latency, packet loss or low tick rate conditions. Thresholds are conservative starting points and will need tuning against real traffic.',
      },
      {
        group: null,
        title: 'The world cache reads the server\'s world rather than a replica of the client\'s',
        copy: 'believed world, so client side block prediction is forgiven through the tolerance model rather than modelled directly.',
      },
      {
        group: null,
        title: 'Break time uses a coarse material hardness and tool speed table, not full per block',
        copy: 'tool tier accuracy.',
      },
      {
        group: null,
        title: 'The predictor does not yet model the post 1.8.2 skipped tick behaviour, which is a',
        copy: 'known source of both false negatives and false positives.',
      },
      {
        group: null,
        title: 'No automatic bans.',
        copy: 'Alerts, logging and setbacks only, by deliberate choice until the detection system is further validated.',
      },
      {
        group: null,
        title: 'Velocity support is limited to packet level checks, as a proxy has no world data and no',
        copy: 'player position API.',
      },
      {
        group: null,
        title: 'Folia is not supported.',
        copy: 'Licensed under the GNU General Public License v3.0, required because the bundled PacketEvents library is GPLv3. The reasoning and the licence of every dependency and every researched project are documented in `credits.md`.',
      },
    ],
  },
  {
    version: '1.0.1-dev',
    tag: 'v1.0.1-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 24,
    items: [
      {
        group: 'Combat',
        title: 'Entity resolution.',
        copy: 'Attacks are now measured against the server\'s own hitboxes instead of the client supplied cursor, with correct survival (3.0), creative (5.0) and vehicle (5.0 and 8.0) reach limits, crouch aware eye height, and a vertical padding.',
      },
      {
        group: 'Combat',
        title: 'Line of sight testing between the eye and the target box, sampled along the segment.',
        copy: '',
      },
      {
        group: 'Combat',
        title: '`criticals` check.',
        copy: 'Detects forced criticals produced by emitting extra position packets with a vertical lift too small for gravity, immediately before an attack.',
      },
      {
        group: 'Combat',
        title: '`rotationsnapback` check.',
        copy: 'Detects a large aim rotation immediately before an attack followed by a reverse rotation immediately after, which human input does not produce.',
      },
      {
        group: 'Movement',
        title: '`groundflag` check.',
        copy: 'Detects sustained ground contact claims that contradict the server block view, the signature of air walk and ground flag no fall spoofing.',
      },
      {
        group: 'Movement',
        title: '`pitchlock` check.',
        copy: 'Detects pitch pinned to an exact constant (straight down, or a fixed glide angle), which placement and glide modules use to defeat server heuristics.',
      },
      {
        group: 'Movement',
        title: '`drift` check.',
        copy: 'Detects a sustained constant per tick offset between the prediction and the reported position, which is how several cheats disguise position edits. The discriminator is the drift rate, not its magnitude, so ordinary jitter does not trigger it.',
      },
      {
        group: 'Packets',
        title: '`extrapackets` check.',
        copy: 'Detects more than one position packet per server tick. A vanilla client sends exactly one per game tick, so this is the most universal signature available and it catches packet replay generically rather than per client.',
      },
      {
        group: 'Packets',
        title: '`packetrate` check.',
        copy: 'Detects a sustained deviation of the movement packet rate from the server tick rate, gated on low ping and healthy tick rate.',
      },
      {
        group: 'World and information cheats',
        title: 'Block obfuscation service.',
        copy: 'Valuable ores can be replaced with a decoy state before being written to the client, across a configurable hidden vertical band, with a stricter mode for deepslate and an optional container hiding mode.',
      },
      {
        group: 'World and information cheats',
        title: 'Mining analyser.',
        copy: 'Records every dig target against the region the server actually sent to that client.',
      },
      {
        group: 'World and information cheats',
        title: '`miningbeyondview` check.',
        copy: 'Detects targeting valuable ores in a region the server never sent, which is knowledge the client could not legitimately have.',
      },
      {
        group: 'Prevention',
        title: 'A configurable enforcement pipeline supporting set back position, teleport',
        copy: 'synchronisation, attack cancellation, block placement cancellation, block break cancellation and interaction cancellation.',
      },
      {
        group: 'Prevention',
        title: 'Every preventive action is gated on accumulated confidence and can be disabled globally.',
        copy: 'Disabling prevention never suppresses flagging or evidence recording.',
      },
      {
        group: 'Prevention',
        title: 'New `prevention.enabled` and `prevention.min-confidence` settings.',
        copy: '',
      },
      {
        group: null,
        title: 'The world cache now carries block material names, not only physical classification, so',
        copy: 'ore identification is real rather than inferred.',
      },
      {
        group: null,
        title: 'Reach prefers resolved hitbox distance and falls back to the cursor only when the target',
        copy: 'is unknown, and records which basis was used in the evidence.',
      },
      {
        group: null,
        title: 'X-Ray, block ESP, ore search, entity ESP and storage ESP require nothing extra from the',
        copy: 'server. They are render time predicates over data a vanilla client already receives, so no protocol level detection is possible. Snuff AC reduces the information volunteered and detects the consequences, and `credits.md` records this rather than implying coverage.',
      },
      {
        group: null,
        title: 'Fullbright and other purely local rendering changes are not detectable and are not',
        copy: 'checked. No placeholder check was added for them.',
      },
      {
        group: null,
        title: 'ViaVersion and Geyser are not yet modelled.',
        copy: 'Bedrock clients move differently and older protocol versions have different movement semantics, so false positives are likely on either.',
      },
      {
        group: null,
        title: 'The movement predictor still does not model the post 1.8.2 skipped tick behaviour, and',
        copy: 'still does not simulate collisions, so speed related thresholds remain untuned.',
      },
      {
        group: null,
        title: '130 unit tests pass, up from 94.',
        copy: '',
      },
      {
        group: null,
        title: 'Clean build with 31 checks registered.',
        copy: '',
      },
      {
        group: null,
        title: 'Zero code comments and zero em dash characters across the project.',
        copy: '',
      },
    ],
  },
  {
    version: '1.0.2-dev',
    tag: 'v1.0.2-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 12,
    items: [
      {
        group: null,
        title: '138 unit tests pass, up from 130.',
        copy: '',
      },
      {
        group: null,
        title: 'Clean build with 31 checks registered.',
        copy: '',
      },
      {
        group: null,
        title: 'Alerts never reached staff in game.',
        copy: 'The alert path ran on the check thread, where the online player lookup returns nothing, so the console worked while chat silently produced no recipients. Delivery now runs on the main thread, and each recipient is isolated so one failure cannot abort the rest. The Velocity messenger had empty broadcast and console bodies, so alerts reached nobody on Velocity at all.',
      },
      {
        group: null,
        title: 'Flag history was discarded the moment a player quit, which made reconnecting the',
        copy: 'cheapest way to wipe a record. History is now persisted per UUID with retention, a per player cap and an async writer, and staff are notified when a player returns carrying flags.',
      },
      {
        group: null,
        title: 'The enforcement pipeline was unreachable.',
        copy: 'Nothing called it, so no confidence ever accumulated and no setback or cancel ever ran. It is now routed from the violation path and gated on confidence.',
      },
      {
        group: null,
        title: 'The auto punishment path was deleted rather than guarded.',
        copy: 'The command execution interface and its call site were removed along with the dead state they used. All 31 checks still ship with alert only.',
      },
      {
        group: null,
        title: 'MiningBeyondView could never fire.',
        copy: 'The world cache is a three block box, so the material at any real dig target was always null. Dig positions are now probed on the main thread, and the chunk distance maths compares relative chunk coordinates instead of the difference of two radii.',
      },
      {
        group: null,
        title: 'Reach read creative mode as a hardcoded false and never evaluated line of sight.',
        copy: 'The combat environment now carries the game mode, and the check samples the segment from eye to target.',
      },
      {
        group: null,
        title: 'Three false positive sources were removed.',
        copy: 'PitchLock required only pitch near ninety for six ticks, which flags anyone glancing down; it now requires bit constant pitch while placing, mining, gliding or airborne. Critical counted ordinary airborne movement and now requires genuine extra packets in the same tick. Drift read an offset another check happened to leave behind and now reads a centrally tracked delta.',
      },
      {
        group: null,
        title: 'A staff menu with a flagged players list, opened with `/snuff`.',
        copy: 'Commands are unchanged and still work from console. Inventory events are cancelled and permissions are rechecked at click time.',
      },
      {
        group: null,
        title: 'A staff permission for the menu, a per staff verbose alert toggle, and an on and off',
        copy: 'form for the alerts command.',
      },
      {
        group: null,
        title: 'Eight regression tests, and a fix so the artifact name derives from the project',
        copy: 'version instead of drifting behind it.',
      },
    ],
  },
  {
    version: '1.0.3-dev',
    tag: 'v1.0.3-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 15,
    items: [
      {
        group: null,
        title: 'Menus could be looted',
        copy: 'Inventory events were handled at HIGH priority with `ignoreCancelled = true`. If any other plugin cancelled a click first, Snuff skipped its own handler and the button items could be picked up, which is an item duplication vector. Handling now runs at HIGHEST priority and never skips. Number key swaps, offhand swaps, middle clicks and double click collect are additionally short circuited, and drag events are cancelled regardless of prior state.',
      },
      {
        group: null,
        title: 'Nuker was not detected',
        copy: 'The check counted *distinct* block positions per second. A nuker that repeatedly dug the same position, or burst many digs inside a short window, never tripped it. Detection is now on dig packet rate: three dig packets inside 700ms, or the distinct position signal as a second condition.',
      },
      {
        group: null,
        title: 'Wind charge flagged legitimate players',
        copy: 'A wind charge gives a large horizontal impulse and being hit by one gives knockback. Neither was modelled, so Fly, Speed, AirMovement, HighJump, LongJump, Drift, Step, NoFall, Velocity and PitchLock all flagged normal play. A wind charge now grants three seconds of grace after the player uses one and one second after the player is hit by one, applied centrally so every movement check inherits it. Detection is driven from real item use, projectile launch and damage events.',
      },
      {
        group: 'Manual punishments',
        title: '`ban`, `timeout`, `tempban`, `ipban`, `tempipban`, `mute`, `tempmute` and',
        copy: '`warn`, each with a reverse: `unban`, `untimeout`, `untempban`, `unipban`, `untempipban`, `unmute`, `untempmute` and `unwarn`.',
      },
      {
        group: 'Manual punishments',
        title: 'Duration syntax accepts `m`, `h` and `d` in any order and any combination, so',
        copy: '`/snuff tempban frteddz 1h 10s cheating` works. Zero, negative, unitless and absurd durations are rejected.',
      },
      {
        group: 'Manual punishments',
        title: 'A reason is mandatory on every punish command.',
        copy: '',
      },
      {
        group: 'Manual punishments',
        title: '`/snuff punishments [player]` lists active punishments, online or offline.',
        copy: '',
      },
      {
        group: 'Manual punishments',
        title: '`/snuff warns <player>` lists warnings.',
        copy: '',
      },
      {
        group: 'Manual punishments',
        title: 'Bans are enforced at pre-login, mutes block chat and commands, and everything',
        copy: 'persists per UUID across restarts.',
      },
      {
        group: 'Manual punishments',
        title: 'Staff caps via `snuffac.punish.maxduration.*`, and `snuffac.exempt.punish`',
        copy: 'protects staff from being punished.',
      },
      {
        group: 'Manual punishments',
        title: 'Snuff still never punishes on its own.',
        copy: 'Every one of these is a staff action and no check or report can reach it.',
      },
      {
        group: null,
        title: 'Settings menu',
        copy: '`/snuff settings` opens an admin menu for log retention, history retention, the prevention switch and the alert cooldown, with in place reload.',
      },
      {
        group: 'Simplified command surface',
        title: '`/snuff` with no arguments now opens the menu instead of printing a command',
        copy: 'list. `/snuff violations [player]` opens the flagged players menu or that player\'s history rather than printing text.',
      },
      {
        group: 'Simplified command surface',
        title: 'Flagged players and warned players are shown with their real skin when they',
        copy: 'are online.',
      },
      {
        group: null,
        title: '157 unit tests pass, up from 138.',
        copy: '',
      },
    ],
  },
  {
    version: '1.0.4-dev',
    tag: 'v1.0.4-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 8,
    items: [
      {
        group: null,
        title: 'A join grace of five seconds now applies to every movement check.',
        copy: 'The join timestamp was recorded but never read, so a player\'s first five seconds of movement, which is exactly when the world cache is still filling and the predictor has no history, were checked against a cold model.',
      },
      {
        group: null,
        title: 'A global lag gate now suppresses movement checks below 18 TPS and above',
        copy: '300ms ping. Timing and position checks are meaningless when the server or the connection is struggling, and this was only applied to the Timer check before. An unmeasured TPS is not treated as lag.',
      },
      {
        group: null,
        title: 'Fly now exempts the full documented list: riptiding, slow falling, levitation,',
        copy: 'recent knockback and a recent block change, in addition to the water, elytra, vehicle and climbable cases it already handled. Each is now driven by real potion, item and damage events.',
      },
      {
        group: null,
        title: 'ExtraPackets was flagging at three position packets in a tick.',
        copy: 'A vanilla client sends one, but 1.8 clients, Bedrock and any client under packet aggregation legitimately send two to three, and four is still within normal variance. The threshold is now four per tick and it must be sustained across six consecutive ticks before flagging, which is what the study describes as the signal rather than a single busy tick.',
      },
      {
        group: null,
        title: 'Post attack timing.',
        copy: 'An attack sent immediately after a movement packet, which is what a targetting assist does and a human does not, is now recorded and compared against the normal 2 to 60ms gap.',
      },
      {
        group: null,
        title: 'Multi target analysis.',
        copy: 'Attacking three or more distinct entities inside one second, each immediately after a movement packet, is flagged. Rotation is now also compared against the angle to the target, and an attack on an entity more than 90 degrees from the facing direction is flagged as hitting outside the normal view.',
      },
      {
        group: null,
        title: 'Dig reach validation.',
        copy: 'Starting a dig on a block more than six blocks away, in a chunk the server has actually loaded, is flagged. This is the ghost dig class of abuse, where the client targets a position the server never told it about.',
      },
      {
        group: null,
        title: '165 unit tests pass, up from 157, with dedicated coverage for the join grace,',
        copy: 'the lag gate and both wind charge windows.',
      },
    ],
  },
  {
    version: '1.0.5-dev',
    tag: 'v1.0.5-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 8,
    items: [
      {
        group: null,
        title: 'Menu back buttons were dead.',
        copy: 'They opened the parent inventory directly instead of going through the menu open path, which left the stale child menu registered as the player\'s open menu. Every click after going back resolved against the wrong inventory, so the whole tab set appeared inert. Navigation now re-registers correctly, which fixes the flagged players list, the warnings list and the settings menu at the same time.',
      },
      {
        group: null,
        title: 'A warning ladder.',
        copy: 'Each confident flag records one warning against the player. When the warning count reaches the configured limit, one timed ban is applied. The ban screen states which check it was for, how many warnings led to it, and that a false detection can be appealed with the admins once it expires.',
      },
      {
        group: null,
        title: 'The ladder is keyed by UUID, so disconnecting does not clear it, and it has',
        copy: 'a cooldown so one burst of packets cannot consume the entire warning allowance. A manual staff punishment or unban resets it, so staff always have the final say.',
      },
      {
        group: null,
        title: 'It only acts on confident detections, and a warning limit of one is the',
        copy: 'floor, so a misconfigured server cannot ban on the first flag.',
      },
      {
        group: null,
        title: 'Configurable under `escalation`, including a warn only mode that stops at',
        copy: 'the limit and never bans, and a settings menu control to toggle the ladder and change the warning limit without editing the file.',
      },
      {
        group: null,
        title: 'Escalation is off by default.',
        copy: 'It should only be enabled after thresholds are tuned on the target server, since no automatic ban can be risk free.',
      },
      {
        group: null,
        title: 'plugin.yml carried a hardcoded version instead of the build template, so',
        copy: 'every build since v1.0.2 reported 1.0.2-dev at runtime regardless of the real version. The template is restored and the version is verified by reading it back out of the built artifact.',
      },
      {
        group: null,
        title: '176 unit tests pass, up from 165, including coverage that the first flag',
        copy: 'never bans, that the ban lands exactly on the configured limit, that a reconnect cannot clear the ladder, that a burst of packets cannot burn warnings, and that the ban reason names the check and the appeal path.',
      },
    ],
  },
  {
    version: '1.0.6-dev',
    tag: 'v1.0.6-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 11,
    items: [
      {
        group: null,
        title: 'Menus no longer hand out items.',
        copy: 'Every button in every tab was inert and the button items could be taken into the player\'s own inventory, which is an item duplication vector with real items. The cause was a lifecycle race: opening a menu registered it, then Bukkit fired the close event for the previous screen, and that handler deleted the registration of the new one. The registry now stores the menu together with its inventory, and a close event only clears the entry when the menu and the inventory both match what is actually closing. Cancellation no longer depends on that lookup succeeding, so a future failure degrades to a cancelled click rather than a free item.',
      },
      {
        group: null,
        title: 'The warning ladder can be enabled again.',
        copy: 'The settings toggle set the value and then immediately re-read the config file, which ships the ladder disabled, and overwrote it. Settings now persist to the file, and the same method was adding the violation listener on every click, which leaked a listener per click.',
      },
      {
        group: null,
        title: 'Log and history retention take a typed value in chat instead of a stepper.',
        copy: 'Stepping from 1 to 365 one click at a time was not usable. Typing cancel keeps the current value. The prompt times out so an ignored prompt cannot lock someone out of the menu, validates the number, sanitises the input, allows one pending prompt per player, re-checks permission when it completes, and always answers with success or failure. The alert cooldown keeps its stepper.',
      },
      {
        group: null,
        title: 'Every placeholder in the punishment path now substitutes.',
        copy: 'Call sites passed the angle brackets as part of the key while the renderer added its own, so the lookup was for a doubled string that appears nowhere. This affected the usage, player, duration and input placeholders, and it would have become more visible once the tags rendered, because a real player name would have been replaced by the literal word player.',
      },
      {
        group: null,
        title: 'Messages render properly and every Snuff line carries the supplied gradient',
        copy: 'prefix. There were two message senders and only one understood the markup language, which is why the admin commands looked right and the punishment commands did not. There is now one shared sender used by the command path, the punishment path, the menu path, the mute and warn notices and the rejoin notice. The legacy hex form is accepted as input, so text generated by the RGBirdflop tool works verbatim, with no runtime dependency on that service.',
      },
      {
        group: null,
        title: 'Punishments print a readable sequential id instead of the first eight',
        copy: 'characters of a UUID, which was an unreadable hex fragment such as 000001a0.',
      },
      {
        group: null,
        title: 'The ban screen now names the appeal route explicitly.',
        copy: '',
      },
      {
        group: null,
        title: 'The rejoin notice reads as a line rather than a sentence with a bracketed',
        copy: 'prefix glued to it, and announces the count once.',
      },
      {
        group: null,
        title: 'Attribute swapping to a spear is now detected.',
        copy: 'The weapon fields existed and were written every tick, but no check read them, so the engine learned the held item every tick and never looked at it. The server side attack attribute is now observed and compared against what the held weapon should give, and reach consumes it. This keys off the attribute value rather than the item, so a legitimate spear user is never flagged simply for holding one.',
      },
      {
        group: null,
        title: '193 unit tests pass, up from 176.',
        copy: 'New coverage includes the exact prefix string supplied, hex and decoration conversion, malformed input, and the menu registry lifecycle, including the case that caused the duplication bug.',
      },
      {
        group: null,
        title: 'Two of the new registry tests failed on the first run and caught a real',
        copy: 'mistake in the fix, where the stored menu was being compared against the closing inventory so the entry could never clear. The registry now stores both and compares like with like.',
      },
    ],
  },
  {
    version: '1.0.7-dev',
    tag: 'v1.0.7-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 14,
    items: [
      {
        group: null,
        title: '**Ping was never measured.** The engine had a working ping recorder that also feeds the',
        copy: 'min, max and smoothed values the tolerance model uses, and nothing on Paper ever called it. Every alert reported 0ms. The user, who actually has 50 to 60ms, was told 0ms. Paper now reads the player ping every tick and feeds the model.',
      },
      {
        group: null,
        title: 'This is not a display bug.',
        copy: 'The tolerance model is documented as widening its allowance as latency rises so that nobody is punished for their ping, and that entire term had been inert since the project began. Every tolerance decision has been made as if every player had zero latency. It is a plausible contributing cause of the false flagging reported throughout testing.',
      },
      {
        group: null,
        title: 'Ping now reports as unknown rather than a fake 0 when it has not been measured, so a',
        copy: 'missing measurement can never again be read as a real zero.',
      },
      {
        group: null,
        title: '**The gradient prefix is now rendered.** The legacy converter was applied to the message',
        copy: 'body but never to the prefix, so the prefix reached players as literal `&` and `#` characters. The prefix and the join now live in `LegacyColour`, which has no dependencies and is directly testable.',
      },
      {
        group: null,
        title: 'The warnings listing was still building its own plain `[Snuff]` string instead of using',
        copy: 'the shared sender, so that one path would have stayed wrong even after the prefix fix. Both paths are routed now, and the audit is for every literal prefix, not one.',
      },
      {
        group: null,
        title: 'Flag history no longer prints a raw epoch integer.',
        copy: 'It shows a real date and time plus a relative value, which is what was originally asked for back in v1.0.2.',
      },
      {
        group: null,
        title: 'Tab completion covers the punishment commands and every subcommand added since the',
        copy: 'completer was written. The previous list was hardcoded and contained none of them, so `/snuff mute` was not special, every new subcommand was missing. Player names, offline known names, and duration suggestions are all offered now.',
      },
      {
        group: null,
        title: 'The retention prompt accepts the same duration syntax the punishment commands teach, so',
        copy: '`5d` works instead of being rejected as not a number. It no longer consumes the first chat message it sees, so unrelated chatter passes through to chat and leaves the prompt standing. A message that is not a value attempt is not cancelled at all.',
      },
      {
        group: null,
        title: 'Command feedback rewritten to be calmer.',
        copy: 'Errors are red and short, hints are grey, usage lines no longer shout.',
      },
      {
        group: null,
        title: '**Mute command allowlist.** Muted players previously could not run a single command.',
        copy: 'A new `mute.allowed-commands` list lets an owner grant specific commands. The default is an empty list, so an owner who configures nothing keeps today\'s strict behaviour. Names are matched case insensitively, with any leading slash stripped and plugin namespaces supported, because a client sends the bare name and a slash would silently never match.',
      },
      {
        group: null,
        title: '**Sound effects** on menus and command outcomes, limited to meaningful moments rather',
        copy: 'than every button: opening a menu, a successful action, a rejected action, a punishment applied. Individual staff can silence their own with `/snuff sounds off` without changing it for everyone, and a new `snuffac.sounds` permission covers it.',
      },
      {
        group: null,
        title: 'The flag history line now reads as a sentence, with the check, the violation level, the',
        copy: 'ping and the time, rather than a run of values with a raw timestamp.',
      },
      {
        group: null,
        title: '201 unit tests pass, up from 193.',
        copy: '',
      },
      {
        group: null,
        title: 'New coverage is deliberately aimed at the integration rather than the mechanism, since',
        copy: 'that is how the previous three releases each slipped through: the prefix is now asserted through the join that was actually broken, and the body is checked for surviving legacy codes, the presence of all seven gradient stops, and safety on a null or malformed body.',
      },
    ],
  },
  {
    version: '1.0.8-dev',
    tag: 'v1.0.8-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 17,
    items: [
      {
        group: null,
        title: 'Detection was muted by the shipped thresholds',
        copy: 'Every one of the 31 checks shipped with a `buffer-threshold` of 20 to 30 while checks only add 5 to 10 buffer per flag, a `buffer-decay` of 0.5 to 0.6 applied every tick, and an `alert-threshold` of 4 to 5 on top. Reaching a single alert needed roughly 3 to 6 consecutive flags to cross the buffer and then 4 to 5 more crossings to reach the alert threshold, with decay eating the buffer between bursts. In practice tens of consecutive cheat actions were needed, and burst cheating never alerted at all. Hard checks now ship with a buffer threshold of 1.0, a decay of 0.1 and an alert threshold of 1.0, so one clear detection reports. Statistical checks use an alert threshold of 2.0. The hardcoded fallback in `CheckConfig.defaults` carried the same muted values and is now strict as well, so a check with no shipped config still reports.`setback-threshold` was 0.0 on all 31 checks, which made `setbacksEnabled()` false everywhere, so no check could prevent anything even after flagging. Prevention thresholds are now 1.0 for hard checks.',
      },
      {
        group: null,
        title: 'Anti-xray never ran on modern Paper',
        copy: 'Obfuscation was applied through the removed `com.destroystokyo.paper` config classes, so on Paper 1.21 the server logged a `ClassNotFoundException` at every startup and shipped raw ore data to every client. X-Ray and Block ESP had full data to work with. It now resolves the current Paper `AntiXrayConfiguration` through `getUnsafe` and applies.`OBFUSCATE` with proper `BlockData` block lists. When no world can be configured the plugin now says so plainly, naming X-Ray and storage ESP as still unblocked, rather than logging a reflection stack trace.',
      },
      {
        group: null,
        title: 'The permission tree was invalid',
        copy: '`plugin.yml` had a stray `snuffac.sounds` key with no value inside the `snuffac.admin` children block, plus a malformed `snuffac.bypass` and a duplicated `snuffac.menu`. Paper rejected the whole `snuffac.admin` node on every load, so the intended inheritance never applied. The tree now parses cleanly with eighteen nodes and twelve valid children.',
      },
      {
        group: 'Prevention that actually prevents',
        title: 'A prevention signal per player that checks request through `preventAttack`,',
        copy: '`preventPlacement`, `preventInteraction` and `requestSetback`. The packet gate runs at `LOWEST` priority so a cancelled attack never reaches the server.',
      },
      {
        group: 'Prevention that actually prevents',
        title: 'Attack packets are now dispatched synchronously on arrival rather than queued, because a',
        copy: 'decision made a tick later cannot cancel the packet that has already landed.',
      },
      {
        group: 'Prevention that actually prevents',
        title: '`EntityDamageByEntityEvent` cancellation as a second line of defence, so an illegal hit',
        copy: 'is stopped even if the packet was already in flight.',
      },
      {
        group: 'Prevention that actually prevents',
        title: 'Speed, Fly and HighJump now request a setback to the last legal position instead of only',
        copy: 'reporting.',
      },
      {
        group: 'Hitbox verification',
        title: '`HitboxVerifier` casts the attacker\'s actual look vector against the true vanilla',
        copy: 'hitbox, 0.6 by 1.8 with a 1.5 sneaking height, using a slab method against the box rather than a distance check.',
      },
      {
        group: 'Hitbox verification',
        title: '`AttackAngleCheck` rejects hits that landed only on an expanded hitbox, tracking a streak',
        copy: 'so a single odd frame is not punished, and cancels the attack. This catches the Hitboxes cheat that Reach alone cannot see.',
      },
      {
        group: 'Hitbox verification',
        title: 'Reach now cancels the attack instead of only flagging, and reports the ray result,',
        copy: 'angle, and reject reason as evidence.',
      },
      {
        group: 'Aim analysis',
        title: '`GcdAnalysis` learns the player\'s own mouse constant from a rolling window of pitch and',
        copy: 'yaw deltas, then flags rotation deltas that are not a multiple of it. Human mouse input always lands on the grid; synthetic aim usually does not.',
      },
      {
        group: 'Aim analysis',
        title: 'KillAura now detects rapid target switching between entities far apart in angle, and',
        copy: 'cancels the attack.',
      },
      {
        group: null,
        title: 'The prefix now closes with a reset, so the gradient colour and the bold and italic',
        copy: 'decorations stop at the bracket instead of bleeding into the message text.',
      },
      {
        group: null,
        title: 'Added `tuning.profile` and a `visual` section to `config.yml`.',
        copy: '',
      },
      {
        group: null,
        title: 'Rendering cheats cannot be detected, only prevented, because the client decides what to',
        copy: 'draw. Anti-xray needs the server to obfuscate, entity hiding needs a line of sight pass, and both depend on `anti-xray.mode` being on.',
      },
      {
        group: null,
        title: 'Folia is still unsupported.',
        copy: '',
      },
      {
        group: null,
        title: 'The warning ladder is still the only automatic action and is still off by default.',
        copy: '',
      },
    ],
  },
  {
    version: '1.0.9-dev',
    tag: 'v1.0.9-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 13,
    items: [
      {
        group: null,
        title: 'packetspam flagged every player, including a stationary one',
        copy: 'The rate was computed as `count * 1000 / (elapsed + 1)`. On the first packet of a window that reports 1000 packets per second, on the second 666, on the third 500. The window also started at an arbitrary packet rather than a clock boundary, so the bad arithmetic repeated forever. Against a 400 per second threshold, a player standing still was flagged every two seconds. The live log proved it: `packetsPerSecond=1000.0, threshold=400.0, count=1`. A window is now only evaluated once it has been open for at least 250ms, so the division always means something. The rate is a real count over a real interval, the window closes on the tick rather than on whichever packet arrived, a per-type breakdown is recorded as evidence, and the rate must hold across three consecutive windows before it alerts. This was broken for every player on every version since it was written.',
      },
      {
        group: null,
        title: 'badpackets reset the player\'s position while they bridged',
        copy: 'The check flagged any block interaction whose position did not match an "active dig".`digActive` stays true for the whole mining duration, so placing a block or breaking the next one while mining tripped it. Nobody had to be cheating, mining and building does it. The rule has been removed entirely. Dig behaviour belongs to FastBreak and Nuker, and a check called badpackets should only reject packets that cannot be decoded. The client attack cursor is no longer validated as if it were authoritative. Reach and AttackAngle already validate against the server-resolved hitbox. Only non-finite coordinates are still flagged immediately, because a NaN genuinely cannot come from a vanilla client. Out-of-bounds positions are buffered. The world border now matches the vanilla maximum of 29999984 rather than 30000000.',
      },
      {
        group: null,
        title: 'Prevention could fire on a single flag from a check that guesses',
        copy: 'v1.0.8 set `setback-threshold: 1.0` on 23 checks, so the first flag from a behavioural check teleported the player. For BadPackets that turned a chat message into a rubber band the player felt every two seconds while bridging. Every check now declares an `evidence` kind. `STRUCTURAL` checks are things that cannot legitimately happen and may act at their own threshold. `DERIVED` checks, which is everything that measures a rate, a ratio, an average or a window, are structurally unable to request a setback until one violation level past their alert threshold, regardless of what the config says. BadPackets, ImpossibleMovement, ImpossibleAttack, InvalidAttackState and GroundSpoof are structural. The other 27 are derived.',
      },
      {
        group: null,
        title: 'Alerts did not use the Snuff prefix',
        copy: 'Alerts were built by a completely separate formatter that took a plain `Snuff` from.`general.alert-prefix` and wrapped it in hardcoded square brackets. `LegacyColour` was never involved, so the gradient never appeared. The brackets were in the format string, not in the prefix, which is why adding the real prefix would have double-bracketed it. Alerts now carry the same gradient prefix as `/snuff`, configured as.`general.chat-prefix`, and converted through `LegacyColour` before MiniMessage sees it. The console line is rendered separately and is plain text, because a terminal cannot show a gradient. Hex codes are resolved rather than left as literal text.',
      },
      {
        group: null,
        title: 'Anti-X-Ray was still guessed at',
        copy: 'The v1.0.8 reflection chain through `world.getUnsafe().getWorldConfiguration()` does not exist. `org.bukkit.World` has no `getUnsafe`, `UnsafeValues` has no world-config access, and paper-api ships no anti-xray classes at all.`AntiXrayBridge` now tries four documented strategies in order and reports every one it attempted. The engine mode resolves against whatever enum constants the running server actually has. Setters are matched by signature and a missing one is reported by name rather than thrown. The diagnostic now logs the exception class, its message and the first four stack frames, instead of the literal text `null`.',
      },
      {
        group: 'Player reports',
        title: '`/snuff report <player>` opens a seven category picker: cheating, exploiting, explicit',
        copy: 'language, offensive behaviour, griefing, inappropriate name, and staff impersonation.',
      },
      {
        group: 'Player reports',
        title: '`/snuff reports` opens the admin view, listing reports newest first with claim and',
        copy: 'resolve actions so two admins cannot both act on the same report and none is silently dropped.',
      },
      {
        group: 'Player reports',
        title: 'A cheating report attaches the target\'s flag count and most recent check from Snuff\'s own',
        copy: 'history, so the admin sees the evidence before the reporter\'s note.',
      },
      {
        group: 'Player reports',
        title: 'Reporter notes are stripped of markup and length limited, self reports are refused, staff',
        copy: 'holding `snuffac.exempt.punish` cannot be reported, and a reporter is rate limited to five reports per ten minutes. Reports persist to `plugins/SnuffAC/reports.tsv` with configurable retention, defaulting to 30 days.',
      },
      {
        group: null,
        title: '308 passing unit tests, up from 258',
        copy: '',
      },
      {
        group: null,
        title: '32 checks, unchanged',
        copy: '',
      },
      {
        group: null,
        title: 'Nobody has clicked through the menus by hand.',
        copy: 'This is the fourth consecutive release that says so. The menus are now data driven, which is different code from the version that was believed fixed three times.',
      },
      {
        group: null,
        title: 'Anti-X-Ray and entity concealment still need a live check against a real cheat client',
        copy: 'before either can be claimed to work. v1.0.8 claimed both and delivered neither.',
      },
    ],
  },
  {
    version: '1.1.0-dev',
    tag: 'v1.1.0-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 9,
    items: [
      {
        group: null,
        title: 'A tempban did not stop a banned player rejoining',
        copy: 'There was no login listener at all. `isBanned` was called in exactly one place, when lifting a ban, so a player who reconnected was never checked. `applyOnline` only fires for a player who is already online, which is why the kick worked and the reconnect did not. Enforcement was structurally incapable of stopping a returning player. An `AsyncPlayerPreLoginEvent` listener now disallows the connection on a live BAN or TEMPBAN, reading the same store the kick path uses, so the two cannot disagree. Reproduced twice by the user, banning an alt and then banning their own main account. Both rejoined while the ban was still running.',
      },
      {
        group: null,
        title: '`packetrate` flagged legitimate play and set the player back',
        copy: 'The check compared a client\'s movement packet rate against a flat 20, which is the server tick rate. Those are not the same quantity. A vanilla client does not send a position packet every tick, so a player standing still sends well under the 0.80 lower bound, and three four second windows of not moving is about twelve seconds of standing still. A window is now discarded unless the player actually moved during it, the bands are wider, five consecutive windows are required instead of three, and a teleport or a recent low TPS resets the measurement rather than being averaged through. This is the second rate based check to fail this way after `packetspam` in v1.0.9. Both were measuring a client quantity against a fixed constant rather than against what the server actually did.',
      },
      {
        group: null,
        title: 'Spear attribute swapping was never detected',
        copy: '`observedReach` read `Attribute.ATTACK_DAMAGE`, which is damage, not reach. The reach attribute on 1.21.11 is `ENTITY_INTERACTION_RANGE`. The code was asking how much damage a player did, storing it in a field called `observedAttackReach`, and comparing that damage number against a distance, so it would flag a weak weapon and miss a reach cheat. The expected reach was also never set. `attackReach` initialised to 3.0 and nothing ever wrote it, so even with the right attribute it compared every weapon against a hardcoded sword reach. Now reads `ENTITY_INTERACTION_RANGE`, derives the expected reach from the held weapon, compares in both directions rather than only looking for a value that is too low, and logs a debug line for a weapon whose reach is not observable instead of guessing. The v1.0.6 release notes claimed this was fixed. It was not, and it never had been.',
      },
      {
        group: null,
        title: '`/snuff reports` threw, and `/snuff report` opened a menu with dead buttons',
        copy: '`render()` called `Bukkit.getPlayer(target == null ? null : target.getUniqueId())`. The admin view is constructed with a null target on purpose, so the guard produced null and handed it to a method that rejects null. `IllegalArgumentException: UUID id cannot be null`, every time. In the picker, the same line set `renderPlayer` to the report target rather than to the person looking at the menu. `renderPlayer` is what the per button permission check consults, so every category button was checked against the wrong player, failed, and was never placed in the inventory. A menu that renders with no buttons looks like a working menu with unresponsive items.`openAdminReports` already called `forViewer(player)` before `build()`. `build()` then called `render()`, which overwrote the correct value. The `forViewer` call was dead.`renderPlayer` is now written in exactly one place.',
      },
      {
        group: null,
        title: '`/snuff menu` was advertised and did nothing',
        copy: '`menu` was in the tab completion list and in the documentation, and had no `case` anywhere, so it produced "unknown subcommand" three keystrokes after the server offered it. The bare `/snuff` worked, which is why it survived. Added the case, and a structural test asserting every advertised subcommand maps to a permission, so the three lists cannot silently disagree again.',
      },
      {
        group: null,
        title: 'Player facing commands were unreachable',
        copy: '`plugin.yml` declared the command with `permission: snuffac.admin`, and Bukkit enforces that before the executor runs. So `/snuff report` and `/snuff version` did not work for a normal player at all, even though `snuffac.report` defaults to true. The two disagreed: a player was told they could report, opened a menu where nothing was clickable, and had no command to fall back on. The command-level permission is gone and each subcommand is gated individually. The permission tree is now three tiers and declares every node the code checks, so LuckPerms and other managers can actually grant them. A node that is not declared cannot be granted, and the failure is silent. Added `snuffac.teleport`, `snuffac.bypass.give`, `snuffac.reports.manage`,.`snuffac.escalation.manage` and the four `snuffac.clear.*` nodes, none of which inherit from the punishment permissions.',
      },
      {
        group: null,
        title: 'Punishment screens with real detail',
        copy: 'Every ban, temporary ban, mute and kick screen now names the staff member who issued it, the exact expiry as a date and a time, the remaining duration, the reason, and how to appeal. A one minute ban tells the player when they may rejoin instead of telling them to run a command they cannot run while banned. Staff names and reasons are stripped of markup, since a reason is free text written by a person.',
      },
      {
        group: null,
        title: 'Bypass, clear commands, and staff location',
        copy: '`/snuff bypass <player> [on|off]` grants or revokes the anticheat bypass, persists to.`bypass.tsv`, and is now a real input to the exempt computation rather than a value that would be overwritten on the next refresh. Every grant and revoke is announced to staff.`/snuff clearflags`, `/snuff clearwarns` and `/snuff clearpunishments`, each with its own permission, each logging what was destroyed and by whom. Destructive, so they require confirmation, and clearing flags also resets the live session state so a cleared player is not still in violation a second later.`ViolationInfo` now carries world and position, so a flag records where it happened.`FlagsMenu` shows last seen world and coordinates per player, and `/snuff tp` teleports to a player or to their last known position, refusing if the chunk is not loaded. This is a breaking change to the public API, which is why it happens on a dev release.',
      },
      {
        group: null,
        title: 'Reference plugin',
        copy: '`DonutSus-1.0.jar` was inspected for metadata only. It has no licence file and declares no licence in its bundled pom, so nothing was decompiled and no code was taken from it. Its dependency list is worth one note: it reads flags from Vulcan and Grim through their public APIs. Snuff should never do that. Two anticheats deciding about the same player without knowing what the other decided is a bad outcome for a server, and Snuff generating its own evidence and owning it is the correct architecture.',
      },
    ],
  },
  {
    version: '1.1.1-dev',
    tag: 'v1.1.1-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 8,
    items: [
      {
        group: null,
        title: 'Reports were unusable in every released version',
        copy: 'The report flow had never once been completed end to end. Filing a report, listing it, claiming it, and resolving it were all separate code paths and none of them had been exercised together. The picker\'s submit button was declared at slot 49 inside a.5 row menu, which only has 45 slots, so the button was silently relocated and the report could not be filed. The picker is now 6 rows, and a test asserts that every declared slot is inside its own inventory.`/snuff reports` opened an empty view because the admin command dispatched to a handler that took no player. Resolved, claim, and reject now exist and were clicked through with two real clients.',
      },
      {
        group: null,
        title: 'Anti-Xray reported itself as active on a live server when it was not',
        copy: 'The bridge called `apply` before worlds existed, caught every failure, and logged a success line anyway. It now defers until the world is loaded and logs the result it actually got. Verified on all three dimensions: `engineMode=OBFUSCATE hidden=10/10 replacement=3`.',
      },
      {
        group: null,
        title: 'HighJump flagged every normal jump',
        copy: 'Root cause was a units mistake. The server attribute `JUMP_STRENGTH` already returns.0.42, and the code multiplied that by its own hardcoded 0.42, so any jump above.0.17 was treated as a violation. Normal vanilla first launch is 0.33, so every jump tripped it. The attribute is now normalised once and the threshold is a multiplier.',
      },
      {
        group: null,
        title: 'Timer flagged a player standing still',
        copy: 'The check measured packet rate over an unbounded window. A stationary player sends.0 packets, which looked identical to a player whose packets were being dropped. It now only measures while the player is actually moving, and has a floor for the window it inspects.',
      },
      {
        group: null,
        title: 'Violations lost their location and history never cleared',
        copy: 'The history file was written with 11 fields but read expecting 15, so world and coordinates were dropped on every restart and `/snuff tp` aimed at 0,0,0. The length guards were also off by one, so z was never read at all.`clearflags` deleted a file named after the player, but the file was written with the dashes removed from the UUID, so the file was never actually deleted. Staff saw."cleared", and every flag came back on the next join.`total` and `history` only read the in memory cache, so anything asked about an offline player reported zero. Both now read from disk on a cold cache. Records written by older builds still load.',
      },
      {
        group: null,
        title: 'A bypass grant was not actually persisted',
        copy: '`setBypass` returned null for anyone who was not online, which made a grant for an offline staff action impossible, and the name was written lower cased so it came back as `tester2` in staff messages.',
      },
      {
        group: null,
        title: 'Console could not use the staff commands',
        copy: 'The new commands all required a `Player`, so the console, which is exactly where server owners run them, was rejected. The console now works and still has to type.`confirm` for anything destructive.',
      },
      {
        group: null,
        title: 'Filler colour could not be configured',
        copy: 'The `filler` key was written twice per file, first as a material name and then as a boolean, so the material was always lost and every menu fell back to black. There is now a separate `filler-material` key.',
      },
    ],
  },
  {
    version: '1.1.2-dev',
    tag: 'v1.1.2-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 4,
    items: [
      {
        group: null,
        title: 'Three menus ignored their own config file',
        copy: 'Flags, Warned and Settings hardcoded every button while still writing a YAML file beside them. Editing `settings-gui.yml` changed nothing, which is worse than having no file, because the file looks like it works. All five menus now render from their layout. The Settings defaults described a different menu than the one that exists, listing a tuning profile button that was never rendered. The defaults now describe the real menu, and live values are substituted for placeholders such as `{prevention}` and `{max-warnings}` so a customised label still shows the current state.',
      },
      {
        group: null,
        title: 'Report categories could not be added',
        copy: 'The list was a switch statement with one case per category, so a category added to a file would render a button and then do nothing. Categories now come from `report-options.yml`, and any category listed there works.`/snuff reload` re-reads the GUI files and the report options, and warns about any button whose slot is outside its inventory.',
      },
      {
        group: null,
        title: 'Every player shared one report note',
        copy: 'The draft note was a static field, so two players drafting reports at the same time overwrote each other.',
      },
      {
        group: null,
        title: 'A menu could fail to open entirely',
        copy: 'The item registry lookup can throw while the server is still starting. It was not guarded, so a single bad material could take the whole menu with it.',
      },
    ],
  },
  {
    version: '1.1.3-dev',
    tag: 'v1.1.3-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 5,
    items: [
      {
        group: null,
        title: 'A report category added to the file never worked',
        copy: 'The allowlist that sanitises every button action held only the seven built in categories, so a category an owner added to `report-options.yml` was sanitised to an empty string. The button never entered the menu at all, so the previous release\'s headline feature did not do the thing it was written for. The id is still constrained to a plain identifier and the five control actions are excluded, so a category cannot shadow Back or Submit. Confirmed by adding an AFK category at runtime, reloading, and filing a report with it, then restarting and filing another.',
      },
      {
        group: null,
        title: 'Two menus had a Back button that closed the inventory',
        copy: 'Settings and Warned were opened without a parent, so Back had nowhere to go and closed the window instead. Settings had no Back button to configure at all, which is why nothing was missed.',
      },
      {
        group: null,
        title: 'Returning to a parent menu showed an empty window',
        copy: 'The parent menu was rendered only once, when it was first created, and never rebuilt. It is now built before it is shown.',
      },
      {
        group: null,
        title: 'The Flags config described one button while the menu renders four',
        copy: 'Previous, Refresh and Next were hardcoded and not configurable, and the file offered only Back. The layout now describes all four, and Warned describes only the two it actually has.',
      },
      {
        group: null,
        title: '/snuff reload ignored the GUI files',
        copy: 'It re-read `config.yml` and `checks.yml` and nothing else. Editing a GUI file or the report options and reloading changed nothing, which is the entire reason those files exist.',
      },
    ],
  },
  {
    version: '1.1.4-dev',
    tag: 'v1.1.4-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 1,
    items: [
      {
        group: null,
        title: 'Documentation claimed a feature that does not exist',
        copy: 'The README said X-Ray, block ESP, ore search, entity ESP and storage ESP are all prevented by withholding data. Ores are obfuscated, entities are hidden without line of sight, and sounds are fuzzed. Container contents are sent to the client exactly as vanilla sends them, and nothing in the plugin changes that. Suppressing them means rewriting block entity payloads on the wire, which is a much larger job than the ore rewrite and has not been done. The `visual` section of `config.yml` said the same thing in a comment, which is the one place a server owner looks to find out what a switch does. The README now has a "Not implemented" section for it rather than leaving the claim folded into a paragraph about what does work.',
      },
    ],
  },
  {
    version: '1.1.5-dev',
    tag: 'v1.1.5-dev',
    stamp: '2026-09-30',
    pre: true,
    count: 4,
    items: [
      {
        group: null,
        title: 'A report can carry a description of what happened',
        copy: 'The picker takes a note in chat, 240 characters, with a 60 second timeout and a cancel word, and the button shows the current text so it can be replaced. Filed and claimed with a real client, and the text is stored as typed. Submitting with no note offers the prompt rather than filing a bare category, since a report with only a category gives staff nothing to act on. Typing `cancel` files it without one.',
      },
      {
        group: null,
        title: 'Chat input was being stripped before it was stored',
        copy: 'The note prompt reused the same sanitiser as the numeric retention prompt, which keeps letters and digits only, so `they were duplicating obsidian` was stored as `theywereduplicatingobsidiani`. Notes are prose, so they keep spacing, case and punctuation, and only control characters are replaced.',
      },
      {
        group: null,
        title: 'A report could not be filed against someone who had just left',
        copy: 'The note prompt held a live `Player` and refused to file if they went offline, and the prompt itself ran on the chat thread, which is not the thread a report store may be touched from. The prompt now holds the player id, and the write is moved onto the server thread.',
      },
      {
        group: null,
        title: 'The category chosen before the note was lost',
        copy: 'Submitting asked for the note, then built a fresh picker to file with, so the report came back as "pick a category first".',
      },
    ],
  },
  {
    version: '1.1.6-dev',
    tag: 'v1.1.6-dev',
    stamp: '2026-10-01',
    pre: true,
    count: 4,
    items: [
      {
        group: null,
        title: 'A block can no longer be used from across the map',
        copy: 'Reach measured the eye to the hitbox distance, and only for attacks. Opening a chest, a door, a button or a workbench sent the same kind of packet and nothing looked at it, so interaction reach was unlimited while the check said it was capped. Distance is now measured to the nearest face of the block rather than its centre, the same way it is measured for a hitbox. Capped at the vanilla survival range of 4.5 blocks and 5 in creative, with the same ping tolerance attacks already had, so a laggy player is not punished for the round trip. Two out of range interactions in a row are needed before anything is reported, and the interaction is blocked rather than only alerted, so the cheat gains nothing.',
      },
      {
        group: null,
        title: 'Flight now runs out of air',
        copy: 'The air time limit was written down, checked, and then thrown away: the code returned early once the limit was passed, which is the one case the limit was there to catch. A player could stay off the ground indefinitely and the check would never speak. Air time is now a budget that only legitimate support refills. Touching ground, or a ladder, water, honey, soul sand, a vehicle, an elytra, slow falling, levitation, riptiding, knockback or a wind charge puts it back in full. Hovering on nothing spends it. A brief brush past a vine does not refill it, so clipping support in a loop no longer lasts forever. The report carries the air time, the unsupported tick count and the height the player was at, so a report says why the budget ran out.',
      },
      {
        group: null,
        title: 'Nuker counted retries on one block as many blocks',
        copy: 'The check counts distinct blocks per second, and says so in its own report, but the burst rule counted raw dig packets instead. Clicking a block you cannot break three times sent three packets, which read as a burst across three blocks. A player using the wrong tool got a Nuker flag for it. The burst now counts distinct blocks, and the report reads "started digging.5 distinct blocks within 700ms" instead of claiming distinct blocks while counting packets. Found with a real client hammering one block: 48 refused dig attempts, no flag.',
      },
      {
        group: null,
        title: 'The Nuker window was pruning block positions as if they were timestamps',
        copy: 'The sliding window stored packed block positions and then compared each one against the clock to decide what had fallen out of the last second. A packed position is a large number, so `now - entry` was never small, nothing ever aged out, and the per second limit was really a limit on all time. The window now stores the time of each dig alongside the position, so the per second limit means per second.',
      },
    ],
  },
  {
    version: '1.1.7-dev',
    tag: 'v1.1.7-dev',
    stamp: '2026-10-01',
    pre: true,
    count: 4,
    items: [
      {
        group: null,
        title: 'Aura no longer ignores what is behind you',
        copy: 'The field of view rule was in the code but dead. `postAttack` measured the angle to the target and flagged anything past 90 degrees, which cannot happen: the vanilla server rejects an attack that far off facing before the packet reaches a check, so the branch never ran on a real server. The limit is now 110 degrees, which is as far as a player can look while still having a target on screen, and three consecutive attacks past it are needed. The attack is blocked, so an aura that targets behind the player gains nothing.',
      },
      {
        group: null,
        title: 'Rotation that no mouse could produce',
        copy: 'Aims that a human cannot make are now caught by shape rather than size. The rotation between attacks is sampled, and flagged when the steps form a straight line with no jitter: a real hand does not produce evenly spaced rotation, and a cheat that interpolates between two points produces exactly that. A turn in the middle of the sample is not flagged, so deliberate sweeps and target changes stay clean. A spread of under 0.35 degrees across the sample is required before the rule applies, so a player who barely moves the mouse is never caught by a pattern that has no pattern in it. The resolved mouse grid is carried in the evidence, so the sensitivity is visible rather than guessed.',
      },
      {
        group: null,
        title: 'Step height is now a number, not a vibe',
        copy: 'HighJump only looked at the launch velocity of a jump. A cheat that steps straight up a block without jumping at all never produced a launch, so it was never seen. Vertical gain past 0.6 blocks in a single tick is now flagged after two consecutive ticks, with the gravity and terminal velocity figures in the evidence.',
      },
      {
        group: null,
        title: 'The jump ceiling was not actually the vanilla ceiling',
        copy: 'The limit was 0.42 multiplied by the jump strength attribute, but a real jump peaks at 0.42 reduced by the 0.98 vertical drag, so a genuine jump sat almost exactly on the boundary. The drag is now applied to the whole sum, so the ceiling is 0.4116 for a standing jump and the boost and sprint bonuses are added in the same place. The three figures are asserted directly, since a check that flags a real jump is worse than no check.',
      },
    ],
  },
  {
    version: '1.1.8-dev',
    tag: 'v1.1.8-dev',
    stamp: '2026-10-01',
    pre: true,
    count: 4,
    items: [
      {
        group: null,
        title: 'A new check for moving through solid blocks',
        copy: 'There was no check for Phase, NoClip, VClip or HClip, which is the family of cheats that send a position on the far side of a wall. A new `phase` check sweeps the straight line between the last position and the new one against the server block view and rejects the move if a solid block is in the way. The player is set back, so the cheat gains nothing. The sweep samples every 0.2 blocks, and every 0.05 on a vertical move, because a thin wall in the middle of a long move must not be stepped over. A block the player already overlaps is not treated as a wall, so being pushed into geometry is not reported, and a jump that clears the one block step height stays clean. The same check also catches a single tick move further than 8 blocks, which is more than any input can produce and is what a teleport or clip resolves to. The evidence names the block that was passed through and its material, plus both endpoints, so a report shows the path rather than just the verdict. Water, a vehicle, a ladder, recent knockback and a recent block change all exempt the tick.',
      },
      {
        group: null,
        title: 'A speed cheat that is only slightly fast is now caught',
        copy: 'Speed flagged a residual past the tolerance, two ticks running. A client running 5 percent fast never trips a per tick threshold, however long it keeps doing it, which is the point of that cheat. A small overshoot between 0.004 and 0.06 blocks per tick now fills an accumulator, and twelve fills over at least fifty ticks is flagged. The accumulator decays on clean ticks, so ordinary jitter never accumulates, and anything above the creep ceiling resets it, so a large single flag is still handled by the original rule. A setback is requested, since a client that is continuously a little fast is still going too fast.',
      },
      {
        group: null,
        title: 'Jumping was reported as flight',
        copy: 'The rising rule counted any tick where the vertical delta was above zero. A real client jumping produces float noise of a few thousandths on some ticks while the server velocity is plainly downward, three of those in a row and the rule fired. A real jump was reported as flying, and the violation level climbed to 2.0 on a legitimate client. Rising now requires a genuine upward move of at least 0.06 blocks and an upward server velocity, so rounding noise while falling cannot reach it. Found by running the jump scenario against a real client: 5 flags in 20 seconds, then none.',
      },
      {
        group: null,
        title: 'The check count was hard coded in two places',
        copy: 'The description checker asserted exactly 32 checks, and the description had to contain the literal text "32 checks". Adding a check meant editing a Python constant, a Markdown heading, a count in the opening paragraph and a count in the prevention sentence, and forgetting any of them failed the build. The count is now a single named constant in the checker.',
      },
    ],
  },
]
