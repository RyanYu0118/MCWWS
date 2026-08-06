#!/usr/bin/env python3
"""Remove post-migration leftovers and upgrade-session backup files."""

from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / "archive" / "legacy-datapacks"

# Individual backup files created during 26.2 upgrade troubleshooting
BACKUP_FILES = [
    ROOT / "server-icon.png.jpg.bak",
    ROOT / "world" / "level.dat.bak",
    ROOT / "world" / "level.dat_old",
    ROOT / "dimensionalhome" / "level.dat_old",
    ROOT / "world" / "dimensions" / "minecraft" / "overworld" / "data" / "minecraft" / "world_gen_settings.dat.bak2",
    ROOT / "world" / "dimensions" / "minecraft" / "the_end" / "data" / "minecraft" / "world_gen_settings.dat.bak",
]

BACKUP_GLOBS = [
    ROOT / "world" / "players" / "advancements" / "*.json.bak",
    ROOT / "world" / "players" / "data" / "*.dat.bak",
]

# Old root-level world folders superseded by world/dimensions/minecraft/*
LEGACY_WORLD_DIRS = [
    ROOT / "world_nether",
    ROOT / "world_the_end",
]

# Failed startup crash reports from Incendium / world_gen migration
CRASH_REPORTS = list((ROOT / "crash-reports").glob("crash-2026-08-06_*.txt")) if (ROOT / "crash-reports").exists() else []


def archive_disabled_datapacks() -> list[Path]:
    moved: list[Path] = []
    datapacks = ROOT / "world" / "datapacks"
    if not datapacks.is_dir():
        return moved

    ARCHIVE.mkdir(parents=True, exist_ok=True)
    for path in sorted(datapacks.glob("_disabled_*")):
        target = ARCHIVE / path.name
        if target.exists():
            target.unlink()
        shutil.move(str(path), str(target))
        moved.append(target)
    return moved


def remove_path(path: Path) -> bool:
    if not path.exists():
        return False
    if path.is_dir():
        shutil.rmtree(path)
    else:
        path.unlink()
    return True


def main() -> None:
    removed_files = 0
    removed_dirs = 0

    for path in BACKUP_FILES:
        if remove_path(path):
            print(f"removed file: {path.relative_to(ROOT)}")
            removed_files += 1

    for pattern in BACKUP_GLOBS:
        for path in pattern.parent.glob(pattern.name):
            if remove_path(path):
                print(f"removed backup: {path.relative_to(ROOT)}")
                removed_files += 1

    for path in LEGACY_WORLD_DIRS:
        if remove_path(path):
            print(f"removed legacy world dir: {path.relative_to(ROOT)}")
            removed_dirs += 1
        elif path.exists():
            print(f"skip legacy world dir (not empty or locked): {path.relative_to(ROOT)}")
        else:
            print(f"legacy world dir already gone: {path.name}")

    for path in CRASH_REPORTS:
        if remove_path(path):
            print(f"removed crash report: {path.relative_to(ROOT)}")
            removed_files += 1

    moved = archive_disabled_datapacks()
    for path in moved:
        print(f"archived datapack: {path.relative_to(ROOT)}")

    print()
    print(f"Done. removed {removed_files} files, {removed_dirs} legacy dirs, archived {len(moved)} datapacks.")


if __name__ == "__main__":
    main()
