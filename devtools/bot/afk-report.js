// Selects the AFK category that was added to report-options.yml at runtime.
const mineflayer = require('mineflayer')
const bot = mineflayer.createBot({
  host: '127.0.0.1', port: 25565, username: 'Tester', auth: 'offline',
  version: '1.21.11', checkTimeoutInterval: 120000
})
const lines = []
const note = (l) => { lines.push(l); console.log(l) }
let window = null
const title = (w) => { try { return typeof w.title === 'string' ? w.title : (w.title && w.title.value) || 'untitled' } catch (e) { return 'unreadable' } }
bot.on('error', (e) => note('ERROR ' + e.message))
bot.on('message', (m) => note('CHAT: ' + m.toString().replace(/\n/g, ' | ')))
bot.on('windowOpen', (w) => {
  window = w
  note('WINDOW ' + title(w))
  w.slots.forEach((it, i) => { if (it && !it.name.includes('glass_pane') && i < w.inventoryStart) note('   [' + i + '] ' + it.name) })
})
const click = (slot) => { if (!window) return; note(`CLICK [${slot}] ${window.slots[slot] && window.slots[slot].name}`); bot.clickWindow(slot, 0, 0) }
bot.once('spawn', () => {
  setTimeout(() => bot.chat('/snuff report Tester2'), 2500)
  setTimeout(() => click(19), 6500)
  setTimeout(() => click(49), 10000)
  setTimeout(() => { require('fs').writeFileSync('/tmp/opencode/bot/afk.json', JSON.stringify(lines, null, 2)); bot.quit(); process.exit(0) }, 15000)
})
setTimeout(() => process.exit(2), 40000)
