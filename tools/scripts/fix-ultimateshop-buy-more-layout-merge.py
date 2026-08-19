#!/usr/bin/env python3
"""Remove only menu-settings.layout from UltimateShop shop YAMLs."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHOPS = ROOT / "plugins" / "UltimateShop" / "shops"
MENUS = ROOT / "plugins" / "UltimateShop" / "menus"
EXAMPLE_MENU = MENUS / "example-shop-menu.yml"

CUSTOM_LAYOUT_SHOPS = {
    "building__p10.yml",
    "colored__p5.yml",
    "combat__p2.yml",
    "enchantments__p3.yml",
    "ingredients__p4.yml",
    "mcwws.yml",
    "natural__p6.yml",
    "redstone__p2.yml",
    "tools__p4.yml",
}


def remove_menu_settings_layout(text: str) -> tuple[str, list[str] | None]:
    lines = text.splitlines()
    out: list[str] = []
    in_menu_settings = False
    menu_settings_indent = 0
    skipping_layout = False
    layout_indent = 0
    captured_layout: list[str] = []

    for line in lines:
        stripped = line.lstrip()
        indent = len(line) - len(stripped)

        if re.fullmatch(r"menu-settings:\s*", stripped):
            in_menu_settings = True
            menu_settings_indent = indent
            out.append(line)
            continue

        if in_menu_settings and indent <= menu_settings_indent and stripped and not stripped.startswith("#"):
            in_menu_settings = False
            skipping_layout = False

        if in_menu_settings and re.fullmatch(r"layout:\s*", stripped):
            skipping_layout = True
            layout_indent = indent
            continue

        if skipping_layout:
            if stripped.startswith("- ") and indent > layout_indent:
                captured_layout.append(stripped[2:].strip().strip("'\""))
                continue
            skipping_layout = False

        out.append(line)

    result = "\n".join(out)
    if text.endswith("\n"):
        result += "\n"
    return result, (captured_layout or None)


def create_custom_menu(layout_lines: list[str]) -> str:
    template = EXAMPLE_MENU.read_text(encoding="utf-8")
    layout_yaml = "\n".join(f"  - '{line}'" for line in layout_lines)
    return re.sub(
        r"(?ms)^layout:\n(?:  - .+\n)+",
        f"layout:\n{layout_yaml}\n",
        template,
        count=1,
    )


def main() -> None:
    stripped: list[str] = []
    created: list[str] = []

    for shop_file in sorted(SHOPS.glob("*.yml")):
        original = shop_file.read_text(encoding="utf-8")
        updated, layout_lines = remove_menu_settings_layout(original)
        if layout_lines is None:
            continue

        name = shop_file.name
        if name in CUSTOM_LAYOUT_SHOPS:
            menu_name = f"shop-menu-{shop_file.stem}"
            menu_path = MENUS / f"{menu_name}.yml"
            menu_path.write_text(create_custom_menu(layout_lines), encoding="utf-8")
            updated = re.sub(
                r"(?m)^(\s*)menu:\s*example-shop-menu\s*$",
                rf"\1menu: {menu_name}",
                updated,
                count=1,
            )
            created.append(menu_path.name)

        if updated != original:
            shop_file.write_text(updated, encoding="utf-8")
            stripped.append(name)

    print(f"Removed menu-settings.layout from {len(stripped)} shop files.")
    if created:
        print("Created custom shop menus:")
        for item in created:
            print(f"  - {item}")


if __name__ == "__main__":
    main()
