#!/usr/bin/env python3
"""Copy plugin jars and config folders from 1.21.11 server to 26.2."""

from __future__ import annotations

import shutil
from pathlib import Path

OLD_PLUGINS = Path(r"D:\Minecraft\服务器\1.21.11\plugins")
NEW_PLUGINS = Path(r"D:\Minecraft\服务器\26.2\plugins")

SKIP_DIRS = {
    ".paper-remapped",
    ".venv",
    ".vscode",
}

SKIP_JAR_SUFFIXES = (
    "-sources.jar",
)

# API libraries, not Bukkit plugins — skip to reduce noise on startup
SKIP_JAR_NAMES = {
    "VaultAPI-1.7.1.jar",
    "denizencore-1.91.0-SNAPSHOT.jar",
    "citizensapi-2.0.41-SNAPSHOT.jar",
}


def should_copy_jar(path: Path) -> bool:
    name = path.name
    if name in SKIP_JAR_NAMES:
        return False
    return not any(name.endswith(suffix) for suffix in SKIP_JAR_SUFFIXES)


def copy_jars() -> tuple[int, int]:
    copied = skipped = 0
    NEW_PLUGINS.mkdir(parents=True, exist_ok=True)
    for jar in sorted(OLD_PLUGINS.glob("*.jar")):
        if not should_copy_jar(jar):
            skipped += 1
            continue
        target = NEW_PLUGINS / jar.name
        shutil.copy2(jar, target)
        copied += 1
    return copied, skipped


def copy_config_dirs() -> tuple[int, int]:
    copied = skipped = 0
    for entry in sorted(OLD_PLUGINS.iterdir()):
        if not entry.is_dir():
            continue
        if entry.name in SKIP_DIRS or entry.name.startswith("."):
            skipped += 1
            continue
        target = NEW_PLUGINS / entry.name
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(entry, target)
        copied += 1
    return copied, skipped


def main() -> None:
    if not OLD_PLUGINS.is_dir():
        raise SystemExit(f"Source not found: {OLD_PLUGINS}")

    jar_copied, jar_skipped = copy_jars()
    dir_copied, dir_skipped = copy_config_dirs()

    print(f"JARs copied: {jar_copied} (skipped {jar_skipped} library/dev jars)")
    print(f"Config dirs copied: {dir_copied} (skipped {dir_skipped} internal dirs)")
    print(f"Target: {NEW_PLUGINS}")
    print()
    print("Next: restart the 26.2 server and check logs/latest.log for load errors.")
    print("Some jars tagged 1.21.11 may need manual updates for 26.2.")


if __name__ == "__main__":
    main()
