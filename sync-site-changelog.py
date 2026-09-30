#!/usr/bin/env python3
"""Generates web/src/releases.ts from CHANGELOG.md.

The site and the changelog kept drifting apart because they were two copies of
the same text. Now there is one source and the site is generated from it.

Two conventions appear in the file and both are read correctly. Early
releases use a short bold noun phrase as a heading with a list of independent
items under it, so the heading is a group. Later releases use a bold sentence
describing one defect with the detail as bullets under it, so the heading is
the change itself. Which one a release uses is decided from that release's own
headings rather than hardcoded per version.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CHANGELOG = ROOT / "CHANGELOG.md"
TARGET = ROOT / "web" / "src" / "releases.ts"

TYPE = (
    "export type ReleaseItem = { group: string | null; title: string; copy: string }\n"
    "export type Release = {\n"
    "  version: string\n"
    "  tag: string\n"
    "  stamp: string\n"
    "  pre: boolean\n"
    "  count: number\n"
    "  items: ReleaseItem[]\n"
    "}\n"
)

ENTRY = re.compile(r"^## \[(\d+\.\d+\.\d+(?:-dev)?)\] - (\S+)\s*$")
SUBHEAD = re.compile(r"^### (.+)\s*$")
BOLD = re.compile(r"^\*\*(.+?)\*\*\s*(?:\(.*?\))?\s*$")
BULLET = re.compile(r"^-\s+(.+)$")
SENTENCE = re.compile(r"(?<=[.!?])\s")
KEEP = re.compile(
    r"fix|add|chang|verif|improv|correct|limit|not yet|known|honest|number|licen"
)


def version_key(version):
    parts = []
    for chunk in version.replace("-dev", "").split("."):
        parts.append(int(chunk) if chunk.isdigit() else 0)
    while len(parts) < 3:
        parts.append(0)
    return tuple(parts)


def wrap(text):
    return " ".join(text.split())


def ts(value):
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


def title_from(text):
    parts = SENTENCE.split(text, maxsplit=1)
    title = parts[0]
    if len(title) > 96:
        cut = title.rfind(" ", 0, 96)
        title = (title[:cut] if cut > 40 else title[:96]).rstrip(",;")
    return title, (parts[1] if len(parts) > 1 else "")


def heading_kinds(sections):
    """Decides per heading whether it names a group or is the change itself.

    A group heading is a short noun phrase like "Engine" or "Hitbox
    verification" that owns several items. A heading that is the change reads
    as a sentence about one defect, so it is longer, and the lines under it
    are its detail. A release can mix both, so this is decided heading by
    heading rather than once per release.
    """
    kinds = {}
    for body in sections:
        pending = None
        bullets = 0
        for line in body:
            bold = BOLD.match(line)
            if bold:
                if pending is not None:
                    kinds[pending] = bullets >= 2 and len(pending.split()) <= 4
                pending = wrap(bold.group(1))
                bullets = 0
            elif BULLET.match(line) and pending is not None:
                bullets += 1
        if pending is not None:
            kinds[pending] = bullets >= 2 and len(pending.split()) <= 4
    return kinds


def split_grouped(body, items, kinds):
    """Reads a section where a heading is either a group or the change."""
    group = None
    heading = None
    detail = []

    def close():
        nonlocal heading, detail
        if heading is not None:
            items.append({"group": None, "title": heading, "copy": as_prose(detail)})
        heading = None
        detail = []

    for line in body:
        bold = BOLD.match(line)
        if bold:
            close()
            text = wrap(bold.group(1))
            if kinds.get(text):
                group = text or None
            else:
                group = None
                heading = text
            continue
        if line.startswith("#") or not line.strip():
            continue
        bullet = BULLET.match(line)
        if heading is not None:
            # a heading that is the change, so everything under it is detail
            detail.append(wrap(bullet.group(1) if bullet else line))
            continue
        if not bullet:
            if items and items[-1]["group"] == group:
                items[-1]["copy"] = wrap(items[-1]["copy"] + " " + wrap(line))
            continue
        title, rest = title_from(wrap(bullet.group(1)))
        items.append({"group": group, "title": title, "copy": rest})


def split_flat(body, items):
    """A bold heading is the change, and the lines under it are its detail."""
    heading = None
    detail = []

    def close():
        nonlocal heading, detail
        if heading is not None:
            items.append({
                "group": None,
                "title": heading,
                "copy": as_prose(detail),
            })
        heading = None
        detail = []

    for line in body:
        bold = BOLD.match(line)
        if bold:
            close()
            heading = wrap(bold.group(1))
            continue
        if line.startswith("#") or not line.strip():
            continue
        bullet = BULLET.match(line)
        detail.append(wrap(bullet.group(1) if bullet else line))
    close()
    return items


def as_prose(lines):
    """Joins a detail list into one paragraph."""
    parts = []
    for index, line in enumerate(lines):
        if index and not line[0].islower() and not line[0].isupper():
            parts.append(".")
        elif index and line[:1].isupper() and parts and parts[-1].endswith("."):
            parts.append(" ")
        elif index:
            parts.append(" ")
        parts.append(line)
    text = "".join(parts)
    return re.sub(r"\s+", " ", text).replace("..", ".").strip()


def parse(lines):
    releases = []
    version = None
    stamp = None
    section = None
    body = []
    sections = []

    def close_section():
        nonlocal section, body
        if section is not None and KEEP.search(section):
            sections.append(body)
        section = None
        body = []

    def close_release():
        close_section()
        nonlocal version, sections
        if version is not None and sections:
            items = []
            kinds = heading_kinds(sections)
            for chunk in sections:
                if kinds and not any(kinds.values()):
                    split_flat(chunk, items)
                else:
                    # a release with no bold headings at all has bare bullets,
                    # which split_grouped reads as ungrouped items
                    split_grouped(chunk, items, kinds)
            items = [item for item in items if item["title"]]
            if items:
                releases.append({"version": version, "stamp": stamp, "items": items})
        version = None
        sections = []

    for line in lines:
        entry = ENTRY.match(line)
        if entry:
            close_release()
            version, stamp = entry.group(1), entry.group(2)
            continue
        if version is None:
            continue
        sub = SUBHEAD.match(line)
        if sub:
            close_section()
            section = sub.group(1).lower()
            continue
        if section is not None:
            body.append(line)
    close_release()
    return releases


def main():
    if not CHANGELOG.is_file():
        print("CHANGELOG.md not found", file=sys.stderr)
        return 1

    releases = parse(CHANGELOG.read_text(encoding="utf-8").splitlines())
    if not releases:
        print("no releases parsed", file=sys.stderr)
        return 1

    # oldest first, so the page reads as a history rather than a feed
    releases.sort(key=lambda release: version_key(release["version"]))

    out = [TYPE, "", "export const releases: Release[] = ["]
    for release in releases:
        out.append("  {")
        out.append(f"    version: {ts(release['version'])},")
        out.append(f"    tag: {ts('v' + release['version'])},")
        out.append(f"    stamp: {ts(release['stamp'])},")
        out.append(f"    pre: {'true' if release['version'].endswith('-dev') else 'false'},")
        out.append(f"    count: {len(release['items'])},")
        out.append("    items: [")
        for item in release["items"]:
            group = ts(item["group"]) if item["group"] else "null"
            out.append("      {")
            out.append(f"        group: {group},")
            out.append(f"        title: {ts(item['title'])},")
            out.append(f"        copy: {ts(item['copy'])},")
            out.append("      },")
        out.append("    ],")
        out.append("  },")
    out.append("]")
    out.append("")

    TARGET.write_text("\n".join(out), encoding="utf-8")
    total = sum(len(release["items"]) for release in releases)
    grouped = sum(1 for r in releases for i in r["items"] if i["group"])
    print(
        f"wrote {TARGET.relative_to(ROOT)}: {len(releases)} releases, "
        f"{total} entries, {grouped} under a group"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
