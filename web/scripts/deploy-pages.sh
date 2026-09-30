#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
DIST="$ROOT/web/dist"
BRANCH=gh-pages
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

[ -f "$DIST/index.html" ] || { echo "no build found, run npm run build in web/" >&2; exit 1; }
[ -f "$DIST/sitemap.xml" ] || { echo "sitemap missing, the build did not finish" >&2; exit 1; }

referenced=$(grep -ohE '(src|poster|href)="/Snuff-AC/[^"#?]+"' "$DIST"/*.html | sed -E 's/.*="//; s/"$//' | sort -u)

missing=0
for url in $referenced; do
  target="$DIST${url#*Snuff-AC}"
  if [ ! -f "$target" ]; then
    echo "referenced but not built: $url" >&2
    missing=1
  fi
done

[ "$missing" -eq 0 ] || { echo "refusing to publish a site with missing assets" >&2; exit 1; }

if [ "${1:-}" = "--domain" ]; then
  cp "$ROOT/web/public/CNAME.documented" "$WORK/CNAME"
  sed -i 's/^# //' "$WORK/CNAME"
fi

git -C "$WORK" init -q -b "$BRANCH"
cp -r "$DIST"/. "$WORK"/
touch "$WORK/.nojekyll"
cd "$WORK"
git add -A
git -c user.name="${GIT_NAME:-github-actions[bot]}" \
    -c user.email="${GIT_EMAIL:-github-actions[bot]@users.noreply.github.com}" \
    commit -q -m "deploy site from ${GIT_COMMIT:-local}"
git push -q --force "https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" "$BRANCH"

echo "published $BRANCH"
