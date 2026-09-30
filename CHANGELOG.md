# Changelog

All notable changes to Snuff AC are documented here.

The format is based on Keep a Changelog, and this project adheres to Semantic
Versioning.

## [1.2.3-dev] - 2026-10-01

NoFall and Velocity. Both were asking the client a question the server could
answer itself.

### Added

**Fall distance is now tracked on the server**

- The check asked whether the client claimed ground contact while the server
  still saw air, which only catches a no-fall module that lies about
  onGround. The source asks for the real fall distance to be tracked server
  side, which catches the module that simply never reports a fall at all.
- The downward distance is accumulated from the position packets while there
  is nothing underfoot, and a landing past 3 blocks of it, twice, is reported.
  A block under the feet clears the total, so a descent that ends on solid
  ground is not carried into the next jump. Water, a ladder, gliding, flying
  and slow falling clear it too, since none of them is a fall.
- The report carries both distances, the server one and the client one, so a
  disagreement between them is visible rather than implied.

**The causes that absorb knockback are checked rather than assumed**

- A player who takes a hit inside a cobweb, in water, or on a ladder loses
  most of the knockback, and the client is right to move less. The check had
  a list of exemptions, and these three were not on it, so a player hit into
  a cobweb was reported for discarding knockback they never received.
- The block above the head, the block below the feet, the feet block itself
  and the climbable state are now read. Any of them is treated as absorbing
  the impulse, and a short grace is granted so the geometry can change
  between the hit and the response. Flat ground absorbs nothing, so the
  original detection is unchanged where it should be.

### Verified

- 446 tests pass, 9 of them new.
- Loaded on a real Paper 1.21.11 server, 36 checks, and a real client
  falling, jumping, walking and taking hits is clean.

## [1.2.2-dev] - 2026-10-01

Timer and Blink. The timer only caught a client running at nearly double
speed, and Blink had no check at all.

### Added

**A new check for held back movement packets**

- There was no check for Blink or LagSwitch, which hold movement packets back
  and then send them all at once so the player appears to teleport and can
  dodge hits. A new `blink` check measures the gap between position packets
  and counts what follows.
- A silence of 350 milliseconds or more, which is more than six vanilla
  ticks, opens a release window. Five or more packets arriving inside 250
  milliseconds of each other after that silence is a release, and two
  releases in a row are reported. A client sending on every tick, and a
  single late packet after lag, are both clean.
- The report separates the silence length from the release length, so it is
  visible whether the player was hidden or merely bursty.

**A timer cheat that is only a little fast is now caught**

- The timer check flagged a rate above 1.6 packets per tick or below 0.35. A
  client running 10 percent fast, which is the whole point of a balance or
  balance mode, never reached either number however long it ran.
- A rate above one packet per tick now accumulates credit, scaled by how far
  past the allowed ratio it is, and four windows running is reported. A rate
  at or below the allowed ratio clears the credit, so ordinary jitter cannot
  build it up, and the existing hard thresholds are untouched, so a blatant
  timer is still reported by the rule that always caught it.

### Fixed

**Blink counted the wrong packets at first**

- The first version measured how many packets were queued when a gap
  appeared, which is the number that arrived before the silence rather than
  the number released after it. Queued packets from before a gap are exactly
  the ones a real client already sent, so nothing was ever counted. The check
  now counts the packets that arrive after the silence, which is the part a
  cheat produces.

### Verified

- 437 tests pass, 6 of them new.
- Loaded on a real Paper 1.21.11 server, 36 checks, and a real client
  walking, jumping and sprinting is clean.

## [1.2.1-dev] - 2026-10-01

Two new checks. Jesus and Spider had no coverage at all, and both are cheap
to detect from data the server already has.

### Added

**A new check for walking on water**

- There was no check for Jesus or WaterWalk. A new `jesus` check looks at
  where the player is standing: inside a liquid, with nothing beneath them
  that they could stand on. Air, water, lava and barrier are not support; a
  solid block, or ice, is.
- The player has to actually move, so standing still in a river is not a
  report, and any upward movement resets the counter, because that is a jump
  out of the water rather than a walk across it. Six ticks of horizontal
  travel with no sinking is reported and the player is set back.
- The evidence reports how deep the liquid is, so shallow water a player is
  wading through can be told apart from a lake.

**A new check for climbing walls**

- A new `spider` check reports sustained upward movement while the player is
  inside a solid block column. Rising 0.12 or more per tick for four ticks
  with nothing climbable is reported and set back.
- Ladders, water, honey, soul sand, gliding, flying, a vehicle, levitation
  and slow falling all exempt the tick, because every one of them raises a
  player legitimately. Rising in open space is a jump, not a wall climb.

### Fixed

**Support detection treated any non liquid block as ground**

- The first version of the water walk check accepted any block that was not
  water or lava, which included air. A player standing on the surface of a
  deep lake was reported as walking on it, because the block below their feet
  was air. Support now requires a block the player cannot sink through, which
  is what the rule was always meant to mean.

### Verified

- 431 tests pass, 9 of them new.
- Loaded on a real Paper 1.21.11 server, 35 checks, anti-xray active, and a
  real client walking, jumping, sprinting and digging is clean.

## [1.2.0-dev] - 2026-10-01

Scaffold and Nuker. The first release of the 1.2 line, and the first two
checks that were measuring a rate but never checking the cheat's actual
signature.

### Added

**Scaffold no longer assumes the player is aiming at what they place**

- The tower pattern was detected, but only if the player was sprinting,
  airborne and not crouching. Every other way a scaffold cheat works was
  invisible: a player standing still, walking backwards, or crouching while
  auto placing was never seen at all.
- Placement is now validated against the look direction. The target is the
  centre of the face being clicked, and both the yaw and the pitch error
  against the player's actual facing are measured. More than 62 degrees off,
  three placements running, is reported and the placement is blocked. A real
  player places within a few degrees of what they are looking at, and 62
  degrees leaves room for lag and for reaching to the side.
- Placing with nothing in hand is now reported after three attempts. The
  server knows the held item, and no placement is possible without one, so
  this is a case that cannot happen legitimately. The evidence records the
  held slot so it is visible why.

**Nuker now checks whether the target was ever visible**

- The check counted dig packets and, since the last release, distinct blocks
  per burst and per second. Neither asks the obvious question: how did the
  player know to dig that block. A client that starts digging a block the
  server has solid geometry between it and the player is using information
  the server never sent.
- The eye to block distance is measured and the segment sampled against the
  block view. Two consecutive dig starts at a block more than 6 blocks away
  and behind solid blocks are reported. A block the server has no data for is
  not judged, because sight cannot be established without data, and a block
  in front of the player is never flagged.

### Verified

- 422 tests pass, 9 of them new.
- Loaded on a real Paper 1.21.11 server, 33 checks, and a real client
  walking, jumping, sprinting and digging is clean.

## [1.1.9-dev] - 2026-10-01

Criticals and AutoClicker. The critical check only looked at one way of
forcing a critical, and the click check only looked at the rate.

### Added

**A critical landed on the ground is now reported**

- The check looked for extra movement packets before an attack and nothing
  else. A cheat that makes the client believe it is falling, without sending
  a packet, produced no extra packets, so it was never seen. The server
  already knows whether a player is really falling: negative vertical
  velocity, not in water, not on a ladder, not in a vehicle, not gliding, no
  levitation, no slow falling.
- A critical landed on two consecutive attacks while the server knows the
  player is not falling is now flagged, and the evidence states which of those
  conditions held. A real jump critical is unaffected, because a real jump is
  genuinely falling.

**Click cadence is judged by the shape of the intervals**

- The check measured the rate and the standard deviation of the intervals,
  but variance was compared against zero, so the perfect timing branch could
  not be reached from a normal sample. Two shape rules are added.
- A spread under 0.06 milliseconds across 14 samples is a perfectly even
  cadence. A hand does not produce that, and a machine does. The spread is
  also compared against the mean interval, so an evenly spaced burst on a slow
  cadence is not mistaken for a fast one.
- The same delay six times running, while the surrounding spread stays under
  1.5 milliseconds, is a repeated delay. That is the signature of a fixed
  delay cheat and of a drag pattern, which the source notes warn about, so
  the wide spread requirement keeps ordinary human clicking out of it.

### Verified

- 413 tests pass, 7 of them new for the cadence shapes.
- Loaded on a real Paper 1.21.11 server, 33 checks, and a real client moving,
  jumping and attacking is clean.

## [1.1.8-dev] - 2026-10-01

Speed and Phase. Speed only ever caught a client that was obviously too
fast, and there was no check at all for moving through walls.

### Added

**A new check for moving through solid blocks**

- There was no check for Phase, NoClip, VClip or HClip, which is the family
  of cheats that send a position on the far side of a wall. A new `phase`
  check sweeps the straight line between the last position and the new one
  against the server block view and rejects the move if a solid block is in
  the way. The player is set back, so the cheat gains nothing.
- The sweep samples every 0.2 blocks, and every 0.05 on a vertical move,
  because a thin wall in the middle of a long move must not be stepped over.
  A block the player already overlaps is not treated as a wall, so being
  pushed into geometry is not reported, and a jump that clears the one block
  step height stays clean.
- The same check also catches a single tick move further than 8 blocks, which
  is more than any input can produce and is what a teleport or clip resolves
  to.
- The evidence names the block that was passed through and its material, plus
  both endpoints, so a report shows the path rather than just the verdict.
  Water, a vehicle, a ladder, recent knockback and a recent block
  change all exempt the tick.

**A speed cheat that is only slightly fast is now caught**

- Speed flagged a residual past the tolerance, two ticks running. A client
  running 5 percent fast never trips a per tick threshold, however long it
  keeps doing it, which is the point of that cheat.
- A small overshoot between 0.004 and 0.06 blocks per tick now fills an
  accumulator, and twelve fills over at least fifty ticks is flagged. The
  accumulator decays on clean ticks, so ordinary jitter never accumulates,
  and anything above the creep ceiling resets it, so a large single flag is
  still handled by the original rule. A setback is requested, since a client
  that is continuously a little fast is still going too fast.

### Fixed

**Jumping was reported as flight**

- The rising rule counted any tick where the vertical delta was above zero.
  A real client jumping produces float noise of a few thousandths on some
  ticks while the server velocity is plainly downward, three of those in a
  row and the rule fired. A real jump was reported as flying, and the
  violation level climbed to 2.0 on a legitimate client.
- Rising now requires a genuine upward move of at least 0.06 blocks and an
  upward server velocity, so rounding noise while falling cannot reach it.
  Found by running the jump scenario against a real client: 5 flags in 20
  seconds, then none.

**The check count was hard coded in two places**

- The description checker asserted exactly 32 checks, and the description had
  to contain the literal text "32 checks". Adding a check meant editing a
  Python constant, a Markdown heading, a count in the opening paragraph and a
  count in the prevention sentence, and forgetting any of them failed the
  build. The count is now a single named constant in the checker.

### Verified

- 406 tests pass, 13 of them new.
- Loaded on a real Paper 1.21.11 server, 33 checks, anti-xray active, and a
  real client walking, jumping and sprinting is clean.

## [1.1.7-dev] - 2026-10-01

KillAura and HighJump. Both checks had a rule for the case they were named
after and nothing for the cases around it.

### Added

**Aura no longer ignores what is behind you**

- The field of view rule was in the code but dead. `postAttack` measured the
  angle to the target and flagged anything past 90 degrees, which cannot
  happen: the vanilla server rejects an attack that far off facing before the
  packet reaches a check, so the branch never ran on a real server.
- The limit is now 110 degrees, which is as far as a player can look while
  still having a target on screen, and three consecutive attacks past it are
  needed. The attack is blocked, so an aura that targets behind the player
  gains nothing.

**Rotation that no mouse could produce**

- Aims that a human cannot make are now caught by shape rather than size. The
  rotation between attacks is sampled, and flagged when the steps form a
  straight line with no jitter: a real hand does not produce evenly spaced
  rotation, and a cheat that interpolates between two points produces exactly
  that. A turn in the middle of the sample is not flagged, so deliberate
  sweeps and target changes stay clean.
- A spread of under 0.35 degrees across the sample is required before the
  rule applies, so a player who barely moves the mouse is never caught by a
  pattern that has no pattern in it. The resolved mouse grid is carried in the
  evidence, so the sensitivity is visible rather than guessed.

**Step height is now a number, not a vibe**

- HighJump only looked at the launch velocity of a jump. A cheat that steps
  straight up a block without jumping at all never produced a launch, so it
  was never seen. Vertical gain past 0.6 blocks in a single tick is now
  flagged after two consecutive ticks, with the gravity and terminal
  velocity figures in the evidence.

### Fixed

**The jump ceiling was not actually the vanilla ceiling**

- The limit was 0.42 multiplied by the jump strength attribute, but a real
  jump peaks at 0.42 reduced by the 0.98 vertical drag, so a genuine jump
  sat almost exactly on the boundary. The drag is now applied to the whole
  sum, so the ceiling is 0.4116 for a standing jump and the boost and sprint
  bonuses are added in the same place. The three figures are asserted
  directly, since a check that flags a real jump is worse than no check.

### Verified

- 393 tests pass, 12 of them new.
- Loaded on a real Paper 1.21.11 server, 32 checks, no errors, and a real
  client moving, jumping and attacking is clean.

## [1.1.6-dev] - 2026-10-01

Reach and Flight. Two things the checks already claimed to do, plus a false
positive a real client turned up while this was being worked on.

### Added

**A block can no longer be used from across the map**

- Reach measured the eye to the hitbox distance, and only for attacks. Opening
  a chest, a door, a button or a workbench sent the same kind of packet and
  nothing looked at it, so interaction reach was unlimited while the check
  said it was capped. Distance is now measured to the nearest face of the
  block rather than its centre, the same way it is measured for a hitbox.
- Capped at the vanilla survival range of 4.5 blocks and 5 in creative, with
  the same ping tolerance attacks already had, so a laggy player is not
  punished for the round trip. Two out of range interactions in a row are
  needed before anything is reported, and the interaction is blocked rather
  than only alerted, so the cheat gains nothing.

**Flight now runs out of air**

- The air time limit was written down, checked, and then thrown away: the code
  returned early once the limit was passed, which is the one case the limit
  was there to catch. A player could stay off the ground indefinitely and the
  check would never speak.
- Air time is now a budget that only legitimate support refills. Touching
  ground, or a ladder, water, honey, soul sand, a vehicle, an elytra, slow
  falling, levitation, riptiding, knockback or a wind charge puts it back in
  full. Hovering on nothing spends it. A brief brush past a vine does not
  refill it, so clipping support in a loop no longer lasts forever.
- The report carries the air time, the unsupported tick count and the height
  the player was at, so a report says why the budget ran out.

### Fixed

**Nuker counted retries on one block as many blocks**

- The check counts distinct blocks per second, and says so in its own report,
  but the burst rule counted raw dig packets instead. Clicking a block you
  cannot break three times sent three packets, which read as a burst across
  three blocks. A player using the wrong tool got a Nuker flag for it.
- The burst now counts distinct blocks, and the report reads "started digging
  5 distinct blocks within 700ms" instead of claiming distinct blocks while
  counting packets. Found with a real client hammering one block: 48 refused
  dig attempts, no flag.

**The Nuker window was pruning block positions as if they were timestamps**

- The sliding window stored packed block positions and then compared each one
  against the clock to decide what had fallen out of the last second. A packed
  position is a large number, so `now - entry` was never small, nothing ever
  aged out, and the per second limit was really a limit on all time. The
  window now stores the time of each dig alongside the position, so the
  per second limit means per second.

### Verified

- 381 tests pass, 16 of them new: 13 for interaction reach and the air
  budget, 3 for the dig window.
- Loaded on a real Paper 1.21.11 server, 32 checks, anti-xray active on all
  three dimensions, no errors.
- A real client standing still, falling from the build limit, climbing, and
  retrying a dig it cannot complete are all clean.

## [1.1.5-dev] - 2026-09-30

The note field. Every stored report has carried a note since reports were
added, and there has never been a way to fill it in.

### Added

**A report can carry a description of what happened**

- The picker takes a note in chat, 240 characters, with a 60 second timeout
  and a cancel word, and the button shows the current text so it can be
  replaced. Filed and claimed with a real client, and the text is stored as
  typed.
- Submitting with no note offers the prompt rather than filing a bare
  category, since a report with only a category gives staff nothing to act
  on. Typing `cancel` files it without one.

### Fixed

**Chat input was being stripped before it was stored**

- The note prompt reused the same sanitiser as the numeric retention prompt,
  which keeps letters and digits only, so `they were duplicating obsidian`
  was stored as `theywereduplicatingobsidiani`. Notes are prose, so they keep
  spacing, case and punctuation, and only control characters are replaced.

**A report could not be filed against someone who had just left**

- The note prompt held a live `Player` and refused to file if they went
  offline, and the prompt itself ran on the chat thread, which is not the
  thread a report store may be touched from. The prompt now holds the player
  id, and the write is moved onto the server thread.

**The category chosen before the note was lost**

- Submitting asked for the note, then built a fresh picker to file with, so
  the report came back as "pick a category first".

### Verified

- 365 tests pass, 8 of them for the note prompt.

## [1.1.4-dev] - 2026-09-30

Documentation release. Storage ESP is listed as working in the README, the
config comments, and the marketplace description. It does not work, and it
never has.

### Fixed

**Documentation claimed a feature that does not exist**

- The README said X-Ray, block ESP, ore search, entity ESP and storage ESP are
  all prevented by withholding data. Ores are obfuscated, entities are hidden
  without line of sight, and sounds are fuzzed. Container contents are sent to
  the client exactly as vanilla sends them, and nothing in the plugin changes
  that. Suppressing them means rewriting block entity payloads on the wire,
  which is a much larger job than the ore rewrite and has not been done.
- The `visual` section of `config.yml` said the same thing in a comment, which
  is the one place a server owner looks to find out what a switch does.
- The README now has a "Not implemented" section for it rather than leaving
  the claim folded into a paragraph about what does work.

### Verified

- All 8 movement scenarios run clean against a real client, including jump and
  sprint jump, which the harness had silently stopped exercising because
  mineflayer has no `bot.jump` and the scenario threw every tick. A green
  suite that skips two of its own cases is worse than a red one.
- 357 tests pass.

## [1.1.3-dev] - 2026-09-30

The release where the previous release's feature was actually tested. Every
menu was opened and every button clicked with a real client, which found five
things that reading the code did not.

### Fixed

**A report category added to the file never worked** (entry 018)

- The allowlist that sanitises every button action held only the seven built
  in categories, so a category an owner added to `report-options.yml` was
  sanitised to an empty string. The button never entered the menu at all, so
  the previous release's headline feature did not do the thing it was written
  for. The id is still constrained to a plain identifier and the five control
  actions are excluded, so a category cannot shadow Back or Submit.
- Confirmed by adding an AFK category at runtime, reloading, and filing a
  report with it, then restarting and filing another.

**Two menus had a Back button that closed the inventory**

- Settings and Warned were opened without a parent, so Back had nowhere to go
  and closed the window instead. Settings had no Back button to configure at
  all, which is why nothing was missed.

**Returning to a parent menu showed an empty window**

- The parent menu was rendered only once, when it was first created, and
  never rebuilt. It is now built before it is shown.

**The Flags config described one button while the menu renders four**

- Previous, Refresh and Next were hardcoded and not configurable, and the file
  offered only Back. The layout now describes all four, and Warned describes
  only the two it actually has.

**/snuff reload ignored the GUI files**

- It re-read `config.yml` and `checks.yml` and nothing else. Editing a GUI
  file or the report options and reloading changed nothing, which is the
  entire reason those files exist.

### Verified

- Every menu opens, every button in it responds, and returning to a parent
  renders it. 12 windows walked with real clicks.
- 357 tests pass.

## [1.1.2-dev] - 2026-09-30

Configurability release. The GUI files for three menus were written to disk
and then ignored, and report categories were a switch statement, so the
documented way to customise either one did nothing.

### Fixed

**Three menus ignored their own config file**

- Flags, Warned and Settings hardcoded every button while still writing a
  YAML file beside them. Editing `settings-gui.yml` changed nothing, which
  is worse than having no file, because the file looks like it works. All
  five menus now render from their layout.
- The Settings defaults described a different menu than the one that
  exists, listing a tuning profile button that was never rendered. The
  defaults now describe the real menu, and live values are substituted for
  placeholders such as `{prevention}` and `{max-warnings}` so a customised
  label still shows the current state.

**Report categories could not be added**

- The list was a switch statement with one case per category, so a category
  added to a file would render a button and then do nothing. Categories now
  come from `report-options.yml`, and any category listed there works.
- `/snuff reload` re-reads the GUI files and the report options, and warns
  about any button whose slot is outside its inventory.

**Every player shared one report note**

- The draft note was a static field, so two players drafting reports at the
  same time overwrote each other.

**A menu could fail to open entirely**

- The item registry lookup can throw while the server is still starting.
  It was not guarded, so a single bad material could take the whole menu
  with it.

### Changed

- The test classpath had no Paper api, so no GUI parsing could be tested at
  all. That is why a submit button could sit at slot 49 inside a 5 row menu
  for three releases without anything failing.
- The local verification harness is now `devtools/` in the repository rather
  than a scratch directory, so it survives and can be rerun.
- 355 tests pass.

## [1.1.1-dev] - 2026-09-30

Verification release. Every fix below was reproduced against a real Paper server with
real client connections, then confirmed fixed. A previous release added features and
claimed they worked; this one proves it or names what is still open.

### Fixed

**Reports were unusable in every released version** (entry 002)

- The report flow had never once been completed end to end. Filing a report, listing
  it, claiming it, and resolving it were all separate code paths and none of them had
  been exercised together. The picker's submit button was declared at slot 49 inside a
  5 row menu, which only has 45 slots, so the button was silently relocated and the
  report could not be filed. The picker is now 6 rows, and a test asserts that every
  declared slot is inside its own inventory.
- `/snuff reports` opened an empty view because the admin command dispatched to a
  handler that took no player. Resolved, claim, and reject now exist and were clicked
  through with two real clients.

**Anti-Xray reported itself as active on a live server when it was not** (entry 019)

- The bridge called `apply` before worlds existed, caught every failure, and logged a
  success line anyway. It now defers until the world is loaded and logs the result it
  actually got. Verified on all three dimensions: `engineMode=OBFUSCATE hidden=10/10
  replacement=3`.

**HighJump flagged every normal jump** (entry 001)

- Root cause was a units mistake. The server attribute `JUMP_STRENGTH` already returns
  0.42, and the code multiplied that by its own hardcoded 0.42, so any jump above
  0.17 was treated as a violation. Normal vanilla first launch is 0.33, so every jump
  tripped it. The attribute is now normalised once and the threshold is a multiplier.

**Timer flagged a player standing still** (entry 003)

- The check measured packet rate over an unbounded window. A stationary player sends
  0 packets, which looked identical to a player whose packets were being dropped. It
  now only measures while the player is actually moving, and has a floor for the
  window it inspects.

**Violations lost their location and history never cleared** (entry 017)

- The history file was written with 11 fields but read expecting 15, so world and
  coordinates were dropped on every restart and `/snuff tp` aimed at 0,0,0. The
  length guards were also off by one, so z was never read at all.
- `clearflags` deleted a file named after the player, but the file was written with
  the dashes removed from the UUID, so the file was never actually deleted. Staff saw
  "cleared", and every flag came back on the next join.
- `total` and `history` only read the in memory cache, so anything asked about an
  offline player reported zero. Both now read from disk on a cold cache.
- Records written by older builds still load.

**A bypass grant was not actually persisted** (entry 010)

- `setBypass` returned null for anyone who was not online, which made a grant for an
  offline staff action impossible, and the name was written lower cased so it came
  back as `tester2` in staff messages.

**Console could not use the staff commands** (entry 016)

- The new commands all required a `Player`, so the console, which is exactly where
  server owners run them, was rejected. The console now works and still has to type
  `confirm` for anything destructive.

**Filler colour could not be configured** (entry 007)

- The `filler` key was written twice per file, first as a material name and then as a
  boolean, so the material was always lost and every menu fell back to black. There is
  now a separate `filler-material` key.

### Verified

- All 8 movement scenarios (idle, walk, jump, sprint jump, strafe, look, crouch, jump
  stop) run against a real client with no flags. Before this release, 4 of the 8
  flagged.
- A tempban blocks the reconnect with the reason, staff, and remaining time, and the
  player is admitted normally once it expires.
- 348 tests pass.

### Still open

- Storage ESP and container suppression are not implemented. The X-Ray prevention is
  real and verified, but the storage viewer in the user guide is not, and the
  description overclaims until it is either built or removed.
- Entity concealment, tracers, and sound fuzzing have not been visually verified.
- A grant for an offline player is accepted now, but Paper's anti-xray is driven
  through private fields, so it is verified on Paper 1.21.11 build 132 only.

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
