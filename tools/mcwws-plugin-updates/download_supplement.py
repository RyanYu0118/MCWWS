#!/usr/bin/env python3
"""Download plugins missing from automatic scan (EssentialsX GeoIP, floodgate, etc.)."""
from __future__ import annotations

import json
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
OUT = REPO / "plugins" / "待更新"


def download(url: str, dest: Path) -> int:
    req = urllib.request.Request(url, headers={"User-Agent": "MCWWS-PluginUpdate/1.0"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        dest.write_bytes(resp.read())
    return dest.stat().st_size


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    items = [
        (
            "EssentialsXGeoIP-2.22.0.jar",
            "https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsXGeoIP-2.22.0.jar",
        ),
    ]
    for fname, url in items:
        dest = OUT / fname
        if dest.exists() and dest.stat().st_size > 1000:
            print(f"SKIP {fname} (exists)")
            continue
        size = download(url, dest)
        print(f"OK {fname} ({size} bytes)")

    api = "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest"
    req = urllib.request.Request(api, headers={"User-Agent": "MCWWS-PluginUpdate/1.0", "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode())
    version = data.get("version", "2.2.6")
    build = data.get("build", "?")
    downloads = data.get("downloads") or {}
    fname = "floodgate-spigot.jar"
    if isinstance(downloads, dict) and isinstance(downloads.get("spigot"), dict):
        fname = downloads["spigot"].get("name") or fname
    spigot_url = f"https://download.geysermc.org/v2/projects/floodgate/versions/{version}/builds/{build}/downloads/spigot"
    # GeyserMC 仅发布 floodgate-spigot 构件，Paper 服务端同样使用该 jar。
    if spigot_url:
        dest = OUT / f"floodgate-spigot-{version}-b{build}.jar"
        if not (dest.exists() and dest.stat().st_size > 1000):
            size = download(spigot_url, dest)
            print(f"OK {dest.name} ({size} bytes)")
        else:
            print(f"SKIP {dest.name} (exists)")
    else:
        print("FAIL floodgate: no spigot download in API response")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
