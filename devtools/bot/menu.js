// Opens every menu and clicks what is actually there, so a moved button is
// reported rather than silently skipped. bot.clickWindow is the API that sends
// a real click; window.clickSlot does nothing at all.
const mineflayer = require('mineflayer')

const bot = mineflayer.createBot({
  host: '127.0.0.1', port: 25565, username: 'Tester', auth: 'offline',
  version: '1.21.11', checkTimeoutInterval: 120000
})

const panes = new Set([
  'black_stained_glass_pane', 'gray_stained_glass_pane', 'red_stained_glass_pane',
  'light_blue_stained_glass_pane', 'purple_stained_glass_pane', 'white_stained_glass_pane',
  'yellow_stained_glass_pane', 'lime_stained_glass_pane', 'cyan_stained_glass_pane'
])

const report = []
const seen = []
let window = null
let opener = () => {}

const title = (w) => {
  try { return typeof w.title === 'string' ? w.title : (w.title && w.title.value) || 'untitled' } catch (e) { return 'unreadable' }
}

bot.on('error', (e) => { report.push('ERROR ' + e.message); console.log('ERROR ' + e.message) })
bot.on('message', (m) => {
  const text = m.toString().replace(/\n/g, ' | ')
  if (text.startsWith('[SNUFF]')) { report.push('CHAT: ' + text); console.log('CHAT: ' + text) }
})
bot.on('windowClose', () => {
  window = null
})
bot.on('windowOpen', (w) => {
  window = w
  const name = title(w)
  seen.push(name)
  const real = []
  w.slots.forEach((item, slot) => {
    if (item && !panes.has(item.name) && slot < w.inventoryStart) real.push([slot, item.name])
  })
  report.push(`WINDOW ${name} buttons=${JSON.stringify(real)}`)
  console.log(`WINDOW ${name}`)
  real.forEach(([slot, item]) => console.log(`   [${slot}] ${item}`))
})

const click = (slot) => {
  if (!window) { report.push('MISS nothing is open'); return }
  const item = window.slots[slot]
  if (!item) { report.push(`MISS ${slot} is empty`); return }
  report.push(`CLICK [${slot}] ${item.name}`)
  console.log(`CLICK [${slot}] ${item.name}`)
  try {
    bot.clickWindow(slot, 0, 0)
  } catch (failure) {
    // the server can close the window while a click is being built
    report.push(`click failed: ${failure.message}`)
    window = null
  }
}

// Walk the main menu by name, so a moved button is found rather than missed.
const find = (predicate) => {
  if (!window) return -1
  for (let slot = 0; slot < window.inventoryStart; slot++) {
    const item = window.slots[slot]
    if (item && !panes.has(item.name) && predicate(item, slot)) return slot
  }
  return -1
}
const byName = (word) => find((item) => item.name.includes(word))

const steps = [
  { at: 2500, do: () => { opener = () => bot.chat('/snuff menu'); opener() } },
  { at: 5000, do: () => click(byName('player_head')) },
  // on a single page Previous is a no op, so go straight back to the main menu
  { at: 7000, do: () => click(byName('oak_door')) },
  { at: 9000, do: () => click(byName('writable_book')) },
  { at: 11000, do: () => click(byName('oak_door')) },
  { at: 13000, do: () => click(byName('bell')) },
  { at: 14500, do: () => click(byName('comparator')) },
  { at: 17000, do: () => click(byName('redstone')) },
  { at: 18500, do: () => click(byName('oak_door')) },
  { at: 20000, do: () => click(byName('barrier')) },
  { at: 23000, do: () => { opener = () => bot.chat('/snuff reports'); opener() } },
  { at: 25500, do: () => click(byName('arrow')) },
  { at: 27500, do: () => { opener = () => bot.chat('/snuff menu'); opener() } },
  { at: 29500, do: () => click(byName('book')) },
  { at: 32000, do: () => click(byName('oak_door')) },
  { at: 33800, do: () => click(byName('barrier')) }
]

bot.once('spawn', () => {
  console.log('spawned')
  for (const step of steps) setTimeout(() => { try { step.do() } catch (e) { report.push('STEP FAIL ' + e.message) } }, step.at)
  setTimeout(() => {
    report.push('WINDOWS SEEN: ' + JSON.stringify(seen))
    console.log('WINDOWS SEEN: ' + JSON.stringify(seen))
    require('fs').writeFileSync('/tmp/opencode/bot/menu.json', JSON.stringify(report, null, 2))
    bot.quit()
    process.exit(0)
  }, 35000)
})

setTimeout(() => process.exit(2), 60000)
