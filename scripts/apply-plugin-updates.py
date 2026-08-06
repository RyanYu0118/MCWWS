#!/usr/bin/env python3
"""Backup old plugin jars and install updated ones from 待更新."""

from __future__ import annotations

import shutil
from pathlib import Path

PLUGINS = Path(r"D:\Minecraft\服务器\26.2\plugins")
PENDING = PLUGINS / "待更新"
BACKUP = PLUGINS / "旧版备份"

# new_jar_in_pending -> list of old jar name patterns / exact names to remove
REPLACEMENTS = [
    ("Citizens-2.0.43-b4231.jar", ["Citizens-2.0.41-b4138.jar"]),
    ("Denizen-1.3.3-b7299-DEV.jar", ["Denizen-1.3.0-b1804-REL.jar"]),
    ("Sentinel-2.9.4-SNAPSHOT-b534.jar", ["Sentinel-2.9.2-SNAPSHOT-b528.jar"]),
    ("Depenizen-2.1.1-b885.jar", ["Depenizen-2.1.1-b878.jar"]),
    ("BKCommonLib-2.0.2-SNAPSHOT-2029.jar", ["BKCommonLib-2.0.1-2025.jar"]),
    ("TrainCarts-2.0.1-SNAPSHOT-1734.jar", ["TrainCarts-2.0.0-1720.jar"]),
    ("TCCoasters-2.0.1-SNAPSHOT-408.jar", ["TCCoasters-1.21.11-v1-383.jar"]),
    ("fake-player-plugin-2.0.3.jar", ["fakeplayer-0.3.19.jar"]),
]

# Remove from plugins (not a server plugin). Do not install Fabric client jar.
REMOVE_ONLY = [
    "SmoothCoasters-1.21.11-v1.jar",
    "clientizen-beta-1.0-74.jar",  # also invalid plugin.yml
]


def main() -> None:
    lock = Path(r"D:\Minecraft\服务器\26.2\world\session.lock")
    if lock.exists():
        print(f"WARNING: {lock} still exists — server may still be running.")

    BACKUP.mkdir(parents=True, exist_ok=True)

    moved: list[str] = []
    installed: list[str] = []
    missing_new: list[str] = []
    missing_old: list[str] = []

    for new_name, old_names in REPLACEMENTS:
        new_path = PENDING / new_name
        if not new_path.is_file():
            missing_new.append(new_name)
            continue

        for old_name in old_names:
            old_path = PLUGINS / old_name
            if not old_path.is_file():
                missing_old.append(old_name)
                continue
            dest = BACKUP / old_name
            if dest.exists():
                dest.unlink()
            shutil.move(str(old_path), str(dest))
            moved.append(old_name)

        target = PLUGINS / new_name
        if target.exists():
            target.unlink()
        shutil.copy2(new_path, target)
        installed.append(new_name)

    for name in REMOVE_ONLY:
        path = PLUGINS / name
        if path.is_file():
            dest = BACKUP / name
            if dest.exists():
                dest.unlink()
            shutil.move(str(path), str(dest))
            moved.append(f"{name} (remove-only)")

    print("=== Backed up ===")
    for n in moved:
        print(" ", n)
    print("=== Installed ===")
    for n in installed:
        print(" ", n)
    if missing_new:
        print("=== Missing in 待更新 ===")
        for n in missing_new:
            print(" ", n)
    if missing_old:
        print("=== Old jar not found (maybe already replaced) ===")
        for n in missing_old:
            print(" ", n)

    # Quick verify
    print("=== Verify plugins root ===")
    for new_name, _ in REPLACEMENTS:
        p = PLUGINS / new_name
        print(f"  {'OK' if p.is_file() else 'MISSING'} {new_name}")
    for name in REMOVE_ONLY:
        p = PLUGINS / name
        print(f"  {'STILL PRESENT' if p.is_file() else 'removed'} {name}")


if __name__ == "__main__":
    main()
