#!/usr/bin/env bash
# Advances the project version using the scheme 1.0.9 -> 1.1.0 -> 1.1.1 .. 1.1.9 -> 1.2.0
# Usage: ./bump-version.sh [explicit-version]
set -euo pipefail
cd "$(dirname "$0")"
if [ -n "${1:-}" ]; then
  TARGET="$1"
  case "$TARGET" in
    *-dev) ;;
    *) TARGET="$TARGET-dev" ;;
  esac
else
  CURRENT=$(grep -oE 'version=[0-9.]+' gradle.properties | head -1 | cut -d= -f2)
  BASE=${CURRENT%-dev}
  MINOR=${BASE#*.}
  PATCH=${MINOR#*.}
  MINOR=${MINOR%%.*}
  if [ "$PATCH" -ge 9 ]; then
    TARGET="1.$((MINOR + 1)).0-dev"
  else
    TARGET="1.$MINOR.$((PATCH + 1))-dev"
  fi
fi
sed -i "s/^version=.*/version=$TARGET/" gradle.properties
sed -i "s/^        version = \".*\",$/        version = \"$TARGET\",/" snuffac-platform-velocity/src/main/java/dev/snuffac/velocity/SnuffVelocityPlugin.java
echo "$TARGET"
