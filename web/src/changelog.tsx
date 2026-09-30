

import { useEffect, useMemo, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { ArrowUpRight, Menu, X } from 'lucide-react'
import { releases } from './releases'
import './styles.css'

const base = import.meta.env.BASE_URL
const asset = (path: string) => `${base}${path.replace(/^\//, '')}`

const repo = 'https://github.com/frteddz/Snuff-AC'
const credits = `${repo}/blob/main/credits.md`

function totals() {
  return releases.reduce(
    (sum, release) => ({
      entries: sum.entries + release.count,
      grouped: sum.grouped + release.items.filter((item) => item.group).length,
    }),
    { entries: 0, grouped: 0 }
  )
}

function SectionLabel({ index, children }: { index: string; children: string }) {
  return <div className="section-label"><span>{index}</span><i />{children}</div>
}

function ReleaseNav({ active, onPick }: { active: string; onPick: (tag: string) => void }) {
  return (
    <nav className="release-nav" aria-label="Releases">
      <span className="release-nav-title">RELEASES</span>
      <ol>
        {[...releases].reverse().map((release) => (
          <li key={release.tag}>
            <a
              href={`#${release.tag}`}
              className={active === release.tag ? 'active' : ''}
              onClick={(event) => { event.preventDefault(); onPick(release.tag) }}
            >
              <b>{release.version}</b>
              <small>{release.count} {release.count === 1 ? 'change' : 'changes'}</small>
            </a>
          </li>
        ))}
      </ol>
    </nav>
  )
}

function Release({ release }: { release: typeof releases[number] }) {
  const groups: { name: string | null; items: typeof release.items }[] = []
  for (const item of release.items) {
    const last = groups[groups.length - 1]
    if (last && last.name === item.group) {
      last.items.push(item)
    } else {
      groups.push({ name: item.group, items: [item] })
    }
  }

  return (
    <article className="release" id={release.tag}>
      <header className="release-head">
        <div className="release-title">
          <h2>{release.version}</h2>
          {release.pre ? <span className="release-badge">PRE-RELEASE</span> : null}
        </div>
        <div className="release-meta">
          <span className="release-date">{release.stamp}</span>
          <span className="release-count">{release.count} {release.count === 1 ? 'change' : 'changes'}</span>
          <a href={`${repo}/releases/tag/${release.tag}`} target="_blank" rel="noreferrer">
            v{release.version} on GitHub <ArrowUpRight size={13} />
          </a>
        </div>
      </header>

      {groups.map((group, index) => (
        <div className="release-group" key={`${release.tag}-${index}`}>
          {group.name ? <h3>{group.name}</h3> : null}
          <ul>
            {group.items.map((item, itemIndex) => (
              <li key={`${item.title}-${itemIndex}`}>
                <b>{item.title}</b>
                {item.copy ? <p>{item.copy}</p> : null}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </article>
  )
}

function Changelog() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [active, setActive] = useState(releases[releases.length - 1].tag)
  const closeMenu = () => setMenuOpen(false)
  const latest = releases[releases.length - 1]
  const counts = useMemo(totals, [])
  const areas = useMemo(() => {
    const names = new Set<string>()
    for (const release of releases) {
      for (const item of release.items) {
        if (item.group) {
          names.add(item.group)
        }
      }
    }
    return names.size
  }, [])

  const jump = (tag: string) => {
    setActive(tag)
    const target = document.getElementById(tag)
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }

  useEffect(() => {
    const onScroll = () => {
      let current = releases[0].tag
      for (const release of releases) {
        const element = document.getElementById(release.tag)
        if (element && element.getBoundingClientRect().top <= 160) {
          current = release.tag
        }
      }
      setActive(current)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return <div id="top">
    <header className="nav-wrap">
      <nav className="nav shell" aria-label="Main navigation">
        <button className="menu-button" onClick={() => setMenuOpen(!menuOpen)} aria-expanded={menuOpen} aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}>{menuOpen ? <X size={20} /> : <Menu size={20} />}</button>
        <div className={`nav-links ${menuOpen ? 'open' : ''}`}>
          <a href={`${base}index.html`} onClick={closeMenu}>Home</a>
          <a href={`${base}index.html#method`} onClick={closeMenu}>Method</a>
          <a href={`${base}index.html#checks`} onClick={closeMenu}>Checks</a>
          <a href={`${base}index.html#architecture`} onClick={closeMenu}>Architecture</a>
          <a className="nav-github" href={repo} target="_blank" rel="noreferrer" onClick={closeMenu}><img className="social-icon" src={asset('social/github_icon.png')} alt="" /> GitHub <ArrowUpRight size={14} /></a>
        </div>
      </nav>
    </header>

    <main className="changelog-page">
      <header className="page-head shell">
        <SectionLabel index="01">Changelog</SectionLabel>
        <div className="section-heading">
          <h1>Every release,<br /><em>what actually changed.</em></h1>
          <p>
            Oldest first, generated straight from{' '}
            <a href={`${repo}/blob/main/CHANGELOG.md`} target="_blank" rel="noreferrer">CHANGELOG.md</a>{' '}
            so this page and the file can never disagree. Each entry names what was broken and
            why. Where a fix was confirmed against a real server with real client connections
            rather than only asserted in a test, it says so.
          </p>
        </div>
        <div className="page-stats">
          <div><span>RELEASES</span><b>{releases.length}</b></div>
          <div><span>CHANGES</span><b>{counts.entries}</b></div>
          <div><span>AREAS</span><b>{areas}</b></div>
          <div><span>LATEST</span><b>{latest.version}</b></div>
          <a className="button button-primary" href={`${repo}/releases/tag/${latest.tag}`} target="_blank" rel="noreferrer">Latest release <ArrowUpRight size={15} /></a>
        </div>
      </header>

      <div className="release-body shell">
        <aside><ReleaseNav active={active} onPick={jump} /></aside>
        <div className="release-stream">
          {releases.map((release) => <Release key={release.tag} release={release} />)}
        </div>
      </div>
    </main>

    <footer className="footer shell"><div><a href={`${base}index.html`}>Home</a><a href={repo} target="_blank" rel="noreferrer"><img className="social-icon" src={asset('social/github_icon.png')} alt="" /> GitHub</a><a href={credits} target="_blank" rel="noreferrer">Credits</a><a href={`${repo}/blob/main/LICENSE`} target="_blank" rel="noreferrer">GPL-3.0</a></div></footer>
  </div>
}

export default Changelog

createRoot(document.getElementById('root')!).render(<Changelog />)
