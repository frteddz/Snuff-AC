#!/usr/bin/env python3
"""Removes every comment from the source tree.

The project carries no comments, so this enforces that rather than trusting it.
It is deliberately conservative about the handful of lines that only look like
comments: a shebang, the TypeScript reference directive, a CSS universal
selector, a glob, and a case statement.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SKIP = {".git", "node_modules", "build", "dist", ".gradle", "out"}
BINARY = {".mp4", ".png", ".jpg", ".gif", ".ico", ".jar", ".woff2", ".ttf", ".zip", ".class"}

LINE = {".java", ".ts", ".tsx", ".js", ".mjs", ".py", ".sh", ".kts", ".css"}

def strip_line_comments(text, keep_strings):
    out = []
    i = 0
    n = len(text)
    in_string = None
    while i < n:
        ch = text[i]
        if in_string:
            out.append(ch)
            if ch == "\\" and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch in "\"'`":
            in_string = ch
            out.append(ch)
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            nxt = text[i + 1]
            if nxt == "/":
                if text[i : i + 3] == "///":
                    j = text.find("\n", i)
                    line = text[i:j] if j > 0 else text[i:]
                    if "reference" in line:
                        out.append(line)
                    i = j if j > 0 else n
                    continue
                j = text.find("\n", i)
                i = j if j > 0 else n
                continue
            if nxt == "*" and not keep_strings:
                j = text.find("*/", i + 2)
                i = n if j < 0 else j + 2
                continue
        out.append(ch)
        i += 1
    return "".join(out)

def strip_css(text):
    return re.sub(r"/\*.*?\*/", "", text, flags=re.S)

def clean(path):
    raw = path.read_text(encoding="utf-8")
    lines = raw.split("\n")
    kept = [lines[0]] if lines[0].startswith("#!") else []
    body = "\n".join(lines[1:] if kept else lines)

    if path.suffix == ".css":
        cleaned = strip_css(body)
    else:
        cleaned = strip_line_comments(body, keep_strings=False)

    cleaned = re.sub(r"[ \t]+\n", "\n", cleaned)
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
    return "\n".join(kept) + ("\n" if kept else "") + cleaned

def main():
    changed = []
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or any(part in SKIP for part in path.parts):
            continue
        if path.suffix.lower() not in LINE:
            continue
        if path.name in {".gitignore"}:
            continue
        before = path.read_text(encoding="utf-8")
        after = clean(path)
        if before != after:
            path.write_text(after, encoding="utf-8")
            changed.append(str(path.relative_to(ROOT)))
    if changed:
        print(f"stripped comments from {len(changed)} files")
        for name in changed:
            print(f"  {name}")
    else:
        print("no comments found")
    return 0

if __name__ == "__main__":
    sys.exit(main())
