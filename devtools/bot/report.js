// Files a report against a second player, then claims it as staff.
// Tester2 must already be connected.
const mineflayer = require('mineflayer')
const bot = mineflayer.createBot({
  host: '127.0.0.1', port: 25565, username: 'Tester', auth: 'offline',
  version: '1.21.11', checkTimeoutInterval: 120000
})
const lines = []
const note = (l) => { lines.push(l); console.log(l) }
const title = (w) => { try { return typeof w.title === 'string' ? w.title : (w.title && w.title.value) || 'untitled' } catch (e) { return 'unreadable' } }
let window = null
const titles = []
bot.on('error', (e) => note('ERROR ' + e.message))
bot.on('message', (m) => note('CHAT: ' + m.toString().replace(/\n/g, ' | ')))
bot.on('windowOpen', (w) => {
  window = w; titles.push(title(w))
  note('WINDOW ' + title(w) + ' items=' + w.slots.filter(Boolean).length)
})
const click = (slot) => {
  if (!window) { note('no window'); return }
  const item = window.slots[slot]
  if (!item) { note(`slot ${slot} empty`); return }
  note(`CLICK [${slot}] ${item.name}`)
  bot.clickWindow(slot, 0, 0)
}
bot.once('spawn', () => {
  note('spawned')
  setTimeout(() => bot.chat('/snuff report Tester2'), 2500)
  setTimeout(() => click(10), 6000)
  setTimeout(() => click(49), 9500)
  setTimeout(() => bot.chat('/snuff reports'), 13000)
  setTimeout(() => click(48), 17000)
  setTimeout(() => {
    note('WINDOWS: ' + JSON.stringify(titles))
    require('fs').writeFileSync('/tmp/opencode/bot/report.json', JSON.stringify(lines, null, 2))
    bot.quit(); process.exit(0)
  }, 21000)
})
setTimeout(() => process.exit(2), 60000)
