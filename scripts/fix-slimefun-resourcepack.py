#!/usr/bin/env python3
"""Make Slimefun-ResourcePack declare support for Minecraft up to 26.3 (pack format 97).

Older clients keep working: pack_format / supported_formats still start at 9, and each
overlay carries both the legacy `formats` range and the 1.21.9+ min/max_format pair.
Patches the unpacked folder and the zip that options.txt actually enables.
"""

from __future__ import annotations

import json
import os
import re
import shutil
import zipfile
from pathlib import Path

PACKS = Path(r"D:\Minecraft\游戏主体\.minecraft\resourcepacks")
RP = PACKS / "Slimefun-ResourcePack"
RP_ZIP = PACKS / "Slimefun-ResourcePack.zip"
OPTIONS = Path(r"D:\Minecraft\游戏主体\.minecraft\options.txt")
MCMETA = RP / "pack.mcmeta"

TARGET_VERSION = "26.3"
TARGET_FORMAT = 97
BACKUP_SUFFIX = ".bak-26.2"

# 26.3 vanilla item definitions for every file these overlays override are identical to
# 26.2, so the 1.21.6+ and 26.2 head overlays simply extend up to TARGET_FORMAT.
OVERLAYS = [
    ("ia_overlay_1_21_4_to_5", 46, 55),
    ("ia_overlay_1_21_6_plus", 63, TARGET_FORMAT),
    ("ia_overlay_26_2_heads", 88, TARGET_FORMAT),
]


def build_mcmeta(overlay_dirs: set[str]) -> dict:
    overlays = [
        {
            "directory": name,
            "formats": [lo, hi],
            "min_format": lo,
            "max_format": hi,
        }
        for name, lo, hi in OVERLAYS
        if name in overlay_dirs
    ]
    return {
        "overlays": {"entries": overlays},
        "pack": {
            "Credits": {
                "AnsonYK": "On Discord",
                "Caribax": "https://github.com/Mooy1/InfinityExpansion/releases/tag/v1",
                "Den4enko": "https://github.com/Den4enko/Slimefun-Resourcepack",
                "DragonMysterious": "On Discord",
                "Filosofas154": "https://github.com/Filosofas154",
                "Jerry": "https://github.com/Keeywe",
                "JustAHuman-xD": "https://github.com/JustAHuman-xD",
                "LoneDev": " https://www.spigotmc.org/resources/addon-slimefun4-textures-for-itemsadder.83877/",
                "Pandicka": "https://github.com/AlmostPanda",
                "Raulh22": "https://www.planetminecraft.com/texture-pack/slimefun-texture-by-raulh22/",
                "RelativoBR": "https://github.com/RelativoBR",
                "Sofia Redmond": "https://github.com/SofiaRedmond",
                "haiman": "https://github.com/haiman233",
                "ybw0014": "https://gzss.link/sf-texture",
            },
            "description": [
                "§2Slimefun §9Resourcepack §6Remake",
                f"\n§d1.20~{TARGET_VERSION} compatible §f(format 9-{TARGET_FORMAT})",
            ],
            "pack_format": TARGET_FORMAT,
            "min_format": 9,
            "max_format": TARGET_FORMAT,
            "supported_formats": {
                "min_inclusive": 9,
                "max_inclusive": TARGET_FORMAT,
            },
        },
    }


def mcmeta_bytes(data: dict) -> bytes:
    return (json.dumps(data, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def backup_once(path: Path) -> None:
    backup = path.with_name(path.name + BACKUP_SUFFIX)
    if backup.exists():
        print(f"backup already exists: {backup.name}")
    else:
        shutil.copy2(path, backup)
        print(f"backed up -> {backup.name}")


def patch_folder() -> None:
    if not RP.is_dir():
        print(f"skip folder (missing): {RP}")
        return
    backup_once(MCMETA)
    overlay_dirs = {p.name for p in RP.iterdir() if p.is_dir() and p.name.startswith("ia_overlay")}
    print("folder overlay dirs:", sorted(overlay_dirs))
    MCMETA.write_bytes(mcmeta_bytes(build_mcmeta(overlay_dirs)))
    print("wrote folder pack.mcmeta")


def patch_zip() -> None:
    if not RP_ZIP.is_file():
        print(f"skip zip (missing): {RP_ZIP}")
        return
    backup_once(RP_ZIP)
    tmp = RP_ZIP.with_name(RP_ZIP.name + ".new")
    with zipfile.ZipFile(RP_ZIP) as src:
        overlay_dirs = {
            n.split("/", 1)[0] for n in src.namelist() if n.startswith("ia_overlay") and "/" in n
        }
        print("zip overlay dirs:", sorted(overlay_dirs))
        with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
            for info in src.infolist():
                if info.filename == "pack.mcmeta":
                    continue
                dst.writestr(info, src.read(info.filename))
            dst.writestr("pack.mcmeta", mcmeta_bytes(build_mcmeta(overlay_dirs)))
    try:
        os.replace(tmp, RP_ZIP)
        print("wrote zip pack.mcmeta")
    except PermissionError:
        print(f"zip is locked (game running?); new pack left at {tmp.name}, swap it in after closing the game")


def clear_incompatible_flag() -> None:
    if not OPTIONS.exists():
        return
    text = OPTIONS.read_text(encoding="utf-8")
    ours = {"file/Slimefun-ResourcePack", "file/Slimefun-ResourcePack.zip"}

    def fix(match: re.Match[str]) -> str:
        parts = [p.strip().strip('"') for p in match.group(1).split(",") if p.strip()]
        parts = [p for p in parts if p not in ours]
        return "incompatibleResourcePacks:[" + ",".join(f'"{p}"' for p in parts) + "]"

    new_text, n = re.subn(r"incompatibleResourcePacks:\[(.*?)\]", fix, text, count=1)
    if n and new_text != text:
        OPTIONS.write_text(new_text, encoding="utf-8")
        print("removed Slimefun-ResourcePack from incompatibleResourcePacks")
    else:
        print("options.txt: no incompatible Slimefun entry to clear")


def main() -> None:
    patch_folder()
    patch_zip()
    if os.environ.get("SKIP_OPTIONS") != "1":
        clear_incompatible_flag()
    print("done")


if __name__ == "__main__":
    main()
