#!/usr/bin/env bash
# Builds a version, installs it into the local server, restarts, and waits until
# the plugin reports itself enabled. Never blocks forever.
# Usage: ./cycle.sh [version]
set -uo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/.." && pwd)
VERSION="${1:-}"

if [ -z "$VERSION" ]; then
  VERSION=$(grep -oE 'version=[0-9.]+(-dev)?' "$ROOT/gradle.properties" | head -1 | cut -d= -f2)
fi
echo "cycling to $VERSION"

cd "$ROOT" || exit 1
TEST_OUT=$(mktemp)
if ! ./gradlew test > "$TEST_OUT" 2>&1; then
  echo "TESTS FAILED, not deploying" >&2
  grep -E "FAILED|error:" "$TEST_OUT" | head -10 >&2
  rm -f "$TEST_OUT"
  exit 1
fi
rm -f "$TEST_OUT"
echo "tests passed"

./gradlew :snuffac-dist:publishJar -q || exit 1
ARTIFACT="$ROOT/snuffac-1.21.x+paper/purpur/snuffac-v$VERSION.jar"
[ -f "$ARTIFACT" ] || {
  echo "artifact missing: $ARTIFACT" >&2
  exit 1
}
echo "built $(basename "$ARTIFACT")"

DIR=$("$HERE/srv.sh" dir)
mkdir -p "$DIR/plugins"
rm -f "$DIR/plugins"/snuffac-*.jar
cp "$ARTIFACT" "$DIR/plugins/snuffac-v$VERSION.jar"
echo "installed"

"$HERE/srv.sh" restart
"$HERE/srv.sh" wait || exit 1
"$HERE/srv.sh" logs 400 | grep -E "Snuff AC|anti-xray|checks" | tail -8
