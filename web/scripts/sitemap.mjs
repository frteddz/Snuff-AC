// Builds sitemap.xml and feed.xml from the generated release data, so the
// sitemap can never list a page or a release that does not exist.
import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const ORIGIN = 'https://frteddz.github.io/Snuff-AC'

const data = await readFile(resolve(root, 'src', 'releases.ts'), 'utf8')
const versions = [...data.matchAll(/version: '([^']+)'/g)].map((m) => m[1])
const stamps = [...data.matchAll(/stamp: '([^']+)'/g)].map((m) => m[1])
const today = new Date().toISOString().slice(0, 10)

const escape = (value) => value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

const pages = [
  { loc: `${ORIGIN}/`, priority: '1.0', changefreq: 'weekly', lastmod: today },
  { loc: `${ORIGIN}/changelog.html`, priority: '0.8', changefreq: 'weekly', lastmod: today },
]

const urls = pages
  .map((page) => `  <url>
    <loc>${escape(page.loc)}</loc>
    <lastmod>${page.lastmod}</lastmod>
    <changefreq>${page.changefreq}</changefreq>
    <priority>${page.priority}</priority>
  </url>`)
  .join('\n')

await writeFile(resolve(root, 'dist', 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>
`, 'utf8')

const items = versions
  .map((version, index) => `  <entry>
    <title>Snuff AC ${escape(version)}</title>
    <link href="${ORIGIN}/changelog.html#v${version}"/>
    <id>${ORIGIN}/changelog.html#v${version}</id>
    <updated>${stamps[index] ?? today}T00:00:00Z</updated>
    <summary type="text">Release ${escape(version)} of Snuff AC, an open source Minecraft anticheat.</summary>
  </entry>`)
  .join('\n')

await writeFile(resolve(root, 'dist', 'feed.xml'), `<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Snuff AC releases</title>
  <subtitle>Release history of Snuff AC, an open source Minecraft anticheat.</subtitle>
  <link href="${ORIGIN}/feed.xml" rel="self"/>
  <link href="${ORIGIN}/changelog.html"/>
  <id>${ORIGIN}/feed.xml</id>
  <updated>${today}T00:00:00Z</updated>
${items}
</feed>
`, 'utf8')

console.log(`wrote sitemap.xml (${pages.length} pages) and feed.xml (${versions.length} releases)`)
