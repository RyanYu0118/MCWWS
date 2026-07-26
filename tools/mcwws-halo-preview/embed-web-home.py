#!/usr/bin/env python3
"""Copy web/public/home.html to wiki/demo with inlined CSS/JS for Halo tests."""
from __future__ import annotations

import re
import sys
from pathlib import Path

# Halo html-edited import can break on emoji; map known UI glyphs to plain text.
_HALO_EMOJI_REPLACEMENTS = (
    ("🏗️", "建"),
    ("🗺️", "图"),
    ("⚙️", "管"),
    ("☀️", "日"),
    ("🛒", "购"),
    ("🏗", "建"),
    ("🗺", "图"),
    ("⚙", "管"),
    ("🌙", "月"),
    ("☀", "日"),
)

_EMOJI_RE = re.compile(
    "["
    "\U0001F600-\U0001F64F"
    "\U0001F300-\U0001F5FF"
    "\U0001F680-\U0001F6FF"
    "\U0001F1E0-\U0001F1FF"
    "\U00002702-\U000027B0"
    "\U0001F900-\U0001F9FF"
    "\U0001FA00-\U0001FA6F"
    "\U0001FA70-\U0001FAFF"
    "\U00002600-\U000026FF"
    "]+",
    flags=re.UNICODE,
)


def halo_safe_text(text: str) -> str:
    for old, new in _HALO_EMOJI_REPLACEMENTS:
        text = text.replace(old, new)
    text = _EMOJI_RE.sub("", text)
    return text.replace("\ufe0f", "").replace("\u200d", "")


def main() -> int:
    repo = Path(__file__).resolve().parents[2]
    pub = repo / "plugins" / "Skript" / "scripts" / "web" / "public"
    out = repo / "wiki" / "demo" / "web-public-home.html"

    style = (pub / "style.css").read_text(encoding="utf-8")
    themes = (pub / "themes.css").read_text(encoding="utf-8")
    theme_js = (pub / "mcwws-theme.js").read_text(encoding="utf-8")
    transition_js = (pub / "mcwws-page-transition.js").read_text(encoding="utf-8")
    config_js = (pub / "services-config.js").read_text(encoding="utf-8")

    style = re.sub(
        r"@font-face\s*\{[^}]*\}",
        "/* MinecraftFont @font-face omitted in Halo embed */",
        style,
        count=1,
        flags=re.DOTALL,
    )

    if ".container {" not in style and ".container\n" not in style:
        style += "\n.container { width: 100%; max-width: 1100px; margin: 0 auto; }\n"

    body_html = (pub / "home.html").read_text(encoding="utf-8")
    # Extract body inner (between <body...> and </body>)
    m = re.search(r"<body[^>]*>(.*)</body>", body_html, re.DOTALL | re.I)
    if not m:
        print("Could not parse home.html body", file=sys.stderr)
        return 1
    body_inner = m.group(1).strip()
    # Drop external script tags; JS inlined below
    body_inner = re.sub(r'\s*<script[^>]+src="[^"]+"[^>]*></script>\s*', "\n", body_inner)
    # Absolute links for Halo (same host as shop web)
    base = "https://www.ryanstudio.work"
    body_inner = body_inner.replace('href="items.html"', f'href="{base}/items.html" target="_blank" rel="noopener"')
    body_inner = body_inner.replace('href="build.html"', f'href="{base}/build.html" target="_blank" rel="noopener"')
    body_inner = body_inner.replace('href="map.html"', f'href="{base}/map.html" target="_blank" rel="noopener"')
    body_inner = body_inner.replace(
        'href="manage/shop-locations.html"',
        f'href="{base}/manage/shop-locations.html" target="_blank" rel="noopener"',
    )

    page = f"""<!--
  MCWWS: copy of plugins/Skript/scripts/web/public/home.html
  Inlined style.css + themes.css (+ JS) for Halo HTML / JSON import test.
  Emoji stripped / replaced for Halo html-edited import compatibility.
  Regenerate: python tools/mcwws-halo-preview/embed-web-home.py
-->
<div class="html-edited mcwws-web-public-home-root">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
<style>
/* === style.css (inlined) === */
{style}
</style>
<style>
/* === themes.css (inlined) === */
{themes}
</style>
<div class="services-hub-page" style="min-height:auto;">
{body_inner}
</div>
<script>
/* mcwws-theme.js */
{theme_js}
</script>
<script>
/* mcwws-page-transition.js */
{transition_js}
</script>
<script>
/* services-config.js */
{config_js}
</script>
</div>
"""

    page = halo_safe_text(page)
    out.write_text(page, encoding="utf-8")
    print(out)
    print(f"{out.stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
