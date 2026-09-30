// A legitimate vanilla client that plays normally, so the anticheat can be
// checked for false positives. Nothing here cheats.
const mineflayer = require('mineflayer')

const scenario = process.argv[2] || 'idle'
const seconds = Number(process.argv[3] || 30)

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
  require('fs').writeFileSync(`/tmp/opencode/bot/last-${scenario}.txt`, out.join('\n'))
}

let ticks = 0

bot.on('error', (error) => note(`ERROR ${error.message}`))
bot.on('kicked', (reason) => note(`KICKED ${JSON.stringify(reason)}`))
bot.on('end', (reason) => note(`DISCONNECTED after ${ticks} ticks, ${reason}`))
bot.on('message', (message) => {
  const text = message.toString()
  if (text.startsWith('[SNUFF]') || text.includes('Welcome')) {
    note(`CHAT: ${text.replace(/\n/g, ' | ')}`)
  }
})

// mineflayer has no bot.jump, a jump is the jump control held for one tick
const doJump = (b) => {
  b.setControlState('jump', true)
  setTimeout(() => b.setControlState('jump', false), 60)
}

const scenarios = {
  idle: () => {},
  walk: (b) => b.setControlState('forward', true),
  jump: (b) => {
    if (ticks % 20 === 0) doJump(b)
  },
  sprintjump: (b) => {
    if (ticks % 20 === 0) {
      b.setControlState('sprint', true)
      b.setControlState('forward', true)
      doJump(b)
    }
  },
  strafe: (b) => {
    b.setControlState('forward', true)
    b.setControlState('left', ticks % 40 < 20)
    b.setControlState('right', ticks % 40 >= 20)
    b.look(Math.sin(ticks / 30) * 1.4, 0, true)
  },
  look: (b) => {
    b.look(Math.sin(ticks / 25) * 2.2, Math.sin(ticks / 17) * 0.6, true)
  },
  crouch: (b) => {
    b.setControlState('sneak', ticks % 30 < 15)
    b.setControlState('forward', true)
  },
  jumpstop: (b) => {
    if (ticks % 20 === 0) {
      b.setControlState('forward', true)
      doJump(b)
    }
    if (ticks % 20 === 10) b.setControlState('forward', false)
  }
}

bot.once('spawn', () => {
  note(`spawned, running scenario=${scenario} for ${seconds}s`)
  const play = scenarios[scenario]
  if (!play) {
    note(`unknown scenario ${scenario}`)
    bot.quit()
    return
  }
  const timer = setInterval(() => {
    ticks++
    play(bot)
    if (ticks >= seconds * 20) {
      clearInterval(timer)
      note(`scenario complete, ${ticks} ticks elapsed`)
      setTimeout(() => bot.quit(), 500)
    }
  }, 50)
})

setTimeout(() => {
  note(`timeout after ${seconds}s`)
  process.exit(2)
}, (seconds + 30) * 1000)
