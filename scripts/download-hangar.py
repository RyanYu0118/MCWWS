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


# Search Hangar
for q in ["Citizens", "Denizen", "Sentinel", "Depenizen"]:
    data = get_json(f"https://hangar.papermc.io/api/v1/projects?q={q}&limit=10&priority=relevance")
    print("===", q)
    for p in data.get("result", []):
        ns = p.get("namespace", {})
        print(f"  {ns.get('owner')}/{ns.get('slug')} name={p.get('name')}")

# Download from known Hangar projects
projects = [
    ("DenizenScript", "Denizen"),
    ("Citizens", "Sentinel"),
    ("Citizens", "Citizens"),
    ("fullwall", "Citizens"),
    ("CitizensNPCs", "Citizens"),
]

for owner, slug in projects:
    try:
        vers = get_json(
            f"https://hangar.papermc.io/api/v1/projects/{owner}/{slug}/versions"
            f"?limit=5&platform=PAPER&includeChannelName=true"
        )
        print(f"\nVERSIONS {owner}/{slug}:")
        result = vers.get("result") or []
        for v in result[:5]:
            name = v.get("name")
            print(" ", name, v.get("channel", {}).get("name"), list((v.get("downloads") or {}).keys()))
            downloads = v.get("downloads") or {}
            paper = downloads.get("PAPER") or downloads.get("Paper")
            if not paper:
                continue
            # Prefer external URL or downloadUrl
            url = paper.get("downloadUrl") or paper.get("externalUrl")
            file_info = paper.get("fileInfo") or {}
            fname = file_info.get("name") or f"{slug}-{name}.jar"
            if url:
                download(url, OUT / fname)
                break
            # Hangar download endpoint
            hangar_dl = (
                f"https://hangar.papermc.io/api/v1/projects/{owner}/{slug}/versions/"
                f"{name}/PAPER/download"
            )
            if download(hangar_dl, OUT / fname):
                break
    except Exception as e:
        print(f"skip {owner}/{slug}: {e}")

print("\nDone hangar pass.")
