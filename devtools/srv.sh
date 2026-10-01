#!/usr/bin/env bash
# Controls a local Paper server for verification. Never blocks on the server.
# Usage:
#   ./srv.sh setup                 download Paper, accept the eula, write server.properties
#   ./srv.sh start | stop | restart| status
#   ./srv.sh cmd "say hi"          send a console command
#   ./srv.sh logs [lines]
set -uo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
DIR="${SNUFFAC_TEST_SERVER:-/tmp/opencode/testserver}"
BUILD_VERSION="1.21.11"
BUILD_NUMBER="132"
# the server jar is kept beside this script rather than in the world writable
# temp directory, because /tmp has been wiped mid session and took the server
# with it. the copy in the server directory is a link, not the original.
CACHE="$HERE/.paper"
JAR="$CACHE/paper-$BUILD_VERSION-$BUILD_NUMBER.jar"
LINKED="$DIR/paper.jar"
FILL_URL="https://fill.papermc.io/v3/projects/paper/versions/$BUILD_VERSION/builds/$BUILD_NUMBER"
MEM_OPTS="-Xms1G -Xmx1500M -XX:+UseG1GC"

download_paper() {
  mkdir -p "$CACHE"
  if [ -f "$JAR" ]; then
    echo "paper already cached at $JAR"
    return 0
  fi
  local url=""
  url=$(curl -fsSL --max-time 60 "$FILL_URL" | python3 -c '
import json, sys
print(json.load(sys.stdin)["downloads"]["server:default"]["url"])
' 2>/dev/null) && [ -n "$url" ] || url=""
  if [ -z "$url" ]; then
    url="https://api.papermc.io/v2/projects/paper/versions/$BUILD_VERSION/builds/$BUILD_NUMBER/downloads/paper-$BUILD_VERSION-$BUILD_NUMBER.jar"
  fi
  echo "downloading $url"
  local attempt=1
  while [ "$attempt" -le 5 ]; do
    if curl -fsSL --max-time 300 "$url" -o "$JAR.part"; then
      mv "$JAR.part" "$JAR"
      echo "paper cached"
      return 0
    fi
    rm -f "$JAR.part"
    echo "download attempt $attempt failed, retrying" >&2
    attempt=$((attempt + 1))
    sleep $((attempt * 5))
  done
  echo "could not download paper $BUILD_VERSION build $BUILD_NUMBER" >&2
  return 1
}

running() {
  [ -f "$DIR/server.pid" ] && kill -0 "$(cat "$DIR/server.pid")" 2>/dev/null
}

setup() {
  mkdir -p "$DIR"
  download_paper || exit 1
  [ -f "$LINKED" ] || cp "$JAR" "$LINKED"
  cat > "$DIR/eula.txt" <<EULA
eula=true
EULA
  # offline mode so the bot clients can join with any name, and no port conflicts
  cat > "$DIR/server.properties" <<'PROPS'
online-mode=false
server-port=25565
level-type=minecraft\:flat
spawn-protection=0
max-players=20
view-distance=4
simulation-distance=4
sync-chunk-writes=false
enable-rcon=false
motd=snuffac dev
allow-nether=true
enable-command-block=false
gamemode=survival
difficulty=peaceful
PROPS
  mkdir -p "$DIR/plugins"
  echo "ready in $DIR"
}

start() {
  if running; then
    echo "already running, pid $(cat "$DIR/server.pid")"
    return 0
  fi
  cd "$DIR" || exit 1
  rm -f console.in
  mkfifo console.in
  # hold the fifo open so the server does not see end of input and exit
  setsid sleep infinity > console.in 2>/dev/null &
  echo $! > fifo.pid
  # fully detached, every descriptor redirected, so the caller can exit at once
  setsid java $MEM_OPTS -jar paper.jar --nogui < console.in > server.log 2>&1 &
  echo $! > server.pid
  disown 2>/dev/null || true
  echo "started, pid $(cat server.pid), log $DIR/server.log"
}

stop() {
  if [ -p "$DIR/console.in" ]; then
    printf 'stop\n' > "$DIR/console.in" 2>/dev/null
  fi
  for _ in $(seq 1 40); do
    running || break
    sleep 0.5
  done
  if running; then
    kill "$(cat "$DIR/server.pid")" 2>/dev/null
  fi
  [ -f "$DIR/fifo.pid" ] && kill "$(cat "$DIR/fifo.pid")" 2>/dev/null
  rm -f "$DIR/console.in" "$DIR/fifo.pid" "$DIR/server.pid"
  echo stopped
}

cmd() {
  if [ ! -p "$DIR/console.in" ]; then
    echo "server is not running" >&2
    exit 1
  fi
  printf '%s\n' "$*" > "$DIR/console.in"
}

wait_ready() {
  for _ in $(seq 1 120); do
    if grep -aq 'Done (' "$DIR/server.log" 2>/dev/null; then
      return 0
    fi
    sleep 1
  done
  echo "server did not finish starting" >&2
  return 1
}

case "${1:-}" in
  setup) setup ;;
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  cmd) shift; cmd "$@" ;;
  wait) wait_ready && echo ready ;;
  status)
    if running; then
      echo "running, pid $(cat "$DIR/server.pid")"
    else
      echo "not running"
    fi
    ;;
  logs) tail -n "${2:-40}" "$DIR/server.log" | sed 's/\x1b\[[0-9;]*m//g' ;;
  jar) echo "$JAR" ;;
  dir) echo "$DIR" ;;
  *)
    echo "usage: $0 {setup|start|stop|restart|cmd|wait|status|logs|jar|dir}" >&2
    exit 1
    ;;
esac
