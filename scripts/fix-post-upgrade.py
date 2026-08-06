#!/usr/bin/env python3
"""Clean stale player data after 1.21.11 -> 26.2 upgrade. Run while server is STOPPED."""

from __future__ import annotations

import json
import shutil
from pathlib import Path

from nbtlib import load
from nbtlib.tag import List, String

ROOT = Path(__file__).resolve().parents[1]
WORLD = ROOT / "world"

# Known removed / invalid IDs from latest.log and datapack changes
REMOVED_RECIPES = {
    "terralith:dispenser_alt",
}

REMOVED_ADVANCEMENT_PREFIXES = (
    "incendium:",
)

REMOVED_ADVANCEMENTS = {
    "minecraft:recipes/decorations/mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting",
}


def clean_advancements() -> int:
    adv_dir = WORLD / "players" / "advancements"
    if not adv_dir.is_dir():
        return 0

    removed = 0
    for path in adv_dir.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        keys = list(data.keys())
        changed = False
        for key in keys:
            if key in REMOVED_ADVANCEMENTS or key.startswith(REMOVED_ADVANCEMENT_PREFIXES):
                del data[key]
                removed += 1
                changed = True
        if changed:
            shutil.copy(path, path.with_suffix(".json.bak"))
            path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    return removed


def clean_recipe_books() -> int:
    data_dir = WORLD / "players" / "data"
    if not data_dir.is_dir():
        return 0

    removed = 0
    for path in data_dir.glob("*.dat"):
        try:
            nbt = load(path)
        except TypeError:
            # Empty or invalid player data file — skip
            continue
        book = nbt.get("recipeBook")
        if book is None or "recipes" not in book:
            continue

        recipes = list(book["recipes"])
        new_recipes = [r for r in recipes if str(r) not in REMOVED_RECIPES]
        if len(new_recipes) == len(recipes):
            continue

        removed += len(recipes) - len(new_recipes)
        book["recipes"] = List[String]([String(str(r)) for r in new_recipes])
        shutil.copy(path, path.with_suffix(".dat.bak"))
        nbt.save()
    return removed


def main() -> None:
    lock = WORLD / "session.lock"
    if lock.exists():
        print("WARNING: world/session.lock exists — stop the server before running this script.")

    adv_removed = clean_advancements()
    recipe_removed = clean_recipe_books()
    print(f"Removed {adv_removed} stale advancement entries")
    print(f"Removed {recipe_removed} stale recipe book entries")
    print("Done. Backups: *.json.bak and *.dat.bak next to edited files")


if __name__ == "__main__":
    main()
