#!/usr/bin/env bash
# Runs every legitimate movement scenario and reports which checks fired.
# A false positive here is a bug, so the output is the pass condition.
set -uo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
DEVTOOLS=$(cd "$HERE/.." && pwd)
LOG=$("$DEVTOOLS/srv.sh" dir)/server.log
OUT="$HERE/suite.txt"

: > "$OUT"
for SCENARIO in idle walk jump sprintjump strafe look crouch jumpstop; do
  MARK=$(wc -l < "$LOG" 2>/dev/null || echo 0)
  (cd "$HERE" && node play.js "$SCENARIO" 30 > "$HERE/run-$SCENARIO.log" 2>&1)
  {
    echo "######## $SCENARIO ########"
    grep -aE "scenario complete|DISCONNECTED|ERROR|KICKED" "$HERE/run-$SCENARIO.log" | head -3
    echo "-- checks that fired --"
    tail -n +"$MARK" "$LOG" 2>/dev/null \
      | grep -oE "\[violation\].*check=[a-zA-Z]+" \
      | sed 's/.*check=//' | sort | uniq -c
    echo "-- detail --"
    tail -n +"$MARK" "$LOG" 2>/dev/null | grep -a "\[violation\]" | sed 's/.*detail=/  /' \
      | cut -c1-200 | sort -u | head -6
  } >> "$OUT" 2>&1
  sleep 2
done
echo "SUITE COMPLETE" >> "$OUT"
cat "$OUT"
