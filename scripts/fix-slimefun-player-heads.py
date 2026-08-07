#!/usr/bin/env python3
"""Fix Slimefun player_head corner-offset on Minecraft 26.2.

Causes:
1. Vanilla 26.2 player_head special models need transformation.translation [0.5, 0, 0.5]
2. Special head base should be minecraft:item/template_skull (not flat item/generated)
3. display_context select should use namespaced property + include on_shelf
"""

from __future__ import annotations

import json
import shutil
from pathlib import Path

RP = Path(r"D:\Minecraft\游戏主体\.minecraft\resourcepacks\Slimefun-ResourcePack")
TARGET = RP / "ia_overlay_1_21_6_plus" / "assets" / "minecraft" / "items" / "player_head.json"

VANILLA_TRANSFORMATION = {
    "left_rotation": [1.0, 0.0, 0.0, -0.0],
    "right_rotation": [0.0, 0.0, 0.0, 1.0],
    "scale": [1.0, 1.0, 1.0],
    "translation": [0.5, 0.0, 0.5],
}

TEMPLATE_SKULL = "minecraft:item/template_skull"
GOOD_BASES = {
    TEMPLATE_SKULL,
    "minecraft:item/player_head",
    "item/template_skull",
    "item/player_head",
}


def is_special(node: dict) -> bool:
    return node.get("type") in ("special", "minecraft:special")


def is_player_head_special(node: dict) -> bool:
    if not is_special(node):
        return False
    model = node.get("model")
    if not isinstance(model, dict):
        return False
    return model.get("type") in (
        "player_head",
        "minecraft:player_head",
        "head",
        "minecraft:head",
    )


def normalize_special_head(node: dict, stats: dict) -> None:
    model = node["model"]
    # Prefer explicit player_head special type
    if model.get("type") in ("head", "minecraft:head"):
        model["type"] = "minecraft:player_head"
        stats["head_type_upgraded"] += 1
    else:
        model["type"] = "minecraft:player_head"

    node["type"] = "minecraft:special"

    base = node.get("base")
    if not isinstance(base, str) or base not in GOOD_BASES:
        node["base"] = TEMPLATE_SKULL
        stats["base_fixed"] += 1
    elif not base.startswith("minecraft:"):
        node["base"] = TEMPLATE_SKULL
        stats["base_fixed"] += 1

    node["transformation"] = json.loads(json.dumps(VANILLA_TRANSFORMATION))
    stats["transform_set"] += 1


def patch(node: object, stats: dict) -> None:
    if isinstance(node, dict):
        # Namespace select / range properties
        prop = node.get("property")
        if prop == "display_context":
            node["property"] = "minecraft:display_context"
            stats["prop_display"] += 1
        elif prop == "custom_model_data":
            node["property"] = "minecraft:custom_model_data"
            stats["prop_cmd"] += 1

        # Ensure GUI cases also cover on_shelf (new in 26.x)
        if "when" in node:
            when = node["when"]
            if isinstance(when, list) and "gui" in when and "on_shelf" not in when:
                when.append("on_shelf")
                stats["on_shelf"] += 1

        if is_player_head_special(node):
            normalize_special_head(node, stats)

        # Normalize model type short names on leaf model nodes
        if node.get("type") == "model":
            node["type"] = "minecraft:model"
        elif node.get("type") == "select":
            node["type"] = "minecraft:select"
        elif node.get("type") == "range_dispatch":
            node["type"] = "minecraft:range_dispatch"

        for value in node.values():
            patch(value, stats)
    elif isinstance(node, list):
        for item in node:
            patch(item, stats)


def main() -> None:
    if not TARGET.exists():
        raise SystemExit(f"missing {TARGET}")

    bak = TARGET.with_suffix(TARGET.suffix + ".bak-pre-26.2-headfix")
    if not bak.exists():
        shutil.copy2(TARGET, bak)
        print("backup:", bak.name)
    else:
        # Prefer editing from original backup if we re-run
        print("using existing backup as source:", bak.name)
        shutil.copy2(bak, TARGET)

    data = json.loads(TARGET.read_text(encoding="utf-8"))
    stats = {
        "prop_display": 0,
        "prop_cmd": 0,
        "on_shelf": 0,
        "transform_set": 0,
        "base_fixed": 0,
        "head_type_upgraded": 0,
    }
    patch(data, stats)

    # Compact JSON to keep file smaller
    TARGET.write_text(
        json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    print("wrote", TARGET)
    print("stats:", stats)
    print("size:", TARGET.stat().st_size)
    print("Reload with F3+T")


if __name__ == "__main__":
    main()
