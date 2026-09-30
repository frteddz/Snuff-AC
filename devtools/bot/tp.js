
const mineflayer = require('mineflayer')
const make = (name) => mineflayer.createBot({
  host: '127.0.0.1', port: 25565, username: name, auth: 'offline',
  version: '1.21.11', checkTimeoutInterval: 120000
})
const a = make('Tester')
let b = null
a.on('error', (e) => console.log('A ERROR ' + e.message))
a.on('message', (m) => console.log('A CHAT: ' + m.toString().replace(/\n/g, ' | ')))
a.once('spawn', () => {
  setTimeout(() => { b = make('Tester2'); b.on('error', (e) => console.log('B ERROR ' + e.message)) }, 3000)
  setTimeout(() => a.chat('/snuff tp'), 9000)
  setTimeout(() => a.chat('/snuff tp Tester2'), 13000)
  setTimeout(() => a.chat('/snuff bypass Tester2'), 17000)
  setTimeout(() => a.chat('/snuff bypass Tester2 off'), 21000)
  setTimeout(() => { a.quit(); b && b.quit(); process.exit(0) }, 25000)
})
setTimeout(() => process.exit(2), 45000)
