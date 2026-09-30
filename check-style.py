#!/usr/bin/env python3
"""Fails on an em dash, an en dash, or a comment in the source tree.

The project carries none of the three. This exists so a future change cannot
add one without the build noticing.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SKIP = {".git", "node_modules", "build", "dist", ".gradle", "out"}
CODE = {".java", ".ts", ".tsx", ".js", ".mjs", ".py", ".sh", ".kts", ".css"}
TEXT = {".md", ".yml", ".yaml", ".toml", ".html", ".gradle", ".properties", ".gitignore"}

def main():
    problems = []
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or any(part in SKIP for part in path.parts):
            continue
        suffix = path.suffix.lower()
        name = path.name
        if suffix in CODE or suffix in TEXT or name == ".gitignore":
            try:
                text = path.read_text(encoding="utf-8")
            except Exception:
                continue
            for number, line in enumerate(text.split("\n"), 1):
                if "\u2014" in line or "\u2013" in line:
                    problems.append(f"{path}:{number}: em or en dash")
                stripped = line.strip()
                if suffix in CODE:
                    if stripped.startswith("///") and "reference" in stripped:
                        continue
                    if stripped.startswith("//") or stripped.startswith("/*") or stripped == "*/":
                        problems.append(f"{path}:{number}: comment")
                    elif suffix == ".css" and "/*" in line:
                        problems.append(f"{path}:{number}: comment")
    if problems:
        print(f"{len(problems)} style problem(s):")
        for problem in problems:
            print(f"  {problem}")
        return 1
    print("no em dashes, no en dashes, no comments")
    return 0

if __name__ == "__main__":
    sys.exit(main())
