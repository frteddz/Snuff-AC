# Snuff AC Research Credits

Snuff AC was developed independently. No source code, comments, documentation, class
names, variable names, configuration layouts, strings or branding were copied from any of
the projects listed below.

Every repository was inspected and its actual `LICENSE` file (or its absence) was read
before any research use. Where a project grants no permission to reuse its work, that
restriction is recorded and the project was not used as a code reference.

Research was limited to high-level architecture, subsystem decomposition, threading
model and detection strategy. Implementation was written from scratch against the
documented behaviour of Minecraft itself.

## Summary of licences

| Project | Licence | Usable as code reference |
| --- | --- | --- |
| GrimAC | GPL-3.0 | Architecture only, nothing copied |
| Windfall AntiCheat | MIT | Architecture only, nothing copied |
| Updated-NoCheatPlus | GPL-3.0 | Architecture only, nothing copied |
| VulcanLite | None found | No, not read |
| ThotPatrol | None found | No, not read |
| Hades | Repository unavailable | No, could not be read |
| AntiHaxerman | MIT (archived) | Architecture only, nothing copied |
| Medusa-Lite | GPL-3.0 | Architecture only, nothing copied |
| Iris | GPL-3.0 | Architecture only, nothing copied |
| Daedalus | None found | No, not read |
| Reflex | None found (disclaimer only) | No, not read |
| Hawk | GPL-3.0 | Architecture only, nothing copied |

---

## GrimAC

Repository: https://github.com/GrimAnticheat/Grim

Licence: GNU General Public License v3.0. Verified by reading the `LICENSE` file at the
repository root, which is the full GPLv3 text dated 29 June 2007.

Restrictions: GPLv3 is strong copyleft. Any derivative work incorporating GrimAC code
would have to be distributed as GPLv3. Snuff AC therefore treats GrimAC as a
documentation-level reference only. No GrimAC source was copied.

Research:
- Separating packet normalisation from check execution, and naming the two phases
  clearly, so the hot path stays small.
- Treating the per-axis "how much should we forgive" question as a first class subsystem
  driven by named real world causes, rather than a single tuned epsilon. This was the
  single most valuable idea encountered during research and Snuff AC reimplements it as
  `dev.snuffac.core.tolerance`.
- Enumerating a candidate set of possible client inputs and selecting the best fit,
  rather than assuming a single input, because the server cannot observe player input.
- Serving all movement checks from arrays built once at startup instead of per packet
  listener dispatch.
- Gating a setback until the client has acknowledged the spawn teleport, so a cheating
  client cannot avoid detection by ignoring the initial teleport.
- Separating predicted post-collision velocity from client pre-collision velocity.
- Latency and tick time compensated world changes.

Architectural lessons learned:
- A dedicated execution context for detection, with documented thread affinity per state
  object, is what makes a heavily asynchronous design survive contact with reality.
- Replicating the world the client sees removes thread safety questions entirely, at the
  cost of memory and per-version chunk parsing. Snuff AC deliberately chose a lighter
  approach for the first release and documented the trade-off.

No source code was copied from GrimAC.

## Windfall AntiCheat

Repository: https://github.com/enis1enis2/Windfall-AntiCheat

Licence: MIT. Verified by reading the `LICENSE` file, which contains the MIT text with
copyright "Enis Polat The Github Coder".

Restrictions: MIT permits reuse with attribution. Snuff AC did not reuse any of it.

Research:
- Publishing immutable per player state snapshots behind volatile references, so a
  Netty thread can publish a consistent state and a main thread reader can never observe
  a torn read. This is a good, cheap cross thread pattern.
- Declaring check metadata, including buffer decay, setback threshold, version range and
  compatibility flags, on the check type itself and failing startup when it is missing.
- Caching all platform API reads on the main thread into plain fields, so the packet
  thread never calls into the server API.
- A severity system that scales violation increments by a tier based on a player's
  accumulated total, so many mild trips escalate faster than one severe trip.
- A broad unit test suite per check, with a shared test base that mocks the plugin
  accessor. This is why Windfall has the most tests of any project researched.
- Tapering a buffer down when a check is clean, and adding proportionally to how far a
  threshold was exceeded rather than by a flat amount.

Architectural lessons learned:
- Buffer plus violation level as two separate decaying accumulators is a clean design
  that separates "confidence right now" from "accumulated history". Snuff AC adopted
  this two accumulator split.
- Deterministic tests require a mockable collaborator surface. Snuff AC keeps the
  platform behind a small interface specifically so checks can be tested with no server.

No source code was copied from Windfall AntiCheat.

## Updated-NoCheatPlus

Repository: https://github.com/Updated-NoCheatPlus/NoCheatPlus

Licence: GNU General Public License v3.0. Verified by reading `LICENSE.txt`.

Restrictions: GPLv3 copyleft. Reference only, nothing copied.

Research:
- The long lived "check plus violation points plus a player fact table" layout that
  shaped a decade of Bukkit anticheats.
- The breadth of the exemptions list: a fact table recording recent world events that
  individual checks consult to decide whether their assumptions still hold.
- Configuration driven per check enable and violation thresholds, which is still the
  right shape for server operators.

Architectural lessons learned:
- A single growing per player fact table is convenient but becomes hard to reason about
  and hard to test. Snuff AC composes small focused state objects instead.
- Event-listener style checks with a flat violation point counter are simple to write
  and hard to make reliable. This is a pattern to avoid rather than copy.

No source code was copied from Updated-NoCheatPlus.

## VulcanLite

Repository: https://github.com/freppp/VulcanLite

Licence: **None.** No `LICENSE`, `LICENSE.md`, `LICENCE`, `COPYING` or `NOTICE` file
exists at the repository root, the GitHub API reports no detected licence, and the
`README` contains no licence grant.

Restrictions: without an explicit grant, the default is exclusive copyright. The work is
not licensed for reuse. The repository root contains only `.gitignore`, `pom.xml` and
`src`.

Action taken: the source was **not** read and the project was **not** used as a
reference. It is listed here for completeness because it was part of the requested
research set, and to record that its licence offers no permission to reuse its work.

## ThotPatrol

Repository: https://github.com/freppp/ThotPatrol

Licence: **None.** No licence file exists at the repository root, the GitHub API reports
no detected licence, and the `README` contains no licence grant.

Restrictions: no explicit permission to reuse. Default exclusive copyright applies.

Action taken: the source was **not** read and the project was **not** used as a
reference.

## Hades

Repository: https://github.com/Tecnio/Hades

Licence: **Could not be determined.** The repository is no longer publicly available.
`https://api.github.com/repos/Tecnio/Hades` returns HTTP 404, and the repository does
not appear in the owner's public repository listing
(`https://api.github.com/users/Tecnio/repos`), which was checked directly.

Restrictions and action taken: because the project could not be inspected, its licence
could not be read and its source could not be studied. It played no part in Snuff AC.

Note: a search surfaced an unrelated repository also named "Hades" by a different author
(GPL-2.0). It is a different project and was **not** treated as a substitute.

## AntiHaxerman

Repository: https://github.com/Tecnio/AntiHaxerman

Licence: MIT. Verified by reading the `LICENSE` file (MIT text, copyright 2023-2024
Tecnio).

Status: the repository is archived and no longer maintained.

Restrictions: MIT permits reuse with attribution. Nothing was reused.

Research:
- A compact check catalogue covering movement, combat, packet and world categories with
  clearly named checks, useful mainly as a checklist of which categories matter.
- Command and permission structure for administering an anticheat from inside the game.

Architectural lessons learned:
- Small, focused, single purpose checks are easier to reason about and to disable safely
  than a few very broad ones. Snuff AC follows this.

No source code was copied from AntiHaxerman.

## Medusa-Lite

Repository: https://github.com/Tecnio/Medusa-Lite

Licence: GNU General Public License v3.0. Verified by reading the `LICENSE` file.

Restrictions: GPLv3 copyleft. Reference only, nothing copied.

Research:
- Automatic moderation and punishment pipelines built on top of a detection engine, and
  the importance of separating detection, alerting and punishment as distinct stages.
- Persisting violation history so repeat offenders can be identified.

Architectural lessons learned:
- Keeping punishment entirely separate from detection means the detection engine can be
  validated before any automated punishment is trusted. Snuff AC ships alerts, logging
  and setbacks first and deliberately ships no automatic bans.

No source code was copied from Medusa-Lite.

## Iris

Repository: https://github.com/funkemunky/Iris

Licence: GNU General Public License v3.0. Verified by reading the `LICENSE` file, which
is the GPLv3 text dated 29 June 2007.

Restrictions: GPLv3 copyleft. Reference only, nothing copied.

Research:
- Check registration and enable/disable handling patterns.
- The general shape of a Bukkit anticheat plugin's package layout.

Architectural lessons learned:
- Little that is not already common knowledge in this space. Its main value during
  research was confirming which legacy approaches the community has moved away from.

No source code was copied from Iris.

## Daedalus

Repository: https://github.com/funkemunky/Daedalus

Licence: **None.** No licence file exists at the repository root, the GitHub API reports
no detected licence, and there is no `README` containing a licence grant. The last push
was in 2019.

Restrictions: no explicit permission to reuse. Default exclusive copyright applies.

Action taken: the source was **not** read and the project was **not** used as a
reference.

## Reflex

Repository: https://github.com/jonahseguin/Reflex

Licence: **None.** No licence file exists at the repository root and the GitHub API
reports no detected licence. The `README` contains a "Disclaimer" section which states
the project is released "without warranty or guarantee of functioning" and invites use,
but a warranty disclaimer is **not** a licence grant and does not grant permission to
copy, modify or redistribute the code.

Restrictions: no explicit permission to reuse. Default exclusive copyright applies.

Action taken: the source was **not** read and the project was **not** used as a
reference.

## Hawk

Repository: https://github.com/HawkAnticheat/Hawk

Licence: GNU General Public License v3.0. Verified by reading the `LICENSE` file.

Restrictions: GPLv3 copyleft. Reference only, nothing copied.

Research:
- Organisation of a check set around combat and movement categories.
- Alert formatting and console output conventions that server administrators expect.

Architectural lessons learned:
- Alert text should be short, prefixed and parseable by log tooling, with the important
  numbers (check, violation level, latency) in a predictable order. Snuff AC uses a
  configurable template for exactly this reason.

No source code was copied from Hawk.

---

## Documentation sources

These are documentation and specification references, not code. Snuff AC's physics
constants and vanilla movement behaviour were taken from public documentation of how
Minecraft works, and implemented independently.

- Minecraft Wiki, movement and physics reference, including the documented walking
  (4.317 m/s), sprinting (5.612 m/s) and sneaking (1.295 m/s) speeds and the terminal
  velocity of 3.92 blocks per tick. Used to derive and then unit test Snuff AC's
  kinematics model.
  https://minecraft.wiki/w/Physics
  https://minecraft.wiki/w/Attributes
- Minecraft Wiki, entity attribute reference, used for gravity, jump strength, step
  height and movement speed attribute semantics.
- PaperMC developer documentation, for the plugin lifecycle, scheduler threading rules
  and configuration handling on Paper and Purpur.
  https://docs.papermc.io/
- PacketEvents documentation, for the packet abstraction API used in place of
  hand written version specific packet code.
  https://docs.packetevents.com/
- Velocity documentation, for proxy plugin lifecycle and threading.
  https://docs.papermc.io/velocity

## Third party libraries used by Snuff AC

### PacketEvents (bundled)

- Project: https://github.com/retrooper/packetevents
- Version used: 2.14.0
- Licence: **GNU General Public License v3.0** (verified by reading the `LICENSE` file
  in the repository)
- Used for: multi-version packet parsing, inbound and outbound packet interception, and
  channel injection on both Paper and Velocity.
- Bundled: **yes**, shaded and relocated into the Snuff AC artifact under
  `dev.snuffac.libs.packetevents`.
- Consequence: because a GPLv3 library is distributed inside the Snuff AC jar, the
  combined distributed work must itself be licensed under GPLv3. See `LICENSE` and the
  licensing decision section of `README.md`.

### JUnit 5 (build and test only)

- Project: https://junit.org/junit5/
- Version used: 5.11.4
- Licence: Eclipse Public License 2.0
- Used for: unit tests only. Not bundled into the distributed artifact, so it imposes no
  obligation on the distributed jar.

### Gradle and plugins (build only)

- Gradle 9.7, Apache License 2.0.
- Shadow plugin (`com.gradleup.shadow`) 9.6.1, Apache License 2.0. Used to build the
  shaded, relocated distribution jar. Build only, not bundled.

### Paper API and Velocity API (compile only)

- `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` and
  `com.velocitypowered:velocity-api`. Both are provided by the server or proxy at
  runtime and are never bundled, so their licences impose no obligation on the
  distributed jar.

## Statement of independence

Snuff AC is an independent implementation. Where an idea in Snuff AC resembles an idea
in a researched project, that is because the idea is either standard public knowledge
about anticheat design or a consequence of how Minecraft's physics and networking
actually behave. Every line of Snuff AC source was written for this project.

The four projects with no licence (VulcanLite, ThotPatrol, Daedalus, Reflex) and the one
that is no longer available (Hades) contributed nothing, because no permission to reuse
their work exists.

---

# Cheat client research

In the v1.0.1 development phase, Snuff AC's own source code was studied in order to
understand what cheating actually does to the wire, and what evidence that leaves on a
server. This is defensive research. No cheat client source code, comments, identifiers,
configuration, strings or implementation structure were copied into Snuff AC. Every Snuff
AC system that resulted from this research was written from scratch.

## Licences of the researched cheat clients

| Project | Repository | Licence | Read? |
| --- | --- | --- | --- |
| LiquidBounce | https://github.com/CCBlueX/LiquidBounce | GPL-3.0 | yes |
| Wurst | https://github.com/Wurst-Imperium/Wurst7 | GPL-3.0 | yes |
| Meteor Client | https://github.com/MeteorDevelopment/meteor-client | GPL-3.0 | yes |
| Lambda | https://github.com/lambda-client/lambda | GPL-3.0 | yes |
| ThunderHack Recode | https://github.com/Pan4ur/ThunderHack-Recode | GPL-3.0, archived | yes |
| BleachHack | https://github.com/BleachDev/BleachHack | GPL-3.0 | yes |
| 3arthh4ck | https://github.com/3arthqu4ke/3arthh4ck | MIT, archived | yes |
| Aoba | https://github.com/CharismaLib/Aoba | unavailable | no |
| Phobos | https://github.com/3arthqu4ke/phobos | unavailable | no |
| Aristois | https://github.com/ImpactDevelopment/Aristois | unavailable | no |
| Inertia | https://github.com/5zig/Inertia | unavailable | no |

All seven available clients are GPLv3 except 3arthh4ck which is MIT. GPLv3 is strong
copyleft, so these are treated as documentation-level references only. No code was taken.

Four repositories were requested but are no longer publicly available. Each was verified
as returning HTTP 404 from the GitHub API and confirmed to be absent from the owning
account's public repository listing, so they contributed nothing:

- **Aoba**: the `CharismaLib` account itself now returns 404.
- **Phobos**: 404. The same author has an unrelated archived project named `phobot`,
  which is a different project and was not treated as a substitute.
- **Aristois**: 404, and not present in the `ImpactDevelopment` repository listing.
- **Inertia**: 404, and not present in the `5zig` repository listing, which contains
  `The-5zig-Mod` and related projects instead.

## What was studied

Module inventories and the mechanism behind each of the following cheat categories, with
particular attention to what the change looks like **on the server**:

- Tick rate scaling and client tick suppression with catch-up bursts
- Packet buffering, replay, and the queue that swallows keep-alive responses
- Field rewriting of movement packets after construction, including the on-ground flag
- Position packet emission, suppression, duplication, and out-of-band landing packets
- Rotation quantisation to mouse granularity, rotation smoothing, and pre- plus post-attack
  rotation delivery
- Target selection policies and the ordering they impose on attack sequences
- Click interval distributions, and specifically the log-normal distribution that modern
  autoclickers converge on
- Knockback scaling of server authored vectors
- Constant pitch values pinned by placement and glide modules
- Per tick position offsets applied inside the outgoing packet
- Swing and action packet ordering
- Item transaction sequence handling
- Keep-alive suppression, which breaks the server's own latency measurement channel
- Backtracking, which makes the client act on a stale target position
- Render time block and entity predicates for x-ray, block ESP, storage ESP, radar and
  freecam

## Honest detectability assessment

This is the most important outcome of the research, and it is largely negative. Snuff AC
records it plainly rather than pretending.

### Directly observable, and therefore detected

These leave reliable server side evidence and are now covered by checks:

- Extra or duplicated position packets within one server tick. This is the single most
  universal signature, because a vanilla client emits exactly one position packet per game
  tick and every modern cheat that replays packets violates it.
- Sustained deviation of the movement packet rate from the server tick rate, which is what
  timer manipulation actually is.
- Gap and burst patterns in the packet stream, which is what blink and freeze produce.
- Claiming ground contact where the server sees no supporting block, which is air walk and
  ground flag no fall spoofing.
- Pitch pinned to an exact constant.
- A sustained constant per tick offset between the prediction and the reported position.
- Extra position packets with tiny vertical offsets immediately before an attack, which
  is forced criticals.
- A large aim rotation immediately before an attack followed by a reverse rotation
  immediately after.
- Attacks beyond the server's own reach, now measured against resolved entity hitboxes
  rather than the client supplied cursor.
- Knockback that does not match the vector the server itself transmitted.
- Movement that contradicts the predicted kinematics after accounting for friction, ground
  state, liquids, ice, slime and vehicles.
- Impossible coordinates, deltas, rotations, block positions and attack cursors.

### Partially observable

- **X-Ray, block ESP, ore search, entity ESP and storage ESP** require **nothing extra
  from the server**. They are render time predicates over block states and entity lists
  that a vanilla client already receives in the ordinary chunk and entity packets. There
  is no protocol level way to detect that a player drew a box, because rendering is
  entirely local. The only server side levers are view distance, simulation distance, and
  whether block entity data is included in chunk packets.

  Snuff AC therefore responds in two ways. First, it reduces the information the server
  volunteers: valuable ores can be replaced with a decoy state before they are written to
  the client, so an x-ray client has nothing real to reveal. Second, it detects the
  *consequence*: mining a valuable ore in a region the server never sent to the client is
  knowledge the client could not legitimately have, and that is a low false positive
  signal.
- **Freecam** produces no evidence until the player interacts. The detectable event is an
  interaction with a face that is not visible from the server side eye ray, because a
  camera cannot change where the player is.
- **Backtrack** is detectable as an attack that lands outside reach against the target's
  server side position at the attack instant.
- **Ping spoof** is detectable because apparent latency lower than the measured keep-alive
  round trip has no legitimate twin.
- **Client brand spoofing** is a signal only, never a primary mechanism, because the brand
  is trivially rewritten.

### Not meaningfully observable, and deliberately not checked

- **Fullbright** applies a local light override. There is no network evidence and no
  meaningful server side mitigation beyond a light level policy change.
- Cosmetic HUD changes, field of view changes, nametag rendering changes and similar purely
  local modifications.

Snuff AC does not ship checks for these. Inventing a check that cannot work would only
create false positives and make the real signal harder to see. This limitation is recorded
rather than papered over.

## Systems implemented as a result

- A server authoritative combat layer that resolves entities to hitboxes, computes
  legitimate reach with the correct survival, creative and vehicle limits, and tests line
  of sight between the eye and the target box.
- A configurable enforcement pipeline that can set a player back, cancel an attack, cancel
  a block placement, cancel a block break, or synchronise a position. Every preventive
  action is gated on accumulated confidence and can be disabled globally, and prevention
  never suppresses flagging or evidence recording.
- A confidence model that accumulates weighted signals across independent checks, tracks
  the peak over a bounded window, and decays, so that several weak signals can contribute
  to a decision instead of one check firing once.
- A block obfuscation service for valuable ores, with a configurable hidden vertical band
  and an optional stricter mode, plus optional container hiding.
- A mining analyser that records every dig target against the region the server actually
  sent, and reports valuable targets that fall outside it.
- Eight new checks, taking the total from 23 to 31.

## Defensive stance on client identification

Snuff AC does not blocklist client names. Clients are renamed, forked, repackaged,
partially enabled and written from scratch constantly, so a name based block is security
theatre. The engine is built entirely on behaviour that a server can observe, and client
identification, where available, is only ever an additional signal.
