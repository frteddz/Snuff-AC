// Opens every menu and clicks the buttons, with real client clicks.
// bot.clickWindow is the API that actually sends a click; window.clickSlot does nothing.
const mineflayer = require('mineflayer')

const bot = mineflayer.createBot({
  host: '127.0.0.1', port: 25565, username: 'Tester', auth: 'offline',
  version: '1.21.11', checkTimeoutInterval: 120000
})

const seen = []
const messages = []
let window = null

const title = (w) => {
  try { return typeof w.title === 'string' ? w.title : (w.title && w.title.value) || 'untitled' } catch (e) { return 'unreadable' }
}

bot.on('error', (e) => { console.log('ERROR ' + e.message) })
bot.on('message', (m) => {
  const text = m.toString().replace(/\n/g, ' | ')
  messages.push(text)
  if (text.startsWith('[SNUFF]')) console.log('CHAT: ' + text)
})
bot.on('windowOpen', (w) => {
  window = w
  const name = title(w)
  seen.push(name)
  console.log(`WINDOW ${name} items=${w.slots.filter(Boolean).length}`)
})

const click = (slot) => {
  if (!window) { console.log('no window open'); return }
  const item = window.slots[slot]
  if (!item) { console.log(`slot ${slot} is empty`); return }
  console.log(`CLICK [${slot}] ${item.name}`)
  bot.clickWindow(slot, 0, 0)
}

const steps = [
  { at: 2500, run: () => bot.chat('/snuff menu') },
  { at: 5000, run: () => click(10) },
  { at: 7500, run: () => click(45) },
  { at: 10000, run: () => click(12) },
  { at: 12500, run: () => click(45) },
  { at: 15000, run: () => click(14) },
  { at: 17500, run: () => click(15) },
  { at: 20000, run: () => click(22) },
  { at: 22500, run: () => click(22) },
  { at: 25000, run: () => bot.chat('/snuff reports') },
  { at: 27500, run: () => click(45) },
  { at: 30000, run: () => click(48) },
  { at: 33000, run: () => click(50) }
]

bot.once('spawn', () => {
  console.log('spawned')
  for (const step of steps) setTimeout(step.run, step.at)
  setTimeout(() => {
    console.log('WINDOWS SEEN: ' + JSON.stringify(seen))
    require('fs').writeFileSync('/tmp/opencode/bot/menu.json', JSON.stringify({ seen, messages }, null, 2))
    bot.quit()
    process.exit(0)
  }, 36000)
})

setTimeout(() => process.exit(2), 60000)
