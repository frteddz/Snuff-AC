#!/usr/bin/env python3
"""Generates web/src/releases.ts from CHANGELOG.md.

The site and the changelog kept drifting apart because they were two copies of
the same text. Now there is one source and the site is generated from it.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CHANGELOG = ROOT / "CHANGELOG.md"
TARGET = ROOT / "web" / "src" / "releases.ts"

TYPE = "type ReleaseItem = { title: string; copy: string }\ntype Release = { version: string; stamp: string; items: ReleaseItem[] }\n"

ENTRY = re.compile(r"^## \[(\d+\.\d+\.\d+(?:-dev)?)\] - (\S+)\s*$")
SUBHEAD = re.compile(r"^### (.+)\s*$")
BOLD = re.compile(r"^\*\*(.+?)\*\*")
BULLET = re.compile(r"^- (.+)$")


def wrap(text):
    return " ".join(text.split())


def ts(value):
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


def main():
    if not CHANGELOG.is_file():
        print("CHANGELOG.md not found", file=sys.stderr)
        return 1

    lines = CHANGELOG.read_text(encoding="utf-8").splitlines()
    releases = []
    current = None
    section = None
    item = None
    body = []

    def flush_item():
        nonlocal item, body
        if item and body:
            item["copy"] = wrap(" ".join(body))
        elif item:
            item["copy"] = ""
        item = None
        body = []

    def flush_section():
        nonlocal current
        if current and current["items"]:
            releases.append(current)
        current = None

    for line in lines:
        entry = ENTRY.match(line)
        if entry:
            flush_item()
            flush_section()
            current = {
                "version": entry.group(1),
                "stamp": entry.group(2),
                "items": [],
            }
            section = None
            continue
        if current is None:
            continue
        sub = SUBHEAD.match(line)
        if sub:
            flush_item()
            section = sub.group(1).lower()
            continue
        if section not in ("fixed", "added", "changed", "verified", "improved"):
            continue
        bold = BOLD.match(line)
        if bold:
            flush_item()
            item = {
                "title": wrap(bold.group(1)),
                "copy": "",
            }
            current["items"].append(item)
            body = []
            continue
        bullet = BULLET.match(line)
        if bullet and item is not None:
            body.append(wrap(bullet.group(1)))
            continue
        if item is not None and line.strip() and not line.startswith("#"):
            body.append(wrap(line))
    flush_item()
    flush_section()

    if not releases:
        print("no releases parsed", file=sys.stderr)
        return 1

    out = [TYPE, "", "export const releases: Release[] = ["]
    for release in releases:
        out.append("  {")
        out.append(f"    version: {ts(release['version'])},")
        out.append(f"    stamp: {ts(release['stamp'])},")
        out.append("    items: [")
        for entry in release["items"]:
            out.append("      {")
            out.append(f"        title: {ts(entry['title'])},")
            out.append(f"        copy: {ts(entry['copy'])},")
            out.append("      },")
        out.append("    ],")
        out.append("  },")
    out.append("]")
    out.append("")

    TARGET.write_text("\n".join(out), encoding="utf-8")
    total = sum(len(r["items"]) for r in releases)
    print(f"wrote {TARGET.relative_to(ROOT)}: {len(releases)} releases, {total} entries")
    return 0


if __name__ == "__main__":
    sys.exit(main())
