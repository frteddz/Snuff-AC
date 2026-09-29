import { useEffect, useMemo, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { ArrowUpRight, Menu, X, BookOpen, Network, FlaskConical, Layers3, Server, CircleAlert } from 'lucide-react'
import './styles.css'

const repo = 'https://github.com/frteddz/Snuff-AC'
const docs = `${repo}/blob/main/docs/architecture.md`
const credits = `${repo}/blob/main/credits.md`

type Category = 'Movement' | 'Combat' | 'World' | 'Packet'
type Check = { name: string; category: Category; concept: string; detail: string }

const checks: Check[] = [
  { name: 'Fly', category: 'Movement', concept: 'Prediction', detail: 'Flags movement that cannot be explained by the server-side physics model.' },
  { name: 'Speed', category: 'Movement', concept: 'Kinematics', detail: 'Measures movement against attributes, environment, and recovered input candidates.' },
  { name: 'NoFall', category: 'Movement', concept: 'Fall state', detail: 'Checks whether fall state and impact behavior remain consistent.' },
  { name: 'AirMovement', category: 'Movement', concept: 'Air control', detail: 'Evaluates airborne acceleration against the predicted movement state.' },
  { name: 'GroundSpoof', category: 'Movement', concept: 'Ground state', detail: 'Tests claimed ground state against observed movement and world context.' },
  { name: 'Step', category: 'Movement', concept: 'Collision', detail: 'Checks vertical movement against step height and collision context.' },
  { name: 'HighJump', category: 'Movement', concept: 'Jump model', detail: 'Detects jump displacement beyond the modelled jump behavior.' },
  { name: 'LongJump', category: 'Movement', concept: 'Momentum', detail: 'Looks for horizontal distance inconsistent with the current movement state.' },
  { name: 'ImpossibleMovement', category: 'Movement', concept: 'State validity', detail: 'Catches movement sequences the predictor cannot reconcile.' },
  { name: 'GroundFlag', category: 'Movement', concept: 'World contradiction', detail: 'Detects sustained ground contact claims that contradict the server block view, the signature of air walk and no fall spoofing.' },
  { name: 'PitchLock', category: 'Movement', concept: 'Constant input', detail: 'Detects pitch pinned to an exact constant, which placement and glide modules use to defeat server heuristics.' },
  { name: 'Drift', category: 'Movement', concept: 'Sustained offset', detail: 'Detects a sustained constant per tick offset between prediction and reported position. The discriminator is drift rate, not magnitude, and it no longer depends on another check running first.' },
  { name: 'Velocity', category: 'Movement', concept: 'Knockback', detail: 'Checks response to server-applied velocity with tolerance for legitimate context.' },
  { name: 'Reach', category: 'Combat', concept: 'Hitbox geometry', detail: 'Validates attack distance against the server resolved hitbox, never the client cursor. Handles survival, creative and vehicle limits.' },
  { name: 'AttackAngle', category: 'Combat', concept: 'Ray against hitbox', detail: "Casts the attacker&#39;s real look vector against the true vanilla hitbox and cancels hits that only landed on an expanded box. Catches hitbox cheats that reach alone cannot see." },
  { name: 'AutoClicker', category: 'Combat', concept: 'Timing', detail: 'Evaluates attack timing patterns visible in packet behavior.' },
  { name: 'Aim', category: 'Combat', concept: 'Rotation', detail: 'Checks suspicious rotation behavior around attacks.' },
  { name: 'KillAura', category: 'Combat', concept: 'Targeting', detail: 'Evaluates attack and target sequences for impossible behavior.' },
  { name: 'ImpossibleAttack', category: 'Combat', concept: 'Sequence', detail: 'Flags attack sequences that do not fit server-observable state.' },
  { name: 'Critical', category: 'Combat', concept: 'Forced critical', detail: 'Detects forced criticals produced by emitting extra position packets with a vertical lift too small for gravity, immediately before an attack.' },
  { name: 'RotationSnapBack', category: 'Combat', concept: 'Rotation sequence', detail: 'Detects a large aim rotation immediately before an attack followed by a reverse rotation immediately after, which human input does not produce.' },
  { name: 'InvalidAttackState', category: 'Combat', concept: 'State validity', detail: 'Checks whether an attack is valid for the current player state.' },
  { name: 'FastBreak', category: 'World', concept: 'Break timing', detail: 'Checks block break timing against material and tool context.' },
  { name: 'FastPlace', category: 'World', concept: 'Interaction rate', detail: 'Checks block placement timing and interaction state.' },
  { name: 'Scaffold', category: 'World', concept: 'Placement', detail: 'Evaluates placement behavior and the movement context around it.' },
  { name: 'MiningBeyondView', category: 'World', concept: 'Information leak', detail: 'Detects targeting valuable ores in a region the server never sent, which is knowledge the client could not legitimately have.' },
  { name: 'Nuker', category: 'World', concept: 'Block interaction', detail: 'Looks for impossible or excessive block break behavior.' },
  { name: 'BadPackets', category: 'Packet', concept: 'Protocol', detail: 'Checks malformed or invalid packet state.' },
  { name: 'PacketSpam', category: 'Packet', concept: 'Rate', detail: 'Checks packet volume and timing for abusive patterns.' },
  { name: 'ExtraPackets', category: 'Packet', concept: 'Tick invariant', detail: 'Detects more than one position packet per server tick. Vanilla sends exactly one, so this catches packet replay generically. Now backed by a real per tick counter.' },
  { name: 'PacketRate', category: 'Packet', concept: 'Rate deviation', detail: 'Detects a sustained deviation of the movement packet rate from the server tick rate, gated on low ping.' },
  { name: 'Timer', category: 'Packet', concept: 'Clock', detail: 'Evaluates packet timing against server tick behavior.' },
]

const tolerance = ['Knockback', 'Pistons', 'Slime', 'Ice', 'Item use', 'Vehicles', 'Teleports', 'Latency', 'Low TPS']

function PixelIcon({ name, alt = '' }: { name: string; alt?: string }) {
  return <img className="pixel-icon" src={`/assets/${name}.svg`} alt={alt} aria-hidden={!alt} />
}

function Logo() {
  return <a className="logo" href="#top" aria-label="Snuff AC home"><img src="/fav-icon.png" alt="" /><span>snuff <b>ac</b></span></a>
}

function SectionLabel({ index, children }: { index: string; children: string }) {
  return <div className="section-label"><span>{index}</span><i />{children}</div>
}

function App() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [activeCategory, setActiveCategory] = useState<'All' | Category>('All')
  const [activeCheck, setActiveCheck] = useState<Check>(checks[1])
  const heroVideo = useRef<HTMLVideoElement>(null)
  const visibleChecks = useMemo(() => activeCategory === 'All' ? checks : checks.filter((check) => check.category === activeCategory), [activeCategory])

  useEffect(() => {
    if (heroVideo.current) heroVideo.current.playbackRate = 1
    window.scrollTo(0, 0)
    const sections = Array.from(document.querySelectorAll('main section'))
    sections.forEach((section) => section.classList.add('scroll-reveal'))
    const observer = new IntersectionObserver((entries) => entries.forEach((entry) => {
      if (entry.isIntersecting) entry.target.classList.add('in-view')
    }), { threshold: 0.12 })
    sections.forEach((section) => observer.observe(section))
    return () => observer.disconnect()
  }, [])

  const closeMenu = () => setMenuOpen(false)

  return <div id="top">
    <header className="nav-wrap">
      <nav className="nav shell" aria-label="Main navigation">
        <button className="menu-button" onClick={() => setMenuOpen(!menuOpen)} aria-expanded={menuOpen} aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}>{menuOpen ? <X size={20} /> : <Menu size={20} />}</button>
        <div className={`nav-links ${menuOpen ? 'open' : ''}`}>
          <a href="#method" onClick={closeMenu}>Method</a>
          <a href="#architecture" onClick={closeMenu}>Architecture</a>
          <a href="#checks" onClick={closeMenu}>Checks</a>
          <a href="#research" onClick={closeMenu}>Research</a>
          <a className="nav-github" href={repo} target="_blank" rel="noreferrer" onClick={closeMenu}><img className="social-icon" src="/social/github_icon.png" alt="" /> GitHub <ArrowUpRight size={14} /></a>
        </div>
      </nav>
    </header>

    <main>
      <section className="hero">
        <video ref={heroVideo} className="hero-background" src="/hero-background.mp4" autoPlay muted loop playsInline aria-hidden="true" />
        <div className="shell">
        <div className="hero-copy">
          <h1><img className="hero-wordmark" src="/snuff.png" alt="Snuff" /><br /><em>Evidence-based detection.</em></h1>
          <p className="hero-lede">An open-source Minecraft Java Edition anticheat built around prediction, evidence, and server-side authority.</p>
          <div className="hero-actions"><a className="button button-primary" href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src="/social/github_icon.png" alt="" /> View on GitHub <ArrowUpRight size={15} /></a><a className="button button-kofi" href="https://ko-fi.com/A1A3259HI2" target="_blank" rel="noreferrer"><img className="kofi-icon" src="/social/kofi_icon.png" alt="" /> Support me on Ko-fi <ArrowUpRight size={15} /></a><a className="button button-quiet" href={docs} target="_blank" rel="noreferrer"><BookOpen size={17} /> Read the architecture</a></div>
        </div>
        <div className="hero-visual" aria-hidden="true" />
        </div>
      </section>

      <section className="section shell" id="method">
        <SectionLabel index="01">The evidence model</SectionLabel>
        <div className="section-heading"><h2>Not every violation<br /><em>starts with a ban.</em></h2><p>A check produces evidence. Evidence accumulates. The response comes later, after context has had its say.</p></div>
        <div className="evidence-layout">
          <div className="evidence-rail">
            {['Observation', 'Check', 'Evidence', 'Buffer', 'Violation level', 'Alert / setback'].map((item, index) => <div className={`evidence-step ${index === 2 ? 'current' : ''}`} key={item}><span>0{index + 1}</span><b>{item}</b>{index < 5 && <i />}</div>)}
          </div>
          <div className="evidence-card">
            <div className="card-top"><span>VIOLATION PIPELINE</span><span className="live-chip"><i /> LIVE MODEL</span></div>
            <div className="buffer-graph"><div className="graph-axis"><span>confidence</span><span>time</span></div><div className="graph-line"><i /><i /><i /><i /><i /><i /><i /></div><div className="graph-threshold"><span>setback threshold</span></div></div>
            <div className="buffer-readout"><div><small>BUFFER</small><strong>6.4</strong><span>decaying</span></div><div><small>VIOLATION LEVEL</small><strong>4.0</strong><span>recorded</span></div><div><small>CONTEXT</small><strong>0.18</strong><span>tolerance applied</span></div></div>
          </div>
        </div>
        <div className="three-columns"><article><PixelIcon name="analytics-sharp" /><h3>Proportional evidence</h3><p>Buffers add according to how far a threshold was exceeded, then bleed down while the player is clean.</p></article><article><PixelIcon name="refresh-sharp" /><h3>Named tolerance</h3><p>Allowance comes from inspectable sources instead of a single hidden epsilon.</p></article><article><PixelIcon name="megaphone" /><h3>Separate response</h3><p>Recording, alerting, logging, setbacks, and configured actions are handled as distinct stages.</p></article></div>
      </section>

      <section className="dark-section" id="prediction"><div className="shell prediction-section"><SectionLabel index="02">Movement prediction</SectionLabel><div className="section-heading"><h2>Predict the movement.<br /><em>Measure the deviation.</em></h2><p>The server cannot see which keys a player is holding. Snuff enumerates 36 discretised input candidates, applies vanilla kinematics, and keeps the best fit.</p></div>
        <div className="flow-diagram"><div className="flow-node"><PixelIcon name="terminal" /><span>client movement</span><small>packet</small></div><div className="flow-arrow" /><div className="flow-node"><PixelIcon name="server" /><span>server state</span><small>attributes + world</small></div><div className="flow-arrow" /><div className="flow-node active"><PixelIcon name="cpu-sharp" /><span>36 candidates</span><small>input enumeration</small></div><div className="flow-arrow" /><div className="flow-node"><PixelIcon name="analytics-sharp" /><span>observed / predicted</span><small>deviation</small></div><div className="flow-arrow" /><div className="flow-node"><PixelIcon name="filter" /><span>tolerance</span><small>evidence</small></div></div>
        <div className="physics-notes"><div><span>0.2806 b/t</span><p>Sprinting</p></div><div><span>0.2159 b/t</span><p>Walking</p></div><div><span>0.0648 b/t</span><p>Sneaking</p></div><div><span>3.92 b/t</span><p>Terminal velocity</p></div></div>
      </div></section>

      <section className="section shell tolerance-section"><SectionLabel index="03">False positive handling</SectionLabel><div className="section-heading"><h2>Context before<br /><em>conclusion.</em></h2><p>Not every impossible-looking movement event is a cheat. The tolerance model keeps legitimate causes visible, named, and temporary.</p></div><div className="tolerance-layout"><div className="tolerance-wheel"><div className="wheel-center">ALLOWANCE<br /><b>DECAYS</b></div>{tolerance.map((item, i) => <span className={`tol tol-${i}`} key={item}>{item}</span>)}</div><div className="tolerance-copy"><div className="note-line"><span>01</span><p><b>Sources are explicit.</b> External pushes, pistons, slime, ice, item use slowdown, vehicles, teleports, latency, and low TPS each have a named place in the model.</p></div><div className="note-line"><span>02</span><p><b>Allowance is bounded.</b> Contributions decay every tick and the total is clamped. A single flag cannot bank unlimited leniency.</p></div><div className="note-line"><span>03</span><p><b>Applicability matters.</b> Checks can skip themselves when their assumptions do not hold instead of producing misleading evidence.</p></div></div></div></section>

      <section className="section shell punish-section"><SectionLabel index="04">Manual punishment</SectionLabel><div className="section-heading"><h2>Staff decide.<br /><em>Snuff never does.</em></h2><p>Every punishment here is typed by a staff member and recorded against their name. The only automatic action is the optional warning ladder, which is off by default and can be set to warn without ever banning.</p></div><div className="punish-grid">{[['ban / tempban / ipban','Permanent, timed and address based bans, enforced before the player loads a world.'],['timeout','Temporary kick for a fixed duration.'],['mute / tempmute','Blocks chat and commands until lifted.'],['warn','A recorded warning with a mandatory reason.'],['unban and the rest','Every action has an exact reverse that logs who lifted it.'],['Caps and protection','maxduration permissions stop junior staff issuing long bans, and staff can be marked unpunishable.']].map(([title, copy]) => <div className="punish-card" key={title}><b>{title}</b><p>{copy}</p></div>)}</div><div className="prev-note"><CircleAlert size={17} /><p>Durations accept <b>m</b>, <b>h</b> and <b>d</b> in any order, so <b>1h 10s</b> is valid. A reason is required on every punish command. Run <b>/snuff punishments [player]</b> to review what someone is serving, and <b>/snuff settings</b> to change retention, prevention and the ladder.</p></div></section>

      <section className="section shell prevention-section"><SectionLabel index="05">Prevention</SectionLabel><div className="section-heading"><h2>Flagging is not<br /><em>prevention.</em></h2><p>Detection that only tells you after the fact is a report. This release can act on the packet, before the server acts on it.</p></div><div className="prev-grid">{[['Setback position','Corrects the client to the last accepted position.'],['Cancel attack','Drops the attack before the server swings.'],['Cancel placement','Drops the block placement before it exists.'],['Cancel break','Drops the block break before it resolves.'],['Sync position','Resynchronises the client when it drifts from authority.'],['Confidence gate','Every action requires accumulated confidence.']].map(([title, copy], i) => <div className="prev-card" key={title}><span>{String(i + 1).padStart(2, '0')}</span><b>{title}</b><p>{copy}</p></div>)}</div><div className="prev-note"><CircleAlert size={17} /><p>Prevention is confidence gated and can be disabled globally without suppressing flagging or evidence. Visual cheats are neutralised at the source: ores are sent as decoy states, entities with no legal line of sight are hidden from the client, and sounds from behind cover have their origin nudged. Snuff never bans, kicks, mutes or freezes anyone by itself. Every punishment is a staff decision.</p></div></section>

      <section className="dark-section checks-section" id="checks"><div className="shell"><SectionLabel index="06">Detection coverage</SectionLabel><div className="section-heading"><h2>32 checks.<br /><em>One evidence system.</em></h2><p>The current release groups focused checks across movement, combat, world interaction, and packet behavior. Every check is enabled by default. This release was driven by a defensive study of seven open source cheat clients, and by false positives caught during live play.</p></div><div className="checks-toolbar" role="tablist" aria-label="Check categories">{(['All', 'Movement', 'Combat', 'World', 'Packet'] as const).map((category) => <button className={activeCategory === category ? 'active' : ''} key={category} onClick={() => setActiveCategory(category)} role="tab" aria-selected={activeCategory === category}>{category}<span>{category === 'All' ? 32 : checks.filter((check) => check.category === category).length}</span></button>)}</div><div className="checks-layout"><div className="check-list">{visibleChecks.map((check) => <button className={`check-row ${activeCheck.name === check.name ? 'selected' : ''}`} onClick={() => setActiveCheck(check)} key={check.name}><span className="check-index">{String(checks.indexOf(check) + 1).padStart(2, '0')}</span><b>{check.name}</b><small>{check.category}</small><ArrowUpRight size={15} /></button>)}</div><div className="check-detail"><div className="detail-icon"><PixelIcon name={activeCheck.category === 'Movement' ? 'server' : activeCheck.category === 'Combat' ? 'crosshair' : activeCheck.category === 'World' ? 'box' : 'terminal'} /></div><span className="detail-category">{activeCheck.category} / active by default</span><h3>{activeCheck.name}</h3><p>{activeCheck.detail}</p><div className="detail-concept"><span>TECHNICAL CONCEPT</span><b>{activeCheck.concept}</b></div></div></div></div></section>

      <section className="section shell" id="architecture"><SectionLabel index="07">Architecture</SectionLabel><div className="section-heading"><h2>One core.<br /><em>Several edges.</em></h2><p>Platform code translates foreign packet wrappers into immutable records. The core never directly depends on Bukkit, Paper, Velocity, or platform packet objects.</p></div><div className="architecture-map"><div className="arch-column"><div className="arch-box platform"><span>PLATFORM EDGE</span><b>Paper / Purpur / Velocity</b><small>PacketEvents wrappers</small></div><div className="arch-connector" /><div className="arch-box"><span>TRANSLATION</span><b>Platform-neutral packet records</b><small>MovementPacket, AttackPacket, BlockBreakPacket</small></div><div className="arch-connector" /><div className="arch-box core"><span>ENGINE</span><b>Snuff AC Core</b><small>queue → dedicated check thread → dispatcher</small></div></div><div className="arch-side"><div className="arch-box"><span>CHECKS</span><b>32 focused checks</b><small>Stateless instances with per-player state</small></div><div className="arch-box"><span>HANDLING</span><b>Evidence / violation handler</b><small>Record, alert, log, setback, API</small></div><div className="arch-box"><span>DEVELOPER SURFACE</span><b>snuffac-api</b><small>Read-only checks, violations, listeners</small></div></div></div><div className="architecture-foot"><div><Network size={18} /><span>Data boundary, not platform inheritance</span></div><div><Layers3 size={18} /><span>Shared detection logic</span></div><div><FlaskConical size={18} /><span>Core tests without a server</span></div></div></section>

      <section className="section shell threading"><SectionLabel index="08">Performance and testing</SectionLabel><div className="two-up"><div><h2>Keep the hot path<br /><em>small and predictable.</em></h2><p>The packet thread decodes and queues. One dedicated daemon thread drains the queue and runs detection. The server tick refreshes immutable world data and applies setbacks.</p><div className="thread-stack"><div><span>NETTY</span><b>decode → stamp → enqueue</b></div><div><span>CHECK THREAD</span><b>drain → dispatch → evidence</b></div><div><span>MAIN THREAD</span><b>cache world → apply setback</b></div></div></div><div className="test-card"><div className="test-heading"><FlaskConical size={21} /><span>ENGINE TEST SUITE</span></div><strong>308</strong><p>passing unit tests</p><div className="test-tags"><span>kinematics</span><span>prediction</span><span>buffers</span><span>tolerance</span><span>configuration</span><span>registry</span><span>integration</span></div><a href={`${repo}/tree/main/snuffac-core/src/test`} target="_blank" rel="noreferrer">Inspect the test suite <ArrowUpRight size={14} /></a></div></div></section>

      <section className="research-section" id="research"><div className="shell research-grid"><div><SectionLabel index="09">Research and independence</SectionLabel><h2>Studied, understood,<br /><em>rebuilt independently.</em></h2><p>Snuff AC was developed by studying existing anticheat architecture, detection concepts, threading models, and failure modes. It was then written from scratch. No source code was copied.</p><a className="text-link" href={credits} target="_blank" rel="noreferrer">Read the full research credits <ArrowUpRight size={15} /></a></div><div className="research-list">{['GrimAC', 'Windfall AntiCheat', 'Updated-NoCheatPlus', 'AntiHaxerman', 'Medusa-Lite', 'Iris', 'Hawk'].map((name, index) => <div key={name}><span>0{index + 1}</span><b>{name}</b><small>architecture reference</small></div>)}<div className="research-foot">12 projects documented in credits.md<br />Licences read before research use</div></div></div></section>

      <section className="section shell honesty"><SectionLabel index="10">The server-side boundary</SectionLabel><div className="section-heading"><h2>Detect what you can<br /><em>actually observe.</em></h2><p>A server cannot see everything happening on a player’s client. Snuff focuses on resulting server-observable behavior, not on pretending the client is transparent.</p></div><div className="honesty-grid"><article><span>01 / DIRECT</span><h3>Packets and state</h3><p>Movement, attacks, rotations, interactions, timing, and invalid protocol behavior can be evaluated from server inputs.</p></article><article><span>02 / PARTIAL</span><h3>Information abuse</h3><p>Reach, targeting, Scaffold, and hidden information require behavioral evidence and server-side validation.</p></article><article><span>03 / OUTSIDE VIEW</span><h3>Purely client-side changes</h3><p>X-Ray, ESP and tracers cannot be directly seen, so Snuff stops volunteering the information instead: obfuscated ores, hidden entities without line of sight, and fuzzed sound origins. Freecam and fullbright remain genuinely out of reach.</p></article></div></section>

      <section className="platform-section"><div className="shell"><SectionLabel index="11">Platform support</SectionLabel><div className="platform-grid"><div className="platform-card primary"><Server size={20} /><span>PRIMARY TARGET</span><h3>Paper 1.21.11</h3><p>Full check set. Tested end to end on Paper build 132 with Java 21.</p><b>TESTED</b></div><div className="platform-card"><Layers3 size={20} /><span>COMPATIBLE</span><h3>Purpur</h3><p>Uses the same build with runtime platform detection. No unnecessary Purpur-specific implementation.</p><b>COMPATIBLE</b></div><div className="platform-card limited"><Network size={20} /><span>LIMITED</span><h3>Velocity 3.4.0</h3><p>Packet and network timing checks only. A proxy has no world data or player position API, so world and combat geometry checks are inactive.</p><b>PACKET LEVEL</b></div></div></div></section>

      <section className="section shell limits-section"><SectionLabel index="12">Honest limitations</SectionLabel><div className="section-heading"><h2>What this<br /><em>cannot do.</em></h2><p>An anticheat that claims to detect everything is lying. These are the known blind spots, stated plainly rather than filled with placeholder checks.</p></div><div className="limits-list">{[['X-Ray, ESP, StorageESP, ore search','Not detectable, but preventable. There is no protocol signal, so instead the server withholds the data: ores are sent as decoy blocks, and entities with no legal line of sight are not sent at all.'],['Fullbright and other local rendering','Not detectable and not preventable. Nothing leaves the client, so nothing arrives at the server.'],['ViaVersion and Geyser movement','Not yet modelled. Bedrock and legacy protocol clients move differently, so both are exempted rather than risk false positives.'],['Collision and skipped tick prediction','Not yet simulated. Speed related thresholds stay untuned until the predictor models post 1.8.2 skipped ticks and collision resolution.'],['Menu click through by a human','Still unverified. Nobody has opened every menu on a live server, which is why three consecutive GUI fixes were each believed done and each were not.']].map(([title, copy]) => <div className="limit-row" key={title}><span>NOT DETECTABLE</span><b>{title}</b><p>{copy}</p></div>)}</div></section>

      <section className="section shell whatsnew-section"><SectionLabel index="13">v1.0.9</SectionLabel><div className="section-heading"><h2>What changed in<br /><em>this release.</em></h2><p>v1.0.8 made detection real for the first time, and a legitimately cheating player immediately found the places where that exposed an arithmetic error and two design mistakes. All of them are fixed, prevention can no longer fire on a single guess, and player reports plus fully owner-editable menus are in.</p></div><div className="whatsnew-list">{[
['A rate calculation that flagged everyone','PacketSpam divided the packet count by the elapsed time plus one. The first packet of a window therefore reported 1000 packets per second, and the window restarted at an arbitrary packet so it kept happening. A player standing still was flagged every two seconds, and the live log proved it: one packet, reported as a flood. A window is now only judged once it has been open long enough for the division to mean anything, and the rate has to hold across several windows before it alerts.'],
['Checks no longer guess at what you are doing','BadPackets flagged any block interaction that did not match an active dig, which is simply what mining and building looks like. That rule is gone, because a check called badpackets should only reject packets that cannot be decoded.'],
['Prevention cannot fire on a single guess','v1.0.8 let any check teleport a player on its first flag, which turned that false positive into rubber-banding while bridging. Every check now declares whether its evidence is structural or derived, and a derived check, which is anything measuring a rate, a ratio or a window, is structurally unable to move a player until one level past its alert threshold.'],
['Alerts carry the Snuff prefix','Alerts were built by a separate formatter that used a plain bracket word and never touched the gradient. Alerts now carry the same prefix as /snuff, and the console line is rendered separately as plain text because a terminal cannot show a gradient.'],
['Anti-X-Ray stops guessing at internals','The reflection chain used in v1.0.8 does not exist on modern Paper. It now tries four documented strategies in order and reports every one it attempted, and the failure log names the exception and its stack frames instead of printing null.'],
['Concealment can reveal again','The entity hiding pass could only ever hide, never show, and its first frame sent nothing at all. It now tracks an explicit visible or hidden state per viewer, seeds it correctly, and reveals entities the player is legally allowed to see. It fails safe toward visible.'],
['Player reports','/snuff report opens a seven category picker covering cheating, exploiting, explicit language, offensive behaviour, griefing, inappropriate names and staff impersonation. /snuff reports opens the admin view with claim and resolve, so no report is silently dropped and two admins cannot both act on it. A cheating report attaches Snuff own flag evidence automatically.'],
['Every menu is yours to edit','Six menu files in a GUI folder, one per menu. Material, name, lore, slot, action, permission, enabled, amount and glint are all configurable, along with rows, title and filler. Actions are an allowlist of identifiers, never a command string.'],
].map(([title, copy]) => <div className="whatsnew-row" key={title}><b>{title}</b><p>{copy}</p></div>)}</div></section>

      <section className="section shell roadmap"><div className="roadmap-head"><div><SectionLabel index="14">Development status</SectionLabel><h2>Useful now.<br /><em>Honest about what’s next.</em></h2></div><div className="release-stamp"><span>DEVELOPMENT RELEASE</span><b>1.0.9-dev</b><small>not recommended for production public servers</small></div></div><div className="roadmap-list">{['Case notes and second opinions', 'Collision and skipped tick prediction', 'Acknowledged velocity and transaction tracking', 'Database backends and staff audit log', 'Folia compatibility decision'].map((item, index) => <div key={item}><span>{String(index + 1).padStart(2, '0')}</span><p>{item}</p><small>{index === 4 ? 'future enforcement' : 'planned work'}</small></div>)}</div><div className="roadmap-note"><CircleAlert size={17} /><p>The warning ladder is the one automatic action, and it is off until an owner turns it on after tuning. Every other punishment in this release is a staff decision.</p></div></section>

      <section className="cta-section"><div className="shell cta"><div className="cta-mark"><img src="/fav-icon.png" alt="" /></div><h2>Build your server<br /><em>on evidence.</em></h2><p>Open source means you can inspect how it works.</p><div className="hero-actions"><a className="button button-primary" href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src="/social/github_icon.png" alt="" /> View on GitHub <ArrowUpRight size={15} /></a><a className="button button-kofi" href="https://ko-fi.com/A1A3259HI2" target="_blank" rel="noreferrer"><img className="kofi-icon" src="/social/kofi_icon.png" alt="" /> Support me on Ko-fi <ArrowUpRight size={15} /></a><a className="button button-quiet" href={docs} target="_blank" rel="noreferrer"><BookOpen size={17} /> Documentation</a></div></div></section>
    </main>
    <section className="section shell shaders-section"><SectionLabel index="15">Shaders</SectionLabel><div className="section-heading"><h2>Every frame on<br /><em>this page.</em></h2><p>All background imagery is rendered Minecraft footage using community shader packs, credited below. Snuff AC itself is not a resource pack and does not ship any of these.</p></div><div className="shader-list">{[['Bliss Shaders', 'https://modrinth.com/shader/bliss-shader'], ['Arc', 'https://modrinth.com/shader/arc-shader'], ['Solas Shader', 'https://modrinth.com/shader/solas-shader'], ['Noble Shaders', 'https://modrinth.com/shader/noble'], ['Super Duper Vanilla', 'https://modrinth.com/shader/super-duper-vanilla']].map(([name, href]) => <a key={name} href={href} target="_blank" rel="noreferrer"><b>{name}</b><ArrowUpRight size={14} /></a>)}</div></section>

    <footer className="footer shell"><div><a href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src="/social/github_icon.png" alt="" /> GitHub</a><a href={credits} target="_blank" rel="noreferrer">Credits</a><a href={`${repo}/blob/main/CHANGELOG.md`} target="_blank" rel="noreferrer">Changelog</a><a href={`${repo}/blob/main/LICENSE`} target="_blank" rel="noreferrer">GPL-3.0</a></div></footer>
  </div>
}

export default App

createRoot(document.getElementById('root')!).render(<App />)
