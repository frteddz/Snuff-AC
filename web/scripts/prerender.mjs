

import { renderToString } from 'react-dom/server'
import { build } from 'vite'
import react from '@vitejs/plugin-react'
import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const gradle = resolve(root, '..', 'gradle.properties')
const ORIGIN = 'https://frteddz.github.io/Snuff-AC'
const BASE = '/Snuff-AC/'

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

    html = html.replace('<div id="root"></div>', `<div id="root">${markup}</div>`)

    html = html.replace(
      '<link rel="canonical" href="/" />',
      `<link rel="canonical" href="${ORIGIN}/" />`
    )

    html = html.replace(/(href|src|poster)="\/(?!Snuff-AC\/|\/\/)/g, `$1="${BASE}`)

    if (!html.includes('as="font"')) {
      html = html.replace(
        '<link rel="icon"',
        `<link rel="preload" as="font" type="font/woff2" href="${ORIGIN}/fonts/space-grotesk-500.woff2" crossorigin />\n    <link rel="icon"`
      )
    }
    html = html.replace(/"softwareVersion": "[^"]*"/, `"softwareVersion": "${current}"`)
    await writeFile(file, html, 'utf8')
    const text = markup.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
    console.log(`prerendered ${page.out}: ${markup.length} bytes of html, ${text.length} characters of text`)
  }
}

await main()
