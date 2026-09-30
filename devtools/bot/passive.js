
const mineflayer = require('mineflayer')
const seconds = Number(process.argv[2] || 60)
const bot = mineflayer.createBot({
  host: '127.0.0.1', port: 25565, username: process.env.SNUFFAC_BOT_NAME || 'Tester2',
  auth: 'offline', version: '1.21.11', checkTimeoutInterval: 120000
})
bot.on('error', (e) => console.log('ERROR ' + e.message))
bot.on('kicked', (r) => console.log('KICKED ' + JSON.stringify(r)))
bot.once('spawn', () => { console.log('spawned') })
setTimeout(() => { bot.quit(); process.exit(0) }, seconds * 1000)
