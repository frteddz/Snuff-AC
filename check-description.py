#!/usr/bin/env python3
"""Checks the marketplace description against the source of truth.

The description is hand written, so it drifts. Every check name, command and
permission node it mentions is verified against checks.yml, SnuffCommand.java
and plugin.yml, and the version against gradle.properties.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent

DESC = ROOT / "description-modrinth-curseforge.md"
CHECKS = ROOT / "snuffac-platform-paper" / "src/main/resources/checks.yml"
PLUGIN = ROOT / "snuffac-platform-paper" / "src/main/resources/plugin.yml"
COMMAND = ROOT / "snuffac-platform-paper/src/main/java/dev/snuffac/paper/SnuffCommand.java"
PROPERTIES = ROOT / "gradle.properties"

# words in the description that are bolded for emphasis rather than naming a check
NOT_CHECKS = {"structural", "derived", "strict", "before", "after"}


def main():
    doc = DESC.read_text(encoding="utf-8")
    problems = []

    checks = set(re.findall(r"^    ([a-z0-9]+):", CHECKS.read_text(encoding="utf-8"), re.M))
    named = [n for n in re.findall(r"\*\*([A-Za-z][A-Za-z0-9]*)\*\*", doc)
             if n.lower() not in NOT_CHECKS]
    for name in named:
        if name.lower() not in checks:
            problems.append(f"description names a check that does not exist: {name}")
    if len(checks) != 32:
        problems.append(f"expected 32 checks in checks.yml, found {len(checks)}")
    if doc.count("32 checks") == 0:
        problems.append("description does not state the check count")

    source = COMMAND.read_text(encoding="utf-8")
    block = source[source.index("SUB_COMMANDS = List.of("):]
    block = block[: block.index(");")]
    commands = set(re.findall(r'"([a-z]+)"', block))
    for name in re.findall(r"/snuff ([a-z]+)", doc):
        if name not in commands:
            problems.append(f"description names a command that does not exist: /snuff {name}")

    nodes = set(re.findall(r"^  (snuff\S*):", PLUGIN.read_text(encoding="utf-8"), re.M))
    for node in re.findall(r"`(snuff[ac0-9.]+[a-z0-9.*]*)`", doc):
        if node not in nodes:
            problems.append(f"description names a permission that is not declared: {node}")
    for claimed in re.findall(r"(\d+) declared nodes", doc):
        if int(claimed) != len(nodes):
            problems.append(f"description claims {claimed} permission nodes, plugin.yml has {len(nodes)}")

    version = re.search(r"version=([0-9.]+(?:-dev)?)", PROPERTIES.read_text(encoding="utf-8")).group(1)
    if f"**{version}**" not in doc:
        problems.append(f"description does not state the current version, expected {version}")

    if "storage" not in doc.lower():
        problems.append("description does not disclose that storage ESP is not implemented")
    if "A1A3259HI2" not in doc:
        problems.append("the ko-fi link does not match the one used by the site")

    if problems:
        print(f"{len(problems)} problem(s) in the description:")
        for problem in problems:
            print(f"  {problem}")
        return 1
    print(f"description is in sync: {len(checks)} checks, {len(commands)} commands, {len(nodes)} permissions, version {version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
