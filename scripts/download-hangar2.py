#!/usr/bin/env python3
import json
import urllib.request
from pathlib import Path

OUT = Path(r"D:\Minecraft\服务器\26.2\plugins\待更新")
UA = {"User-Agent": "Mozilla/5.0 (compatible; MCWWS-updater/1.0)"}


def get_json(url: str):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def download(url: str, dest: Path) -> bool:
    print(f"GET {url}")
    try:
        req = urllib.request.Request(url, headers=UA)
        with urllib.request.urlopen(req, timeout=180) as r:
            data = r.read()
        if len(data) < 1000:
            print(f"  TOO SMALL ({len(data)})")
            return False
        dest.write_bytes(data)
        print(f"  OK {dest.name} ({len(data)} bytes)")
        return True
    except Exception as e:
        print(f"  FAIL {e}")
        return False


projects = [
    ("DenizenScript", "Denizen"),
    ("Citizens", "Sentinel"),
    ("Citizens", "Citizens"),
    ("fullwall", "Citizens"),
    ("CitizensNPCs", "Citizens"),
    ("DenizenScript", "Depenizen"),
]

for owner, slug in projects:
    print(f"\n=== {owner}/{slug} ===")
    try:
        proj = get_json(f"https://hangar.papermc.io/api/v1/projects/{owner}/{slug}")
        print(" project ok:", proj.get("name"))
    except Exception as e:
        print(" project fail:", e)
        continue

    try:
        vers = get_json(
            f"https://hangar.papermc.io/api/v1/projects/{owner}/{slug}/versions?limit=8&platform=PAPER"
        )
    except Exception as e:
        print(" versions fail:", e)
        continue

    for v in vers.get("result") or []:
        name = v.get("name")
        downloads = v.get("downloads") or {}
        print(" version", name, "platforms", list(downloads.keys()))
        paper = downloads.get("PAPER")
        if not paper:
            continue
        url = paper.get("downloadUrl") or paper.get("externalUrl")
        file_info = paper.get("fileInfo") or {}
        fname = file_info.get("name") or f"{slug}-{name}.jar"
        # externalUrl might point to CI
        if url:
            if download(url, OUT / fname):
                break
        hangar_dl = (
            f"https://hangar.papermc.io/api/v1/projects/{owner}/{slug}/versions/"
            f"{name}/PAPER/download"
        )
        if download(hangar_dl, OUT / fname):
            break

print("\nListing OUT:")
for p in sorted(OUT.iterdir()):
    if p.is_file():
        print(f"  {p.name} {p.stat().st_size}")
