#!/usr/bin/env python3
import json
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(r"D:\Minecraft\服务器\26.2\plugins\待更新")
OUT.mkdir(parents=True, exist_ok=True)
CLIENT_OUT = OUT / "客户端模组_非服务端插件"
CLIENT_OUT.mkdir(exist_ok=True)

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


downloads = []

# --- mg-dev (BKCommonLib / TrainCarts / TCCoasters) ---
mg = [
    (
        "https://ci.mg-dev.eu/job/BKCommonLib/lastSuccessfulBuild/artifact/build/BKCommonLib-2.0.2-SNAPSHOT-2029.jar",
        OUT / "BKCommonLib-2.0.2-SNAPSHOT-2029.jar",
    ),
    (
        "https://ci.mg-dev.eu/job/TrainCarts/lastSuccessfulBuild/artifact/build/TrainCarts-2.0.1-SNAPSHOT-1734.jar",
        OUT / "TrainCarts-2.0.1-SNAPSHOT-1734.jar",
    ),
    (
        "https://ci.mg-dev.eu/job/TC%20Coasters/lastSuccessfulBuild/artifact/target/TCCoasters-2.0.1-SNAPSHOT-408.jar",
        OUT / "TCCoasters-2.0.1-SNAPSHOT-408.jar",
    ),
]

# SmoothCoasters is a CLIENT mod (Fabric/NeoForge), not a Bukkit plugin
mg_client = [
    (
        "https://ci.mg-dev.eu/job/SmoothCoasters/lastSuccessfulBuild/artifact/fabric/build/libs/SmoothCoasters-Fabric-26.2-v2-SNAPSHOT.jar",
        CLIENT_OUT / "SmoothCoasters-Fabric-26.2-v2-SNAPSHOT.jar",
    ),
]

# --- FakePlayer: original tanyaofei has no 26.2; provide FPP alternative ---
try:
    fpp_versions = get_json(
        "https://api.modrinth.com/v2/project/fake-player-plugin-(fpp)/version?"
        + urllib.parse.urlencode({"game_versions": '["26.2"]', "limit": "1"})
    )
    if fpp_versions:
        file0 = fpp_versions[0]["files"][0]
        downloads.append(
            (
                file0["url"],
                OUT / file0["filename"],
                "fakeplayer 替代品 FPP（原 fakeplayer-0.3.19 无 26.2 版）",
            )
        )
except Exception as e:
    print("FPP lookup failed:", e)

# --- Citizens family via Modrinth/Hangar/known mirrors ---
# Try Modrinth project search
for slug_guess in ["citizens", "citizens-npc", "denizen", "sentinel"]:
    try:
        meta = get_json(f"https://api.modrinth.com/v2/project/{slug_guess}")
        print("Modrinth project", slug_guess, "->", meta.get("title"))
    except Exception as e:
        print("no modrinth", slug_guess, type(e).__name__)

# Try Hangar project pages
for ns, proj in [
    ("Citizens", "Citizens"),
    ("DenizenScript", "Denizen"),
    ("Citizens", "Sentinel"),
]:
    try:
        data = get_json(f"https://hangar.papermc.io/api/v1/projects/{ns}/{proj}")
        print("Hangar", ns, proj, data.get("name"))
    except Exception as e:
        print("no hangar", ns, proj, e)

# Citizens CI direct (try several build numbers)
citizens_urls = []
for build in range(4230, 4210, -1):
    citizens_urls.append(
        (
            f"https://ci.citizensnpcs.co/job/Citizens2/{build}/artifact/net.citizensnpcs/citizens/2.0.43-SNAPSHOT/citizens-2.0.43-SNAPSHOT.jar",
            OUT / f"Citizens-2.0.43-SNAPSHOT-b{build}.jar",
        )
    )

denizen_urls = []
for build in range(1900, 1800, -1):
    denizen_urls.append(
        (
            f"https://ci.citizensnpcs.co/job/Denizen/{build}/artifact/target/Denizen-1.3.1-b{build}-DEV.jar",
            OUT / f"Denizen-b{build}.jar",
        )
    )

# Write README for anything that fails
notes = []

print("\n=== Downloading mg-dev artifacts ===")
for url, dest in mg:
    ok = download(url, dest)
    notes.append((dest.name, "OK" if ok else "FAIL", url))

print("\n=== Downloading SmoothCoasters CLIENT mod ===")
for url, dest in mg_client:
    ok = download(url, dest)
    notes.append((dest.name, "CLIENT-OK" if ok else "FAIL", url))

print("\n=== Downloading FPP ===")
for url, dest, note in downloads:
    ok = download(url, dest)
    notes.append((f"{dest.name} ({note})", "OK" if ok else "FAIL", url))

print("\n=== Trying Citizens CI builds ===")
citizens_ok = False
for url, dest in citizens_urls[:15]:
    if download(url, dest):
        citizens_ok = True
        notes.append((dest.name, "OK", url))
        break
if not citizens_ok:
    notes.append(("Citizens", "NEED_MANUAL", "https://ci.citizensnpcs.co/job/Citizens2/"))

# Try alternate Denizen URL patterns quickly with HEAD on latest known
print("\n=== Trying Denizen known patterns ===")
denizen_patterns = [
    "https://ci.citizensnpcs.co/job/Denizen/lastSuccessfulBuild/artifact/target/Denizen.jar",
    "https://ci.citizensnpcs.co/job/Denizen_Developmental/lastSuccessfulBuild/artifact/target/Denizen.jar",
]
denizen_ok = False
for url in denizen_patterns:
    dest = OUT / "Denizen-latest.jar"
    if download(url, dest):
        denizen_ok = True
        notes.append((dest.name, "OK", url))
        break
if not denizen_ok:
    notes.append(("Denizen", "NEED_MANUAL", "https://ci.citizensnpcs.co/job/Denizen/"))
    notes.append(("Sentinel", "NEED_MANUAL", "https://ci.citizensnpcs.co/job/Sentinel/"))
    notes.append(("Depenizen", "NEED_MANUAL", "https://ci.citizensnpcs.co/job/Depenizen/"))

readme = OUT / "README_下载说明.txt"
lines = [
    "待更新插件下载结果",
    "==================",
    "",
]
for name, status, url in notes:
    lines.append(f"[{status}] {name}")
    lines.append(f"  {url}")
    lines.append("")
lines += [
    "说明：",
    "1. AxiomPaper 你已自行升级，未下载。",
    "2. SmoothCoasters 官方产物是 Fabric/NeoForge 客户端模组，不是服务端插件；",
    "   已放到「客户端模组_非服务端插件」子目录。原 plugins 里的 SmoothCoasters-*.jar 应移除。",
    "3. 原 fakeplayer-0.3.19（tanyaofei）暂无官方 26.2 版；已下载兼容 26.2 的 FPP 作为替代。",
    "4. Citizens / Denizen / Sentinel / Depenizen 若标记 NEED_MANUAL，请浏览器打开 CI 手动下载：",
    "   https://ci.citizensnpcs.co/job/Citizens2/",
    "   https://ci.citizensnpcs.co/job/Denizen/",
    "   https://ci.citizensnpcs.co/job/Sentinel/",
    "   https://ci.citizensnpcs.co/job/Depenizen/",
    "",
    "替换步骤：停服 → 旧 jar 移到 plugins/旧版备份 → 新 jar 移入 plugins/ → 开服。",
]
readme.write_text("\n".join(lines), encoding="utf-8")
print("\nWrote", readme)
print("Done.")
