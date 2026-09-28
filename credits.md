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
