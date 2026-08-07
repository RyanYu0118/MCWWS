#!/usr/bin/env python3
"""Make Slimefun-ResourcePack declare support for Minecraft 26.2 (pack format 88)."""

from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

RP = Path(r"D:\Minecraft\游戏主体\.minecraft\resourcepacks\Slimefun-ResourcePack")
OPTIONS = Path(r"D:\Minecraft\游戏主体\.minecraft\options.txt")
MCMETA = RP / "pack.mcmeta"
BACKUP = RP / "pack.mcmeta.bak-1.21.11"


def main() -> None:
    if not RP.is_dir():
        raise SystemExit(f"missing resource pack: {RP}")

    if not BACKUP.exists():
        shutil.copy2(MCMETA, BACKUP)
        print(f"backed up -> {BACKUP.name}")
    else:
        print(f"backup already exists: {BACKUP.name}")

    overlay_dirs = {
        p.name for p in RP.iterdir() if p.is_dir() and p.name.startswith("ia_overlay")
    }
    print("overlay dirs:", sorted(overlay_dirs))

    # Only keep overlays that exist on disk.
    # Extend ia_overlay_1_21_6_plus through format 88 so CMD item models apply on 26.2.
    overlays: list[dict] = []
    if "ia_overlay_1_21_4_to_5" in overlay_dirs:
        overlays.append(
            {
                "directory": "ia_overlay_1_21_4_to_5",
                "formats": [46, 55],
                "min_format": 46,
                "max_format": 55,
            }
        )
    if "ia_overlay_1_21_6_plus" in overlay_dirs:
        overlays.append(
            {
                "directory": "ia_overlay_1_21_6_plus",
                "formats": [63, 88],
                "min_format": 63,
                "max_format": 88,
            }
        )

    data = {
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
                "\n§d26.2 compatible §f(max_format 88)",
            ],
            "pack_format": 88,
            "min_format": 9,
            "max_format": 88,
            "supported_formats": {
                "min_inclusive": 9,
                "max_inclusive": 88,
            },
        },
    }

    MCMETA.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("wrote pack.mcmeta")

    if OPTIONS.exists():
        text = OPTIONS.read_text(encoding="utf-8")

        def fix_incompatible(match: re.Match[str]) -> str:
            inner = match.group(1)
            parts = [p.strip().strip('"') for p in inner.split(",") if p.strip()]
            parts = [p for p in parts if p != "file/Slimefun-ResourcePack"]
            if not parts:
                return "incompatibleResourcePacks:[]"
            joined = ",".join(f'"{p}"' for p in parts)
            return f"incompatibleResourcePacks:[{joined}]"

        new_text, n = re.subn(
            r"incompatibleResourcePacks:\[(.*?)\]",
            fix_incompatible,
            text,
            count=1,
        )
        if n and new_text != text:
            OPTIONS.write_text(new_text, encoding="utf-8")
            print("removed Slimefun-ResourcePack from incompatibleResourcePacks")
        else:
            print("options.txt: no incompatible Slimefun entry to clear (or unchanged)")

    print("done")


if __name__ == "__main__":
    main()
