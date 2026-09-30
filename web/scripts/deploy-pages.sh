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

# a custom domain redirects every visitor, so it only ships when asked for
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
