#!/usr/bin/env python3
"""Download Paper-preferred jars, fix URL-encoded filenames, remove obsolete duplicates."""
from __future__ import annotations

import sys
import urllib.parse
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
OUT = REPO / "plugins" / "待更新"

# (filename, url) — Paper or Paper-compatible official builds
PAPER_DOWNLOADS = [
    (
        "bluemap-5.16-paper.jar",
        "https://cdn.modrinth.com/data/swbUV1cr/versions/Vb2ZE8bR/bluemap-5.16-paper.jar",
    ),
]

# Old artifacts superseded by Paper builds above or re-download
OBSOLETE = [
    "bluemap-5.16-spigot.jar",
    "CommandAPI-12.0.0-Spigot.jar",
    "FastAsyncWorldEdit-Bukkit-2.15.3.jar",
]


def download(url: str, dest: Path) -> int:
    req = urllib.request.Request(url, headers={"User-Agent": "MCWWS-PluginUpdate/1.0"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        dest.write_bytes(resp.read())
    return dest.stat().st_size


def decode_jar_name(name: str) -> str:
    return urllib.parse.unquote(name)


def rename_encoded_jars() -> None:
    for path in sorted(OUT.glob("*.jar")):
        decoded = decode_jar_name(path.name)
        if decoded == path.name:
            continue
        dest = path.with_name(decoded)
        if dest.exists():
            path.unlink()
            print(f"DEL duplicate encoded {path.name}")
        else:
            path.rename(dest)
            print(f"REN {path.name} -> {decoded}")


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    rename_encoded_jars()
    for fname, url in PAPER_DOWNLOADS:
        dest = OUT / fname
        if dest.exists() and dest.stat().st_size > 1000:
            print(f"SKIP {fname}")
            continue
        size = download(url, dest)
        print(f"OK {fname} ({size} bytes)")

    for name in OBSOLETE:
        path = OUT / name
        if path.exists():
            path.unlink()
            print(f"DEL {name}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
