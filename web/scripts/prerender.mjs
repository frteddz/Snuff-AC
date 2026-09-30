// Pre-renders both pages to real HTML at build time.
//
// The app is a React SPA, so without this a crawler receives an empty <div>
// and has to run JavaScript to see anything. Google does render JS, but later
// and less reliably, and most other crawlers never do at all. Rendering the
// same components to a string gives every crawler the full text in the
// response, with the JavaScript left in place for the interactive site.
import { renderToString } from 'react-dom/server'
import { build } from 'vite'
import react from '@vitejs/plugin-react'
import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const gradle = resolve(root, '..', 'gradle.properties')
const ORIGIN = 'https://frteddz.github.io/Snuff-AC'
const BASE = '/Snuff-AC/' 

// both entry files call createRoot at module scope, which needs a real DOM.
// The call is stripped from the bundle before it is imported.
const MOUNT = /createRoot\([^;]*?\)\s*(?:#|\/\*[^\n]*\*\/)?[;\n]/

const PAGES = [
  { entry: 'index.html', out: 'index.html', module: '/src/main.tsx', component: 'App' },
  { entry: 'changelog.html', out: 'changelog.html', module: '/src/changelog.tsx', component: 'Changelog' },
]

const render = async (page) => {
  const vite = await build({
    root,
    logLevel: 'error',
    plugins: [react()],
    build: {
      write: false,
      ssr: page.module,
      rollupOptions: { output: { format: 'esm', entryFileNames: `${page.out}.mjs` } },
    },
  })
  const output = Array.isArray(vite) ? vite[0].output : vite.output
  const chunk = output.find((item) => item.type === 'chunk')
  if (!chunk) {
    throw new Error(`no server bundle produced for ${page.module}`)
  }
  // the bundle is written as <page>.mjs, so the scratch file has to keep that
  // extension or node refuses to import it
  const scratch = resolve(root, 'node_modules', '.prerender', `${page.out}.mjs`)
  const stripped = chunk.code.replace(MOUNT, '')
  if (stripped === chunk.code) {
    throw new Error(`could not find the mount call in ${page.module}, prerender would be empty`)
  }
  await writeFile(scratch, stripped, 'utf8')
  const module = await import(`${scratch}?v=${Date.now()}`)
  return module[page.component] ?? module.default
}

const version = async () => {
  const properties = await readFile(gradle, 'utf8')
  return properties.match(/version=([0-9.]+(?:-dev)?)/)?.[1] ?? '0.0.0-dev'
}

const main = async () => {
  for (const page of PAGES) {
    const component = await render(page)
    if (typeof component !== 'function') {
      throw new Error(`${page.module} did not export ${page.component}`)
    }
    const { default: React } = await import('react')
    const markup = renderToString(React.createElement(component))
    const current = await version()

    const file = resolve(root, 'dist', page.out)
    let html = await readFile(file, 'utf8')
    // keep whatever the page's own <head> already declares, and add the body
    html = html.replace('<div id="root"></div>', `<div id="root">${markup}</div>`)

    // an empty or relative canonical is worse than none at all
    html = html.replace(
      '<link rel="canonical" href="/" />',
      `<link rel="canonical" href="${ORIGIN}/" />`
    )

    // vite rewrites urls it can see in html, but the head of these pages is
    // hand written, so a root relative path here would 404 on a project
    // pages subpath
    html = html.replace(/(href|src|poster)="\/(?!Snuff-AC\/|\/\/)/g, `$1="${BASE}`)

    if (!html.includes('as="font"')) {
      html = html.replace(
        '<link rel="icon"',
        `<link rel="preload" as="font" type="font/woff2" href="${ORIGIN}/fonts/space-grotesk-500.woff2" crossorigin />\n    <link rel="icon"`
      )
    }
    // the softwareVersion in the structured data has to match the jar
    html = html.replace(/"softwareVersion": "[^"]*"/, `"softwareVersion": "${current}"`)
    await writeFile(file, html, 'utf8')
    const text = markup.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
    console.log(`prerendered ${page.out}: ${markup.length} bytes of html, ${text.length} characters of text`)
  }
}

await main()
