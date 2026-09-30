# Minecraft Cheat Client Hacks and How to Prevent Them

A reference for server owners and anticheat developers. Each entry covers what the hack is, what it does, and how to detect or prevent it.

General principle: **never trust the client.** Validate everything server side, and use packet-level checks (for example via PacketEvents or ProtocolLib) when Bukkit events are not enough.

---

## 1. Combat Hacks

### KillAura
- **What it does:** Automatically attacks nearby entities or players, often with no need to aim at them.
- **Variants:** Single target, multi target (hits several entities per tick), switch aura, and "legit" aura that fakes smooth rotation.
- **Prevention:**
  - Check that the attacker's look direction actually points at the target hitbox (raytrace against the hitbox at the time of attack, using lag compensation).
  - Flag attacks on multiple targets within the same tick or a very short window.
  - Flag attacks on entities outside the player's field of view or behind them.
  - Analyze rotation patterns: perfectly linear snaps, constant angular speed, or no rotation jitter.
  - Use invisible bot entities (honeypots) that only a cheat would attack.

### Reach
- **What it does:** Extends attack or interaction distance beyond vanilla limits (about 3 blocks for survival melee).
- **Prevention:**
  - Compute distance from the player's eye position to the nearest point of the target's hitbox, not to the center.
  - Account for ping with position history (lag compensation) so legit high ping players are not flagged.
  - Use a violation buffer so one edge case does not cause a ban.
  - Set hard caps on block interaction distance too (5 blocks in survival).

### Hitbox / HitBoxes
- **What it does:** Expands enemy hitboxes client side so attacks land when they should miss.
- **Prevention:** Server side raytracing against vanilla-size hitboxes. Flag hits where the ray misses the real box by a consistent margin.

### Velocity / AntiKnockback
- **What it does:** Reduces or removes knockback taken from hits, explosions, or other sources.
- **Prevention:**
  - Track expected velocity after a hit and compare to the player's actual movement on the next ticks.
  - Flag players whose movement ignores the velocity packet consistently.
  - Handle edge cases such as blocks above the head, water, cobwebs, and ladders.

### Criticals
- **What it does:** Forces critical hits without actually falling, usually by sending fake tiny jumps or ground state spoofing.
- **Prevention:** Verify the player is genuinely falling (negative Y velocity, not on ground, not in water or climbing) when a critical hit lands. Cross check onGround claims with the collision box.

### AutoClicker
- **What it does:** Clicks at high or perfectly consistent rates.
- **Prevention:**
  - Count clicks per second and flag sustained rates above a human threshold.
  - Analyze click interval statistics (standard deviation, kurtosis, repeated identical delays).
  - Flag "butterfly" or "drag click" patterns only with care, since they are hard to separate from skilled players.

### TriggerBot
- **What it does:** Automatically attacks the moment the crosshair is over a target.
- **Prevention:** Look for attacks with inhumanly consistent reaction time after the crosshair enters a hitbox, and with no pre-aim movement.

### Aimbot / AimAssist
- **What it does:** Snaps or smoothly pulls the camera toward targets.
- **Prevention:**
  - Analyze yaw and pitch deltas for unnatural acceleration, perfect smoothing, or GCD (mouse sensitivity grid) violations.
  - Flag rotation that does not fit the mouse sensitivity step pattern.
  - Flag rotation changes that happen only while a target is in range.

### AutoTotem / AutoPot / AutoSoup / AutoArmor
- **What it does:** Automatically swaps a totem, drinks potions, eats soup, or equips armor at superhuman speed.
- **Prevention:**
  - Measure time between a trigger event (low health, totem pop) and the inventory action.
  - Flag inventory actions with consistent delays under human reaction time.
  - Flag item swaps that happen while the inventory GUI is not open (if applicable to the version).

### AutoCrystal / CrystalAura
- **What it does:** Automatically places and detonates end crystals near targets for maximum damage, extremely fast.
- **Prevention:**
  - Rate limit crystal place and break actions per tick.
  - Flag break actions faster than a human can react after placement.
  - Consider disabling crystal PvP or limiting damage if your mode does not need it.

### AnchorAura / BedAura
- **What it does:** Same idea as CrystalAura using respawn anchors or beds.
- **Prevention:** Same rate limits and reaction time analysis. Restrict placement distance and count per second.

### FastBow / BowAimbot
- **What it does:** FastBow releases arrows with very short draw time. BowAimbot aims arrows automatically.
- **Prevention:** Track the time between use-item start and release, and enforce minimum charge time for full power. Check arrow velocity matches draw time.

### Macro / KeyBind Swappers
- **What it does:** Executes scripted combos such as sword to axe to pearl swaps in one tick.
- **Prevention:** Flag multiple hotbar slot changes and actions within the same tick or sub 50ms windows.

---

## 2. Movement Hacks

### Fly
- **What it does:** Lets the player fly in survival. Variants include creative style, glide, jetpack, and vertical only.
- **Prevention:**
  - Simulate vanilla movement (gravity, drag, jump) and compare to reported positions.
  - Flag sustained hovering, upward movement without jumps, and Y velocity that does not match gravity.
  - Kick after a max air time with no valid support (accounting for elytra, slow falling, levitation, bubble columns, and so on).
  - Use the vanilla "flying is not allowed" kick as a baseline and enforce your own stricter version.

### Speed / BHop
- **What it does:** Increases horizontal movement speed beyond vanilla. BHop chains jumps to keep momentum.
- **Prevention:**
  - Run a full movement prediction engine (simulate vanilla physics each tick) and compare predicted and actual velocity.
  - Account for speed effects, ice, soul sand, slabs, stairs, water, and knockback.
  - Flag small but consistent overshoots over many ticks.

### Strafe
- **What it does:** Allows unnatural air control and direction changes mid air.
- **Prevention:** Predict allowed air acceleration from vanilla physics. Flag velocity direction changes that exceed what inputs could produce.

### Step / HighJump / Jesus-Step
- **What it does:** Step lets you climb blocks taller than one block instantly. HighJump raises jump height.
- **Prevention:** Enforce max step height (0.6 without jump) and max jump height with jump boost. Flag Y gain that exceeds physical limits.

### Jesus / WaterWalk
- **What it does:** Walk on water or lava.
- **Prevention:** Check that the player is not standing on liquid without a valid block (lily pad, boat, frost walker ice). Flag horizontal movement on liquid with zero sinking.

### Spider / WallClimb
- **What it does:** Climb walls like a ladder.
- **Prevention:** Flag upward movement next to a wall without a climbable block. Compare against movement prediction.

### NoFall
- **What it does:** Cancels fall damage by lying about the onGround state.
- **Prevention:**
  - Track the real fall distance server side and apply damage yourself.
  - Flag onGround true while clearly in the air (no block below in the collision box), and onGround false while on the ground.

### Phase / NoClip
- **What it does:** Move through solid blocks.
- **Prevention:** Raytrace or sweep the movement path against block collision between positions. Reject movement that passes through solid blocks and teleport the player back.

### Teleport / VClip / HClip
- **What it does:** Instantly move far distances or through walls by abusing movement packet limits.
- **Prevention:** Enforce a maximum movement per tick (and a max distance per packet). Reject and rubberband oversized moves.

### Blink / Lag Switch
- **What it does:** Holds back movement packets then sends them all at once, making the player look like they teleported and dodge hits.
- **Prevention:** Watch for gaps in packet flow followed by bursts. Track packets per second and flag long silence followed by many packets. Use transaction or keepalive pings to detect held packets.

### Timer
- **What it does:** Speeds up the client game tick, making everything faster (movement, placing, attacking).
- **Prevention:**
  - Measure the rate of movement packets vs real time. Vanilla sends about 20 per second.
  - Use a "balance" system that accumulates allowed packets over time and flags overuse.

### Sprint / OmniSprint / NoSlow
- **What it does:** Sprint in any direction. NoSlow removes the slowdown from eating, using a bow, blocking with a shield, sneaking, or soul sand.
- **Prevention:**
  - Validate sprint state against movement direction and hunger.
  - Check that speed while using an item matches the vanilla slowed value.

### Sneak / Safewalk / Scaffold-adjacent movement
- **What it does:** Safewalk prevents walking off edges without sneaking.
- **Prevention:** Flag long sequences of edge stops that match safewalk behavior, and movement that hangs at edges with no sneak state.

### Elytra Fly / ElytraBoost
- **What it does:** Infinite elytra flight, speed boost without fireworks, or flight without an elytra equipped.
- **Prevention:** Simulate elytra physics. Verify the elytra is equipped, and only allow boosts from fireworks or valid sources.

### EntityControl / BoatFly / Horse hacks
- **What it does:** Fly or move fast using boats, horses, or other rideable entities.
- **Prevention:** Validate vehicle movement packets and speeds server side. Limit vehicle speed and Y movement to vanilla values.

### Anti Hunger / AirJump / Glide
- **What it does:** Avoids hunger loss, jumps in mid air, or slows falling.
- **Prevention:** Track hunger and exhaustion server side. Validate jump events against ground state.

---

## 3. World and Building Hacks

### Scaffold / Tower
- **What it does:** Automatically places blocks under the player while moving or jumping.
- **Prevention:**
  - Check block placement against the player's look direction (they must be facing the placed block face).
  - Flag placements with no rotation toward the target, very fast placement rates, and placing while sprinting backward.
  - Check placement speed and pattern consistency.

### Nuker / FastBreak / InstaMine
- **What it does:** Breaks many blocks at once or breaks blocks faster than allowed.
- **Prevention:**
  - Compute minimum break time from tool, enchant, effects, and block hardness. Reject early break packets.
  - Limit blocks broken per tick and per second.
  - Check distance and line of sight to each broken block.

### FastPlace
- **What it does:** Removes the placement cooldown.
- **Prevention:** Rate limit block place packets per tick and per second.

### Xray
- **What it does:** Shows only ores or chosen blocks by making other blocks transparent (client side texture or render change).
- **Prevention:**
  - Use **anti-xray** (Paper has built in anti-xray with engine modes 1 to 3). Engine mode 2 replaces hidden ores with fake ones, and engine mode 3 adds more noise.
  - Only send block data the player could plausibly see.
  - Statistical detection: flag players whose mining path heavily favors ores compared to stone (ore to stone ratios, straight tunnels to ores).
  - Use a resource pack check to block known xray packs (limited value, easy to bypass).

### Freecam / Spectate-style cameras
- **What it does:** Detaches the camera from the player, purely client side. Mostly harmless alone but paired with info gathering.
- **Prevention:** Hard to detect since it is client side only. Reduce what the server sends (entity culling, hide distant entities) so there is little to learn.

### Liquid Interact / Reach placing
- **What it does:** Place or interact with blocks from invalid angles or distances.
- **Prevention:** Raytrace the interaction from the eye position and verify the clicked face is visible and in range.

### Schematica / Printer
- **What it does:** Shows build blueprints (legit) or automatically places the blocks (printer, cheating).
- **Prevention:** Rate limit and validate placement like Scaffold. Require item in hand, valid angle, and reach.

### AutoFarm / AutoMine / AutoFish
- **What it does:** Automates farming, mining, or fishing.
- **Prevention:**
  - Detect repetitive patterns with no variance in timing or movement.
  - Add periodic captchas or interaction checks (be careful about UX).
  - Flag extremely long sessions with inhuman regularity.

---

## 4. Information and Visual Hacks

### ESP / Wallhack / Tracers / Chams
- **What it does:** Shows players, entities, or chests through walls with outlines, boxes, or lines.
- **Prevention:**
  - **Entity occlusion culling:** do not send entity data for players who cannot be seen (Paper has options, and plugins like EntityCulling or ProtocolLib based hiders exist).
  - Hide other players' positions until they are within line of sight or near range.
  - Hide chest contents and tile entities from far away.

### Nametags / Health Display
- **What it does:** Shows enlarged or always visible nametags, health, or armor.
- **Prevention:** Since it is client side, limit what data is sent (hide health metadata for other players, use packet modifications to obscure exact values).

### Fullbright / NightVision
- **What it does:** Removes darkness.
- **Prevention:** Not detectable in most cases since it is a client render change. Design gameplay so darkness is not critical, or use mob spawn and other systems that do not rely on it.

### Tracers to Containers / ChestESP / StorageESP
- **What it does:** Highlights chests, shulkers, and other storage.
- **Prevention:** Do not send tile entity data for far away blocks. Consider obscuring chest types in packets.

### Coordinate and Seed Crackers
- **What it does:** Uses world seed, structure positions, or patterns to find bases and hidden things (for example via loot tables, bedrock patterns, or first person terrain).
- **Prevention:** Use a custom seed obfuscation approach (Paper has seed obfuscation settings), randomize structure placement if possible, and keep the real seed private.

### Player/Base Finders (Tracking via chunk loading, render distance exploits)
- **What it does:** Detects players or bases using chunk load data, entity tracking range, or sound and particle packets.
- **Prevention:** Limit entity tracking range, cull sounds and particles that leak positions, and avoid sending far away entity packets.

### Anti-Blind / NoRender / NoWeather / NoHurtcam
- **What it does:** Removes visual effects like blindness, fog, hurt camera shake.
- **Prevention:** Client side only. Mostly not detectable. Balance gameplay so these effects are not critical to fairness.

### Zoom
- **What it does:** Optifine-style or modded zoom.
- **Prevention:** Generally allowed and undetectable. Decide by server rules.

---

## 5. Exploits (Protocol and Server Attacks)

These are sometimes included in cheat clients as "destructive" modules.

### Crash Exploits (Book, Sign, NBT, Packet Flood)
- **What it does:** Crashes or lags the server or other players using oversized or malformed data.
- **Prevention:**
  - Keep the server software updated (Paper, Purpur, Velocity patch many of these).
  - Enable packet limiter settings (Paper has packet limits and book size limits).
  - Validate NBT size and depth, book page count and length, and sign text length.
  - Use a proxy (Velocity or BungeeCord) with rate limits and connection throttling.
  - See the AntiBot and AntiRaid sections below for the dedicated systems.

### Lag Machines / Redstone and Entity Abuse
- **What it does:** Builds or spawns things that overload the tick loop.
- **Prevention:** Set limits on entities per chunk, hopper and redstone limits, and use tools like Paper entity activation ranges.

### Packet Spoofing / Invalid Packets
- **What it does:** Sends fake or out of order packets to bypass checks or crash.
- **Prevention:** Validate packet order, state, and values. Kick on protocol violations.

### Duplication (Dupe) Glitches
- **What it does:** Duplicates items using chunk, inventory, or entity bugs.
- **Prevention:** Keep the server patched, since most dupes are bugs fixed in updates. Log large inventory changes and monitor economy for unusual item spikes. Use Paper, which patches many common dupes.

### Bed, Anchor, and Crystal Exploits Used for Griefing
- **What it does:** Large scale destruction.
- **Prevention:** Region protection plugins (WorldGuard, GriefPrevention) and limits on explosions.

### Command / Plugin Exploits (Op Bypass, Command Injection)
- **What it does:** Abuses vulnerable plugins to gain permissions or execute commands.
- **Prevention:** Keep plugins updated, avoid unknown plugins, restrict op, use a permissions plugin (LuckPerms) with least privilege, and log command usage.

### UUID / Skin / Name Spoofing (Offline Mode Abuse)
- **What it does:** Joins as another player in offline mode servers.
- **Prevention:** Use online mode. If behind a proxy, enable Velocity modern forwarding with a secret, and block direct backend access with firewall rules.

### Bot Attacks / Join Spam
- **What it does:** Floods the server with fake players.
- **Prevention:** Handled by the AntiBot module (section 10): connection throttling, join verification, attack mode, and firewall/DDoS protection.

### Denial of Service via Chunk Loading (Teleport / Elytra Chunk Loading)
- **What it does:** Forces the server to generate huge numbers of chunks.
- **Prevention:** Set a world border, limit elytra speed, pregenerate the world, and limit chunk send rate.

---

## 6. Inventory and Interaction Hacks

### InventoryMove
- **What it does:** Move, sprint, and jump while the inventory is open.
- **Prevention:** Track whether the inventory is open (via window click packets) and flag movement that continues normally while it is.

### ChestStealer / AutoLoot / AutoSteal
- **What it does:** Instantly takes all items from a chest.
- **Prevention:** Rate limit inventory click packets, and flag click speeds beyond human capability.

### Inventory Clean / AutoDrop
- **What it does:** Automatically drops or sorts items rapidly.
- **Prevention:** Same click rate limits, and check for clicks outside the visible slot range.

### FastUse / FastEat
- **What it does:** Uses items (eating, drinking) faster than normal.
- **Prevention:** Enforce vanilla use duration (32 ticks for food) before applying the effect.

### NoSlow (Item Use)
- **What it does:** Prevents the slowdown while using items (covered above in movement).
- **Prevention:** Enforce the slowed speed while an item is being used.

### AutoClickerless Interact / FastInteract / Reach Interact
- **What it does:** Interact with blocks or entities too fast or too far.
- **Prevention:** Rate limit interaction packets and validate distance and raytrace.

---

## 7. Client Modification and Identification

### Injected Clients (Ghost Clients)
- **What it does:** Injects into the game process at runtime, leaving no mod in the mods folder.
- **Prevention:** Server side detection is the only reliable method since client scans can be bypassed. Use behavior based anticheat.

### Hacked Clients and Client Brand Spoofing
- **What it does:** Clients change the brand string (vanilla, fabric, forge) to hide.
- **Prevention:** Never rely on brand alone. Treat it as a weak signal. Whitelist mods through a proper mod validation system if you need it.

### Resource Pack Abuse (Xray packs, Transparent textures)
- **What it does:** Modifies textures to see through blocks.
- **Prevention:** Force a server resource pack, and use anti-xray (texture level cheats are undetectable, so use server side anti-xray as the real fix).

### Mod Detection (Fabric/Forge mods)
- **What it does:** Mods like Meteor, Wurst, Impact, Future, LiquidBounce, Aristois, and others bundle many hacks above.
- **Prevention:** Behavior detection as described. Mod channel registration checks give weak hints only.

---

## 8. Anticheat Design Principles

### Architecture
- Use **packet level** checks (PacketEvents, ProtocolLib) for accuracy and speed.
- Keep per player data (position history, rotation history, click history) with bounded memory.
- Run heavy checks async where safe, and keep Bukkit API calls on the main thread.
- Separate **detection** from **punishment**: checks produce violations, a manager decides actions.

### Reducing False Positives
- Use **violation levels** with decay, and buffers before flagging.
- Account for **ping and lag spikes** using transaction or keepalive based latency tracking.
- Account for edge cases: slime blocks, bubble columns, ladders, vines, scaffolding, cobwebs, honey, soul sand, ice, pistons, knockback, potions, enchantments (Depth Strider, Soul Speed), elytra, riptide, and version differences (ViaVersion clients).
- Offer config per check (enable, threshold, punishment) so owners can tune.

### Punishment Strategy
- **Setback (rubberband)** for movement checks, so cheaters gain nothing.
- **Cancel** the action for combat checks (cancel the hit).
- **Kick or temp ban** for repeated flags.
- **Delayed bans (ban waves)** so cheat developers cannot easily learn what triggered detection.
- Log evidence (recent packets, positions) for staff review.

### Advanced Techniques
- **Movement prediction engine:** simulate all possible vanilla inputs each tick and compare to the client's reported movement.
- **Machine learning** on combat data (aim, click patterns) as a second opinion, not sole proof.
- **Honeypots:** bots or fake entities and fake ores that only cheaters react to.
- **Statistical analysis** over long windows (mining, clicking, rotations).
- **Replay and review tools** so staff can verify flagged players.

### Staff and Community Measures
- Clear rules about allowed mods (for example Optifine, Sodium, Iris, minimaps).
- A report system with evidence review.
- Regular updates for new Minecraft versions, since new mechanics bring new bypasses.

---

## 9. Quick Reference Table

| Hack | Category | Main Prevention |
|------|----------|-----------------|
| KillAura | Combat | Raytrace, multi target check, rotation analysis |
| Reach | Combat | Hitbox distance with lag compensation |
| Velocity | Combat | Expected vs actual knockback |
| Criticals | Combat | Verify real fall state |
| AutoClicker | Combat | CPS limit and interval statistics |
| Aimbot | Combat | GCD and rotation pattern analysis |
| AutoTotem | Combat | Reaction time analysis |
| CrystalAura | Combat | Rate limits and reaction time |
| Fly | Movement | Physics simulation |
| Speed | Movement | Movement prediction engine |
| NoFall | Movement | Server side fall tracking |
| Phase | Movement | Collision sweep |
| Timer | Movement | Packet rate balance |
| Blink | Movement | Packet gap and burst detection |
| Scaffold | World | Placement angle, rate, rotation checks |
| Nuker | World | Break time and count limits |
| Xray | Visual | Anti-xray engine modes, statistical detection |
| ESP | Visual | Entity occlusion culling |
| ChestStealer | Inventory | Click rate limits |
| InventoryMove | Inventory | Inventory state tracking |
| Crash exploits | Exploit | Updated server, packet limits |
| Bot attacks | Exploit | Bot filter, throttling |
| Dupes | Exploit | Patches, logging, Paper |
| Offline spoofing | Exploit | Online mode, proxy forwarding secret |
| Bot join flood | AntiBot | Rate limits, attack mode, join verification |
| Fake client bots | AntiBot | Verification challenge, packet sequence checks |
| Chat spam bots | AntiBot | Message similarity, new player cooldowns |
| Mass grief raid | AntiRaid | Trust levels, lockdown mode, action rate limits |
| Coordinated accounts | AntiRaid | Correlation by IP, subnet, names, timing |

---

## 10. AntiBot

Bots are fake clients that join in large numbers to flood, crash, spam, or prepare a raid. The goal is to stop them before they ever reach the main world.

### Bot Types
| Type | Behavior |
|------|----------|
| Join flood | Thousands of login attempts per second to overload the server |
| Handshake / status flood | Spams ping and handshake packets without logging in |
| Fake client bots | Scripted clients that skip normal client packets and do nothing in game |
| Chat spam bots | Join and post ads, links, or repeated messages |
| Account generator bots | Random or sequential names, often from many proxies |
| Slow bots | Join at a low rate to avoid rate limits |
| Proxy / VPN bots | Rotate IPs to dodge IP based limits |
| Smart bots | Pass basic checks by sending movement and settings packets |

### Layer 1: Connection Level
- **Global and per IP connection rate limits** (connections per second, per minute).
- **Per subnet limits** (for example /24) so rotating IPs in one range still get caught.
- **Max concurrent connections per IP.**
- **Handshake validation:** reject unknown protocol numbers, invalid hostnames, bad ports, oversized or malformed handshake packets.
- **Status/ping flood limits:** cache the status response and rate limit pings per IP.
- **Login packet limits:** reject oversized names, invalid characters, and malformed login packets early.
- **Timeouts:** drop connections that stall in handshake or login states.
- **Firewall integration:** auto add abusive IPs to a temporary blocklist (iptables, ipset, nftables, or a cloud firewall). Do this before the packets reach the Java process when possible.

### Layer 2: Attack Mode
- Track **joins per second** and **failed logins per second** with a sliding window.
- When a threshold is exceeded, enable **attack mode** automatically:
  - Tighten all rate limits.
  - Force verification for every new connection.
  - Only allow **known players** (previously verified IP and UUID pairs) straight in.
  - Alert staff and log the attack.
- Turn attack mode off after a cooldown with no spikes.
- Allow a manual toggle command for staff.

### Layer 3: Join Verification (Limbo / Sandbox)
Send new or suspicious players to a lightweight **limbo** or isolated check world first. Then pass them through checks such as:
- **Gravity test:** the client must fall and land with correct physics.
- **Movement challenge:** require a small valid movement or rotation change.
- **Keepalive and transaction response timing:** real clients answer consistently, simple bots often do not.
- **Client settings packet:** real clients send locale, view distance, and skin parts. Many bots skip it.
- **Client brand packet and plugin channel behavior:** flag missing or impossible combinations (treat as a weak signal).
- **Packet order and timing:** flag clients that send packets in an order or rate a real client never does.
- **Optional CAPTCHA style challenge** (for example look at a target, click a specific item, type a code) only during attack mode.
- **Resource pack response** if you require one (bots often ignore it).

Once verified, store the result (UUID plus IP) so the player skips verification next time.

### Layer 4: Identity and Reputation
- **Online mode only.** Verified Mojang auth stops most cheap bots.
- **Name pattern analysis:** flag random strings, sequential numbers, and many joins with similar names (for example same prefix with changing digits).
- **Account per IP limits:** cap accounts allowed per IP in a time window.
- **IP reputation:** optional check for VPN, proxy, and datacenter ranges. Use as a **risk score**, not an automatic ban, because real players use VPNs.
- **Trust score per player:** combine account age on your server, playtime, verification result, IP history, and behavior. Low trust means more restrictions.
- **Whitelist / allowlist bypass** for staff and trusted players.

### Layer 5: Chat and Behavior Bots
- **New player chat cooldown** and slowmode.
- **Repeat and similarity detection:** same or near identical messages from one player or across several players.
- **Link and IP advertising filter** for players with low trust.
- **Command restrictions** for new players.
- **Flag idle bots:** players that join and never move or interact, then mass act later.
- **Mass identical action detection:** many players doing the same thing at the same time.

### Layer 6: Crash and Packet Abuse
- Packet per second limits per connection.
- Max packet size and max NBT depth and size.
- Limits on book pages, sign length, and window click frequency.
- Kick on protocol violations, and temp block the IP after repeated ones.

### Staff Tools
- `/antibot status`, `/antibot attack on|off`, `/antibot whitelist <player>`, `/antibot stats`.
- Discord or in game alerts when attack mode starts and ends.
- Logs with IP, name, time, and reason for each blocked connection.

### Tuning and False Positives
- Shared IPs (schools, households, mobile carriers, cafes) must not be punished too hard. Use soft limits and verification instead of bans.
- Offer **config presets** (relaxed, normal, strict) and per module toggles.
- Always let verified known players bypass attack mode restrictions.
- Test with simulated bot load on a staging server.

---

## 11. AntiRaid

A raid is a **coordinated group** joining to grief, spam, crash, or cause chaos. AntiRaid works on **behavior and coordination**, while AntiBot works on **connections and clients**. They should share data.

### Raid Types
| Type | What happens |
|------|--------------|
| Mass grief raid | Many accounts break, burn, or destroy builds |
| TNT / lava / fire raid | Explosions, lava, and fire spam to destroy quickly |
| Chat raid | Mass spam, advertising, slurs, or links |
| Command raid | Spamming commands, teleport requests, or plugin abuse |
| Entity / lag raid | Spawning mobs, items, minecarts, or redstone to crash the server |
| Kill raid | Mass attacking players at spawn or in hubs |
| Advertising raid | Spamming another server's IP |
| Insider / compromised account raid | A trusted or staff account is used to destroy things |

### Detection Signals
- **Join burst:** many new players in a short window.
- **Correlation:** accounts sharing IP, subnet, similar names, join timing, client behavior, or the same actions.
- **Mass action:** many players performing the same action (break, place, chat, command) within seconds.
- **New account destruction:** high block break, TNT, lava, or fire counts from low trust players.
- **Protected area hits:** many players hitting claims or regions they do not own.
- **Entity and explosion spikes** in a small area.
- **Chat similarity spike** across different players.

### Prevention
#### Trust Levels
Give every player a tier and gate actions by tier:
1. **New:** no TNT, lava, fire, flint and steel, fire charges, or explosives. No links in chat. Limited commands. Low block break and place rates. No PvP in protected zones.
2. **Member:** normal gameplay after a playtime threshold.
3. **Trusted:** fewer restrictions.
4. **Staff:** full access, with extra audit logging.

#### Lockdown Mode
- **Auto lockdown** when raid signals exceed thresholds, and a **manual panic command** for staff.
- Options while locked down:
  - Only known or trusted players can join.
  - New players join a waiting area or limbo.
  - Mute chat for low trust players.
  - Disable TNT, lava, fire, and explosives globally.
  - Freeze block breaking for new players.
- Clear status messages to players ("Server is under protection, please wait").

#### Rate Limits (per player, per region, and global)
- Blocks broken and placed per second and per minute.
- TNT, crystals, lava, water, fire, and explosives placed per minute.
- Entities spawned per chunk and per area.
- Commands per second, and teleport requests per minute.
- Chat messages per second.
- Item drops and inventory actions.

#### Area Protection
- Require claims or regions for builds that matter (spawn, hubs, bases).
- Make spawn and hub **indestructible** for non staff.
- Alert when a region takes many denied actions in a short time.

#### Coordinated Account Handling
- Group accounts by IP, subnet, join time, name pattern, and shared behavior.
- When a group triggers raid signals, **apply group wide restrictions** (mute, freeze, isolate) instead of acting on one account.
- Support **ban waves** for the whole correlated group after staff review, so the attacker cannot easily learn what triggered detection.

#### Chat Raid Defense
- Global and per player slowmode that tightens during a raid.
- Similarity filter across players (same message from many accounts).
- Block links, IPs, and known advertising patterns for low trust players.
- Auto mute with staff review queue.

#### Anti Crash and Lag
- Entity caps per chunk and per player.
- Limits on redstone clocks, hoppers, pistons, and fluid spreading near new accounts.
- Packet and click limits (shared with AntiBot Layer 6).

### Response and Recovery
- **Action logging** for every block change, container access, and explosion with player, time, and location (needed for rollback).
- **Rollback tools:** rollback by player, by group of correlated accounts, by area, and by time window.
- **Snapshots or backups** before and after risky events.
- **Staff alerts** in game and on Discord with a short summary (accounts involved, area, action counts).
- **Evidence export** (accounts, IPs, timestamps, actions) for bans and appeals.
- **Post raid report** to tune thresholds.

### Protecting Against Compromised Staff
- Require 2FA or login verification for staff accounts.
- Limit destructive commands (mass ban, op, world edits) and log them.
- Require a second staff confirmation for very large actions.
- Alert on unusual staff activity (new IP, mass actions).

### Tuning and False Positives
- Events, builders, and big communities create legit bursts. Offer **event mode** that raises limits for chosen regions or times.
- Use **scores and buffers** rather than instant punishments.
- Always allow staff to whitelist players and regions.
- Provide per module toggles and presets (relaxed, normal, strict).

### Suggested Commands
- `/antiraid status`, `/antiraid lockdown on|off`, `/antiraid trust <player> <level>`
- `/antiraid group <player>` to see correlated accounts
- `/antiraid rollback <player|group> <time>`
- `/antiraid event on|off <region>`

---


*Note: Names and behavior vary between cheat clients and versions. New cheats and bypasses appear constantly, so keep detections updated and test against current clients on your own test server.*
