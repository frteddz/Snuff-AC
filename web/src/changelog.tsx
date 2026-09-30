// The changelog lives on its own page, generated from CHANGELOG.md by
// sync-site-changelog.py so the two can never disagree about a release.
import { useState } from 'react'
import { createRoot } from 'react-dom/client'
import { ArrowUpRight, Menu, X } from 'lucide-react'
import { releases } from './releases'
import './styles.css'

const repo = 'https://github.com/frteddz/Snuff-AC'
const credits = `${repo}/blob/main/credits.md`

function SectionLabel({ index, children }: { index: string; children: string }) {
  return <div className="section-label"><span>{index}</span><i />{children}</div>
}

function LatestRelease() {
  const [release] = releases
  return (
    <header className="page-head">
      <SectionLabel index="01">Changelog</SectionLabel>
      <div className="section-heading">
        <h1>Every release,<br /><em>what actually changed.</em></h1>
        <p>Newest first, generated straight from <a href={`${repo}/blob/main/CHANGELOG.md`} target="_blank" rel="noreferrer">CHANGELOG.md</a> so this page and the file can never disagree. Each entry names what was broken and why, and where a fix was confirmed against a real server rather than only asserted in a test.</p>
      </div>
      <div className="latest-strip">
        <div><span>LATEST</span><b>{release.version}</b></div>
        <div><span>RELEASED</span><b>{release.stamp}</b></div>
        <div><span>ENTRIES</span><b>{release.items.length}</b></div>
        <a className="button button-primary" href={`${repo}/releases/tag/v${release.version}`} target="_blank" rel="noreferrer">Download it <ArrowUpRight size={15} /></a>
      </div>
    </header>
  )
}

function Changelog() {
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = () => setMenuOpen(false)

  return <div id="top">
    <header className="nav-wrap">
      <nav className="nav shell" aria-label="Main navigation">
        <button className="menu-button" onClick={() => setMenuOpen(!menuOpen)} aria-expanded={menuOpen} aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}>{menuOpen ? <X size={20} /> : <Menu size={20} />}</button>
        <div className={`nav-links ${menuOpen ? 'open' : ''}`}>
          <a href="/index.html" onClick={closeMenu}>Home</a>
          <a href="/index.html#method" onClick={closeMenu}>Method</a>
          <a href="/index.html#checks" onClick={closeMenu}>Checks</a>
          <a href="/index.html#architecture" onClick={closeMenu}>Architecture</a>
          <a className="nav-github" href={repo} target="_blank" rel="noreferrer" onClick={closeMenu}><img className="social-icon" src="/social/github_icon.png" alt="" /> GitHub <ArrowUpRight size={14} /></a>
        </div>
      </nav>
    </header>

    <main className="changelog-page">
      <LatestRelease />

      <section className="section shell">
        <div className="changelog-list">{releases.map((release) => <article className="changelog-entry" key={release.version}>
          <header>
            <h2>{release.version}</h2>
            <span className="changelog-stamp">{release.stamp}</span>
            <a href={`${repo}/releases/tag/v${release.version}`} target="_blank" rel="noreferrer">Release <ArrowUpRight size={13} /></a>
          </header>
          <ul>{release.items.map((item) => <li key={item.title}><b>{item.title}</b><p>{item.copy}</p></li>)}</ul>
        </article>)}</div>
      </section>
    </main>

    <footer className="footer shell"><div><a href="/index.html">Home</a><a href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src="/social/github_icon.png" alt="" /> GitHub</a><a href={credits} target="_blank" rel="noreferrer">Credits</a><a href={`${repo}/blob/main/LICENSE`} target="_blank" rel="noreferrer">GPL-3.0</a></div></footer>
  </div>
}

export default Changelog

createRoot(document.getElementById('root')!).render(<Changelog />)
