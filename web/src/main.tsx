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
  { name: 'Drift', category: 'Movement', concept: 'Sustained offset', detail: 'Detects a sustained constant per tick offset between prediction and reported position. The discriminator is drift rate, not magnitude.' },
  { name: 'Velocity', category: 'Movement', concept: 'Knockback', detail: 'Checks response to server-applied velocity with tolerance for legitimate context.' },
  { name: 'Reach', category: 'Combat', concept: 'Hitbox geometry', detail: 'Validates attack distance against the server resolved hitbox, never the client cursor. Handles survival, creative and vehicle limits.' },
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
  { name: 'ExtraPackets', category: 'Packet', concept: 'Tick invariant', detail: 'Detects more than one position packet per server tick. Vanilla sends exactly one, so this catches packet replay generically.' },
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
      <section className="hero shell">
        <video ref={heroVideo} className="hero-background" src="/hero-background.mp4" autoPlay muted loop playsInline aria-hidden="true" />
        <div className="hero-copy">
          <h1><img className="hero-wordmark" src="/snuff.png" alt="Snuff" /><br /><em>Evidence-based detection.</em></h1>
          <p className="hero-lede">An open-source Minecraft Java Edition anticheat built around prediction, evidence, and server-side authority.</p>
          <div className="hero-actions"><a className="button button-primary" href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src="/social/github_icon.png" alt="" /> View on GitHub <ArrowUpRight size={15} /></a><a className="button button-kofi" href="https://ko-fi.com/A1A3259HI2" target="_blank" rel="noreferrer"><img className="kofi-icon" src="/social/kofi_icon.png" alt="" /> Support me on Ko-fi <ArrowUpRight size={15} /></a><a className="button button-quiet" href={docs} target="_blank" rel="noreferrer"><BookOpen size={17} /> Read the architecture</a></div>
        </div>
        <div className="hero-visual" aria-hidden="true" />
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

      <section className="section shell prevention-section"><SectionLabel index="04">Prevention</SectionLabel><div className="section-heading"><h2>Flagging is not<br /><em>prevention.</em></h2><p>Detection that only tells you after the fact is a report. This release can act on the packet, before the server acts on it.</p></div><div className="prev-grid">{[['Setback position','Corrects the client to the last accepted position.'],['Cancel attack','Drops the attack before the server swings.'],['Cancel placement','Drops the block placement before it exists.'],['Cancel break','Drops the block break before it resolves.'],['Sync position','Resynchronises the client when it drifts from authority.'],['Confidence gate','Every action requires accumulated confidence.']].map(([title, copy], i) => <div className="prev-card" key={title}><span>{String(i + 1).padStart(2, '0')}</span><b>{title}</b><p>{copy}</p></div>)}</div><div className="prev-note"><CircleAlert size={17} /> Prevention is confidence gated and can be disabled globally without suppressing flagging or evidence. Anti-X-Ray obfuscation replaces valuable ores with decoy states before they are written to the client.</div></section>

      <section className="dark-section checks-section" id="checks"><div className="shell"><SectionLabel index="04">Detection coverage</SectionLabel><div className="section-heading"><h2>31 checks.<br /><em>One evidence system.</em></h2><p>The current release groups focused checks across movement, combat, world interaction, and packet behavior. Every check is enabled by default. This release was driven by a defensive study of seven open source cheat clients, and by false positives caught during live play.</p></div><div className="checks-toolbar" role="tablist" aria-label="Check categories">{(['All', 'Movement', 'Combat', 'World', 'Packet'] as const).map((category) => <button className={activeCategory === category ? 'active' : ''} key={category} onClick={() => setActiveCategory(category)} role="tab" aria-selected={activeCategory === category}>{category}<span>{category === 'All' ? 31 : checks.filter((check) => check.category === category).length}</span></button>)}</div><div className="checks-layout"><div className="check-list">{visibleChecks.map((check) => <button className={`check-row ${activeCheck.name === check.name ? 'selected' : ''}`} onClick={() => setActiveCheck(check)} key={check.name}><span className="check-index">{String(checks.indexOf(check) + 1).padStart(2, '0')}</span><b>{check.name}</b><small>{check.category}</small><ArrowUpRight size={15} /></button>)}</div><div className="check-detail"><div className="detail-icon"><PixelIcon name={activeCheck.category === 'Movement' ? 'server' : activeCheck.category === 'Combat' ? 'crosshair' : activeCheck.category === 'World' ? 'box' : 'terminal'} /></div><span className="detail-category">{activeCheck.category} / active by default</span><h3>{activeCheck.name}</h3><p>{activeCheck.detail}</p><div className="detail-concept"><span>TECHNICAL CONCEPT</span><b>{activeCheck.concept}</b></div></div></div></div></section>

      <section className="section shell" id="architecture"><SectionLabel index="05">Architecture</SectionLabel><div className="section-heading"><h2>One core.<br /><em>Several edges.</em></h2><p>Platform code translates foreign packet wrappers into immutable records. The core never directly depends on Bukkit, Paper, Velocity, or platform packet objects.</p></div><div className="architecture-map"><div className="arch-column"><div className="arch-box platform"><span>PLATFORM EDGE</span><b>Paper / Purpur / Velocity</b><small>PacketEvents wrappers</small></div><div className="arch-connector" /><div className="arch-box"><span>TRANSLATION</span><b>Platform-neutral packet records</b><small>MovementPacket, AttackPacket, BlockBreakPacket</small></div><div className="arch-connector" /><div className="arch-box core"><span>ENGINE</span><b>Snuff AC Core</b><small>queue → dedicated check thread → dispatcher</small></div></div><div className="arch-side"><div className="arch-box"><span>CHECKS</span><b>31 focused checks</b><small>Stateless instances with per-player state</small></div><div className="arch-box"><span>HANDLING</span><b>Evidence / violation handler</b><small>Record, alert, log, setback, API</small></div><div className="arch-box"><span>DEVELOPER SURFACE</span><b>snuffac-api</b><small>Read-only checks, violations, listeners</small></div></div></div><div className="architecture-foot"><div><Network size={18} /><span>Data boundary, not platform inheritance</span></div><div><Layers3 size={18} /><span>Shared detection logic</span></div><div><FlaskConical size={18} /><span>Core tests without a server</span></div></div></section>

      <section className="section shell threading"><SectionLabel index="06">Performance and testing</SectionLabel><div className="two-up"><div><h2>Keep the hot path<br /><em>small and predictable.</em></h2><p>The packet thread decodes and queues. One dedicated daemon thread drains the queue and runs detection. The server tick refreshes immutable world data and applies setbacks.</p><div className="thread-stack"><div><span>NETTY</span><b>decode → stamp → enqueue</b></div><div><span>CHECK THREAD</span><b>drain → dispatch → evidence</b></div><div><span>MAIN THREAD</span><b>cache world → apply setback</b></div></div></div><div className="test-card"><div className="test-heading"><FlaskConical size={21} /><span>ENGINE TEST SUITE</span></div><strong>130</strong><p>passing unit tests</p><div className="test-tags"><span>kinematics</span><span>prediction</span><span>buffers</span><span>tolerance</span><span>configuration</span><span>registry</span><span>integration</span></div><a href={`${repo}/tree/main/snuffac-core/src/test`} target="_blank" rel="noreferrer">Inspect the test suite <ArrowUpRight size={14} /></a></div></div></section>

      <section className="research-section" id="research"><div className="shell research-grid"><div><SectionLabel index="07">Research and independence</SectionLabel><h2>Studied, understood,<br /><em>rebuilt independently.</em></h2><p>Snuff AC was developed by studying existing anticheat architecture, detection concepts, threading models, and failure modes. It was then written from scratch. No source code, comments, documentation, class names, configuration layouts, strings, or branding were copied.</p><a className="text-link" href={credits} target="_blank" rel="noreferrer">Read the full research credits <ArrowUpRight size={15} /></a></div><div className="research-list">{['GrimAC', 'Windfall AntiCheat', 'Updated-NoCheatPlus', 'AntiHaxerman', 'Medusa-Lite', 'Iris', 'Hawk'].map((name, index) => <div key={name}><span>0{index + 1}</span><b>{name}</b><small>architecture reference</small></div>)}<div className="research-foot">12 projects documented in credits.md<br />Licences read before research use</div></div></div></section>

      <section className="section shell honesty"><SectionLabel index="08">The server-side boundary</SectionLabel><div className="section-heading"><h2>Detect what you can<br /><em>actually observe.</em></h2><p>A server cannot see everything happening on a player’s client. Snuff focuses on resulting server-observable behavior, not on pretending the client is transparent.</p></div><div className="honesty-grid"><article><span>01 / DIRECT</span><h3>Packets and state</h3><p>Movement, attacks, rotations, interactions, timing, and invalid protocol behavior can be evaluated from server inputs.</p></article><article><span>02 / PARTIAL</span><h3>Information abuse</h3><p>Reach, targeting, Scaffold, and hidden information require behavioral evidence and server-side validation.</p></article><article><span>03 / OUTSIDE VIEW</span><h3>Purely client-side changes</h3><p>X-Ray, ESP, Freecam, and screen-only changes cannot be directly seen. Reduce unnecessary information exposure and validate what the server owns.</p></article></div></section>

      <section className="platform-section"><div className="shell"><SectionLabel index="09">Platform support</SectionLabel><div className="platform-grid"><div className="platform-card primary"><Server size={20} /><span>PRIMARY TARGET</span><h3>Paper 1.21.11</h3><p>Full check set. Tested end to end on Paper build 132 with Java 21.</p><b>TESTED</b></div><div className="platform-card"><Layers3 size={20} /><span>COMPATIBLE</span><h3>Purpur</h3><p>Uses the same build with runtime platform detection. No unnecessary Purpur-specific implementation.</p><b>COMPATIBLE</b></div><div className="platform-card limited"><Network size={20} /><span>LIMITED</span><h3>Velocity 3.4.0</h3><p>Packet and network timing checks only. A proxy has no world data or player position API, so world and combat geometry checks are inactive.</p><b>PACKET LEVEL</b></div></div></div></section>

      <section className="section shell limits-section"><SectionLabel index="09">Honest limitations</SectionLabel><div className="section-heading"><h2>What this<br /><em>cannot do.</em></h2><p>An anticheat that claims to detect everything is lying. These are the known blind spots, stated plainly rather than filled with placeholder checks.</p></div><div className="limits-list">{[['X-Ray, ESP, StorageESP, ore search','Not detectable. These are render time predicates over block data a vanilla client already receives. There is no protocol signal to find.'],['Fullbright and other local rendering','Not detectable. Nothing leaves the client, so nothing arrives at the server.'],['ViaVersion and Geyser movement','Not yet modelled. Bedrock and legacy protocol clients move differently, so both are exempted rather than risk false positives.'],['Collision and skipped tick prediction','Not yet simulated. Speed related thresholds stay untuned until the predictor models post 1.8.2 skipped ticks and collision resolution.']].map(([title, copy]) => <div className="limit-row" key={title}><span>NOT DETECTABLE</span><b>{title}</b><p>{copy}</p></div>)}</div></section>

      <section className="section shell roadmap"><div className="roadmap-head"><div><SectionLabel index="10">Development status</SectionLabel><h2>Useful now.<br /><em>Honest about what’s next.</em></h2></div><div className="release-stamp"><span>DEVELOPMENT RELEASE</span><b>1.0.1-dev</b><small>not recommended for production public servers</small></div></div><div className="roadmap-list">{['Collision and skipped tick prediction', 'Per-block tool tier accuracy', 'Recorded movement traces as regression fixtures', 'ViaVersion and Geyser movement modelling', 'Expanded Velocity capability', 'Automatic punishments after further validation'].map((item, index) => <div key={item}><span>{String(index + 1).padStart(2, '0')}</span><p>{item}</p><small>{index === 5 ? 'future enforcement' : 'planned work'}</small></div>)}</div><div className="roadmap-note"><CircleAlert size={17} /> No automatic bans ship in this release. Alerts, logging, and confidence gated prevention come first while detection is validated further.</div></section>

      <section className="cta-section"><div className="shell cta"><div className="cta-mark"><img src="/fav-icon.png" alt="" /></div><h2>Build your server<br /><em>on evidence.</em></h2><p>Open source means you can inspect how it works.</p><div className="hero-actions"><a className="button button-primary" href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src="/social/github_icon.png" alt="" /> View on GitHub <ArrowUpRight size={15} /></a><a className="button button-kofi" href="https://ko-fi.com/A1A3259HI2" target="_blank" rel="noreferrer"><img className="kofi-icon" src="/social/kofi_icon.png" alt="" /> Support me on Ko-fi <ArrowUpRight size={15} /></a><a className="button button-quiet" href={docs} target="_blank" rel="noreferrer"><BookOpen size={17} /> Documentation</a></div></div></section>
    </main>
    <footer className="footer shell"><div><a href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src="/social/github_icon.png" alt="" /> GitHub</a><a href={credits} target="_blank" rel="noreferrer">Credits</a><a href={`${repo}/blob/main/CHANGELOG.md`} target="_blank" rel="noreferrer">Changelog</a><a href={`${repo}/blob/main/LICENSE`} target="_blank" rel="noreferrer">GPL-3.0</a></div></footer>
  </div>
}

export default App

createRoot(document.getElementById('root')!).render(<App />)
