#!/usr/bin/env python3
"""Fix Slimefun-ResourcePack husk spawn egg + zombie head on Minecraft 26.2.

Causes:
1. assets/.../models/item/husk_spawn_egg.json still parents minecraft:item/template_spawn_egg,
   which was removed when spawn eggs switched to per-egg textures → purple missing texture.
2. zombie_head special models lack transformation.translation [0.5, 0, 0.5] (same as the
   old player_head corner-offset bug on 26.2).
"""

from __future__ import annotations

import json
import shutil
from pathlib import Path

RP = Path(r"D:\Minecraft\游戏主体\.minecraft\resourcepacks\Slimefun-ResourcePack")
OVERLAY_PLUS = RP / "ia_overlay_1_21_6_plus" / "assets" / "minecraft" / "items"
OVERLAY_26 = RP / "ia_overlay_26_2_heads" / "assets" / "minecraft" / "items"
HUSK_MODEL = RP / "assets" / "minecraft" / "models" / "item" / "husk_spawn_egg.json"

VANILLA_TRANSFORM = {
    "left_rotation": [1.0, 0.0, 0.0, -0.0],
    "right_rotation": [0.0, 0.0, 0.0, 1.0],
    "scale": [1.0, 1.0, 1.0],
    "translation": [0.5, 0.0, 0.5],
}

VANILLA_HUSK_MODEL = {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "minecraft:item/husk_spawn_egg"},
}

HUSK_ITEMS = {
    "model": {
        "type": "minecraft:range_dispatch",
        "property": "minecraft:custom_model_data",
        "index": 0,
        "fallback": {
            "type": "minecraft:model",
            "model": "minecraft:item/husk_spawn_egg",
        },
        "entries": [
            {
                "threshold": 2202020,
                "model": {
                    "type": "minecraft:model",
                    "model": "slimefunwarfare:general/dummy",
                },
            },
            {
                "threshold": 2202021,
                "model": {
                    "type": "minecraft:model",
                    "model": "minecraft:item/husk_spawn_egg",
                },
            },
        ],
    },
    "oversized_in_gui": True,
}

ZOMBIE_HEAD_ITEMS = {
    "model": {
        "type": "minecraft:range_dispatch",
        "property": "minecraft:custom_model_data",
        "index": 0,
        "fallback": {
            "type": "minecraft:special",
            "base": "minecraft:item/template_skull",
            "model": {"type": "minecraft:head", "kind": "zombie"},
            "transformation": VANILLA_TRANSFORM,
        },
        "entries": [
            {
                "threshold": 2201418,
                "model": {
                    "type": "minecraft:select",
                    "property": "minecraft:display_context",
                    "cases": [
                        {
                            "model": {
                                "type": "minecraft:model",
                                "model": "bump:ia_auto_gen/update_power_icon",
                            },
                            "when": ["gui", "ground", "fixed", "on_shelf"],
                        }
                    ],
                    "fallback": {
                        "type": "minecraft:model",
                        "base": "minecraft:item/template_skull",
                        "model": "bump:magical_items/update_power",
                    },
                },
            },
            {
                "threshold": 2201419,
                "model": {
                    "type": "minecraft:special",
                    "base": "minecraft:item/template_skull",
                    "model": {"type": "minecraft:head", "kind": "zombie"},
                    "transformation": VANILLA_TRANSFORM,
                },
            },
        ],
    },
    "oversized_in_gui": True,
}


def backup_once(path: Path, suffix: str) -> None:
    bak = path.with_name(path.name + suffix)
    if path.exists() and not bak.exists():
        shutil.copy2(path, bak)
        print(f"backup -> {bak.name}")
    elif bak.exists():
        print(f"backup exists: {bak.name}")


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"wrote {path.relative_to(RP)} ({path.stat().st_size} bytes)")


def main() -> None:
    if not RP.is_dir():
        raise SystemExit(f"missing resource pack: {RP}")

    # 1) Restore vanilla husk model (drop obsolete template_spawn_egg parent)
    backup_once(HUSK_MODEL, ".bak-pre-26.2-huskfix")
    write_json(HUSK_MODEL, VANILLA_HUSK_MODEL)

    # 2) Namespaced CMD dispatch for husk spawn egg (1.21.6+ overlay)
    husk_plus = OVERLAY_PLUS / "husk_spawn_egg.json"
    backup_once(husk_plus, ".bak-pre-26.2-huskfix")
    write_json(husk_plus, HUSK_ITEMS)

    # Prefer 26.2-only overlay so format-88 clients always win
    write_json(OVERLAY_26 / "husk_spawn_egg.json", HUSK_ITEMS)

    # 3) Zombie head: add skull translation + namespaced properties
    zombie_plus = OVERLAY_PLUS / "zombie_head.json"
    backup_once(zombie_plus, ".bak-pre-26.2-headfix")
    write_json(zombie_plus, ZOMBIE_HEAD_ITEMS)
    write_json(OVERLAY_26 / "zombie_head.json", ZOMBIE_HEAD_ITEMS)

    print("done — reload textures with F3+T (or re-enable the pack)")


if __name__ == "__main__":
    main()
