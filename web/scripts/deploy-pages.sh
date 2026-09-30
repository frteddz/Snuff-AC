#!/usr/bin/env bash
# Publishes the built site to the gh-pages branch.
#
# The Actions workflow does this too, but it only runs once Pages is switched
# to the "GitHub Actions" source in the repository settings, which is a
# one time manual step. This script works today and keeps working after that.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
DIST="$ROOT/web/dist"
BRANCH=gh-pages
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

[ -f "$DIST/index.html" ] || { echo "no build found, run npm run build in web/" >&2; exit 1; }
[ -f "$DIST/sitemap.xml" ] || { echo "sitemap missing, the build did not finish" >&2; exit 1; }

# every local asset the html and css ask for has to exist in the build, or the
# page ships broken. this is how the hero video went missing: it was ignored by
# a *.mp4 rule in .gitignore, so it was never in the checkout the ci builds.
missing=0
while read -r url; do
  target="$DIST${url#*Snuff-AC}"
  if [ ! -f "$target" ]; then
    echo "referenced but not built: $url" >&2
    missing=1
  fi
done < <(
  grep -ohE '(src|poster|href)="/Snuff-AC/[^"#?]+"' "$DIST"