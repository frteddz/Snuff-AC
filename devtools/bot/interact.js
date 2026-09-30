const mineflayer = require('mineflayer')

const seconds = Number(process.argv[2] || 25)
const bot = mineflayer.createBot({
  host: '127.0.0.1',
  port: 25565,
  username: process.env.SNUFFAC_BOT_NAME || 'Tester',
  auth: 'offline',
  version: '1.21.11',
  checkTimeoutInterval: 120000
})

const out = []
const note = (line) => {
  out.push(line)
  console.log(line)
  require('fs').writeFileSync('/tmp/opencode/bot/last-interact.txt', out.join('\n'))
}

let placed = 0
let digFailed = 0
let dug = 0
let acted = 0

bot.on('error', (error) => note(`ERROR ${error.message}`))
bot.on('kicked', (reason) => note(`KICKED ${JSON.stringify(reason)}`))
bot.on('end', (reason) => note(`DISCONNECTED after ${acted} actions, ${reason}`))

bot.once('spawn', () => {
  note('spawned')
  note(`pos ${bot.entity.position.toString()}`)

  const cycle = async () => {
    acted++
    const feet = bot.entity.position
    const below = bot.blockAt(feet.offset(0, -1, 0))
    if (!below || below.name === 'air') {
      note('no ground below, waiting')
      return
    }
    bot.lookAt(below.position.offset(0.5, 0.5, 0.5), true)
    await new Promise((resolve) => setTimeout(resolve, 220))
    try {
      const shovel = bot.inventory.items().find((item) => item.name.includes('shovel'))
      if (shovel) {
        await bot.equip(shovel, 'hand')
      }
    } catch (error) {
      note(`equip refused: ${error.message}`)
    }
    try {
      await bot.dig(below)
      dug++
    } catch (error) {
      digFailed++
      if (digFailed < 3) {
        note(`dig refused: ${error.message}`)
      }
    }
  }

  const timer = setInterval(() => {
    cycle().catch((error) => note(`cycle error: ${error.message}`))
  }, 500)

  setTimeout(() => {
    clearInterval(timer)
    note(`summary dug=${dug} digFailed=${digFailed} actions=${acted}`)
    bot.quit()
    process.exit(0)
  }, seconds * 1000)
})
