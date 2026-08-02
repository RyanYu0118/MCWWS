#!/usr/bin/env python3
"""Scan plugins/*.jar, check known Modrinth/Hangar sources, download updates to plugins/待更新."""
from __future__ import annotations

import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
PLUGINS = REPO / "plugins"
OUT = PLUGINS / "待更新"
SOURCES_FILE = Path(__file__).with_name("plugin-sources.json")
REPORT = OUT / "update-report.json"
SUMMARY = OUT / "update-report.txt"

# plugin.yml name (lower) -> source
DEFAULT_SOURCES: dict[str, dict] = {
    "luckperms": {"platform": "modrinth", "slug": "luckperms"},
    "essentials": {"platform": "modrinth", "slug": "essentialsx"},
    "essentialsx": {"platform": "modrinth", "slug": "essentialsx"},
    "essentialschat": {"platform": "modrinth", "slug": "essentialsx-chat-module"},
    "essentialsxchat": {"platform": "modrinth", "slug": "essentialsx-chat-module"},
    "essentialsspawn": {"platform": "modrinth", "slug": "essentialsx-spawn"},
    "essentialsxspawn": {"platform": "modrinth", "slug": "essentialsx-spawn"},
    "vault": {"platform": "modrinth", "slug": "vault"},
    "placeholderapi": {"platform": "modrinth", "slug": "placeholderapi"},
    "worldguard": {"platform": "modrinth", "slug": "worldguard"},
    "fastasyncworldedit": {"platform": "modrinth", "slug": "fastasyncworldedit"},
    "geyser-spigot": {"platform": "modrinth", "slug": "geyser"},
    "floodgate": {"platform": "modrinth", "slug": "floodgate"},
    "skript": {"platform": "modrinth", "slug": "skript"},
    "skript-placeholders": {"platform": "modrinth", "slug": "skript-placeholders"},
    "skript-reflect": {"platform": "modrinth", "slug": "skript-reflect"},
    "skript-yaml": {"platform": "modrinth", "slug": "skript-yaml"},
    "protocolib": {"platform": "modrinth", "slug": "protocollib"},
    "citizens": {"platform": "hangar", "author": "Citizens", "slug": "Citizens"},
    "denizen": {"platform": "modrinth", "slug": "denizen"},
    "bluemap": {"platform": "modrinth", "slug": "bluemap"},
    "train_carts": {"platform": "hangar", "author": "TrainCarts", "slug": "TrainCarts"},
    "bkcommonlib": {"platform": "hangar", "author": "TrainCarts", "slug": "BKCommonLib"},
    "smoothcoasters": {"platform": "hangar", "author": "TrainCarts", "slug": "SmoothCoasters"},
    "tccoasters": {"platform": "hangar", "author": "TrainCarts", "slug": "TCCoasters"},
    "gsit": {"platform": "modrinth", "slug": "gsit"},
    "playerparticles": {"platform": "modrinth", "slug": "playerparticles"},
    "inventoryrollbackplus": {"platform": "modrinth", "slug": "inventoryrollbackplus"},
    "decentholograms": {"platform": "modrinth", "slug": "decentholograms"},
    "ultimateshop": {"platform": "modrinth", "slug": "ultimateshop"},
    "ultimatetimber": {"platform": "modrinth", "slug": "ultimatetimber"},
    "veinminer": {"platform": "modrinth", "slug": "veinminer-bukkit"},
    "itemedit": {"platform": "modrinth", "slug": "itemedit"},
    "chestsort": {"platform": "modrinth", "slug": "chestsort"},
    "griefprevention": {"platform": "modrinth", "slug": "griefprevention"},
    "commandapi": {"platform": "modrinth", "slug": "commandapi"},
    "fakeplayer": {"platform": "modrinth", "slug": "fakeplayer"},
    "ajleaderboards": {"platform": "modrinth", "slug": "ajleaderboards"},
    "litesignin": {"platform": "modrinth", "slug": "litesignin"},
    "servervariables": {"platform": "modrinth", "slug": "servervariables"},
    "bankplus": {"platform": "modrinth", "slug": "bankplus"},
    "banitem": {"platform": "modrinth", "slug": "banitem"},
    "setspawn": {"platform": "modrinth", "slug": "setspawn"},
    "pluginportal": {"platform": "modrinth", "slug": "pluginportal"},
    "pluginupdater": {"platform": "modrinth", "slug": "pluginupdater"},
    "infinitecoreboard": {"platform": "modrinth", "slug": "infinitescoreboard"},
    "infinitescoreboard": {"platform": "modrinth", "slug": "infinitescoreboard"},
    "commandprompter": {"platform": "modrinth", "slug": "commandprompter"},
    "commandtimer": {"platform": "modrinth", "slug": "commandtimer"},
    "item-nbt-api": {"platform": "modrinth", "slug": "item-nbt-api"},
    "nbt-api": {"platform": "modrinth", "slug": "item-nbt-api"},
    "coreprotect": {"platform": "modrinth", "slug": "coreprotect"},
    "worldedit": {"platform": "hangar", "author": "IntellectualSites", "slug": "FastAsyncWorldEdit"},
}

SKIP_JAR_PREFIX = ("MCWWS_", "citizensapi", "denizencore", "VaultAPI", "wolfyutils")


@dataclass
class PluginInfo:
    jar: str
    name: str
    version: str


@dataclass
class UpdateHit:
    jar: str
    name: str
    current: str
    latest: str
    platform: str
    project: str
    download_url: str
    filename: str
    status: str
    note: str = ""


def http_json(url: str, timeout: int = 15) -> dict | list | None:
    req = urllib.request.Request(url, headers={"User-Agent": "MCWWS-PluginUpdate/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, json.JSONDecodeError):
        return None


def http_download(url: str, dest: Path, timeout: int = 180) -> bool:
    req = urllib.request.Request(url, headers={"User-Agent": "MCWWS-PluginUpdate/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            dest.write_bytes(resp.read())
        return dest.stat().st_size > 1000
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError):
        return False


def read_plugin_yml(jar_path: Path) -> tuple[str, str] | None:
    try:
        with zipfile.ZipFile(jar_path) as zf:
            yml_name = next((n for n in zf.namelist() if n.endswith("paper-plugin.yml")), None)
            if yml_name is None:
                yml_name = next((n for n in zf.namelist() if n.endswith("plugin.yml")), None)
            if yml_name is None:
                return None
            text = zf.read(yml_name).decode("utf-8", errors="replace")
    except (zipfile.BadZipFile, OSError):
        return None

    name_m = re.search(r"^name:\s*['\"]?(.+?)['\"]?\s*$", text, re.M)
    ver_m = re.search(r"^version:\s*['\"]?(.+?)['\"]?\s*(?:#.*)?$", text, re.M)
    if not name_m or not ver_m:
        return None
    name = name_m.group(1).strip()
    version = ver_m.group(1).strip().strip('"').strip("'")
    version = version.replace("${project.version}", "?")
    version = re.sub(r"\s+#.*$", "", version).strip()
    return name, version


def normalize_version(version: str) -> tuple:
    if not version or version == "?":
        return ()
    if re.search(r"git|build|snapshot|beta|dev|rel|\$|\?", version, re.I):
        return ()
    parts = re.split(r"[.\-_+]", version)
    out: list = []
    for p in parts:
        m = re.match(r"(\d+)", p)
        if m:
            out.append(int(m.group(1)))
        elif p.isdigit():
            out.append(int(p))
        else:
            break
    return tuple(out) if out else ()


def is_newer(current: str, latest: str) -> bool | None:
    c = normalize_version(current)
    l = normalize_version(latest)
    if not c or not l:
        return None
    return l > c


def modrinth_latest(slug: str) -> tuple[str, str] | None:
    enc = urllib.parse.quote(slug)
    data = http_json(
        f"https://api.modrinth.com/v2/project/{enc}/version"
        f"?loaders=[%22paper%22,%22bukkit%22,%22spigot%22]"
        f"&game_versions=[%221.21.11%22,%221.21.10%22,%221.21%22]"
    )
    if not isinstance(data, list) or not data:
        data = http_json(f"https://api.modrinth.com/v2/project/{enc}/version")
    if not isinstance(data, list) or not data:
        return None
    ver = data[0]
    files = ver.get("files") or []
    if not files:
        return None
    return ver.get("version_number", ""), files[0].get("url", "")


def hangar_latest(author: str, slug: str) -> tuple[str, str] | None:
    a, s = urllib.parse.quote(author), urllib.parse.quote(slug)
    # /latestrelease is deprecated/unreliable; use versions list instead.
    data = http_json(
        f"https://hangar.papermc.io/api/v1/projects/{a}/{s}/versions"
        f"?limit=10&offset=0&platform=PAPER"
    )
    if not isinstance(data, dict):
        return None
    for ver in data.get("result") or []:
        channel = (ver.get("channel") or {}).get("name", "")
        if channel and channel.lower() not in ("release", "stable"):
            continue
        ver_name = ver.get("name") or ""
        if not ver_name:
            continue
        detail = http_json(
            f"https://hangar.papermc.io/api/v1/projects/{a}/{s}/versions/{urllib.parse.quote(ver_name, safe='')}"
        )
        if not isinstance(detail, dict):
            continue
        downloads = detail.get("downloads") or {}
        url = None
        for key in ("PAPER", "PAPER_PLUGIN"):
            entry = downloads.get(key)
            if isinstance(entry, dict):
                url = entry.get("downloadUrl") or entry.get("externalUrl")
            elif isinstance(entry, str) and entry.endswith(".jar"):
                url = entry
            if url:
                break
        if not url:
            for entry in downloads.values():
                if isinstance(entry, dict):
                    url = entry.get("downloadUrl") or entry.get("externalUrl")
                elif isinstance(entry, str) and entry.endswith(".jar"):
                    url = entry
                if url:
                    break
        if url:
            if url.startswith("/"):
                url = "https://hangar.papermc.io" + url
            return ver_name, url
    return None


def pick_filename(url: str, jar: str, latest: str) -> str:
    base = Path(urllib.parse.urlparse(url).path).name
    if base.endswith(".jar"):
        return base
    stem = Path(jar).stem.split("-")[0]
    safe = re.sub(r"[^\w.\-+]", "_", latest)
    return f"{stem}-{safe}.jar"


def load_sources() -> dict[str, dict]:
    src = dict(DEFAULT_SOURCES)
    if SOURCES_FILE.exists():
        extra = json.loads(SOURCES_FILE.read_text(encoding="utf-8"))
        for k, v in extra.items():
            src[k.lower()] = v
    return src


def check_plugin(info: PluginInfo, sources: dict[str, dict]) -> UpdateHit:
    if info.jar.startswith(SKIP_JAR_PREFIX):
        return UpdateHit(info.jar, info.name, info.version, "", "", "", "", "", "skip", "MCWWS/依赖 jar")

    key = info.name.lower().replace(" ", "")
    src = sources.get(key)
    if not src:
        return UpdateHit(info.jar, info.name, info.version, "", "", "", "", "", "manual", "未配置更新源（Slimefun 附属/Spigot 付费等）")

    platform = src["platform"]
    latest = url = project = ""
    if platform == "modrinth":
        slug = src["slug"]
        got = modrinth_latest(slug)
        project = slug
        if got:
            latest, url = got
    elif platform == "hangar":
        got = hangar_latest(src["author"], src["slug"])
        project = f"{src['author']}/{src['slug']}"
        if got:
            latest, url = got

    if not latest or not url:
        return UpdateHit(info.jar, info.name, info.version, "", platform, project, "", "", "error", "API 未返回版本")

    newer = is_newer(info.version, latest)
    if newer is True:
        status = "update"
    elif newer is False:
        status = "same"
    else:
        status = "review"
        note = "版本号非纯数字，请人工确认"
        return UpdateHit(info.jar, info.name, info.version, latest, platform, project, url, pick_filename(url, info.jar, latest), status, note)

    fname = pick_filename(url, info.jar, latest)
    return UpdateHit(info.jar, info.name, info.version, latest, platform, project, url, fname, status, "")


def main() -> int:
    download = "--download" in sys.argv
    OUT.mkdir(parents=True, exist_ok=True)
    sources = load_sources()

    infos: list[PluginInfo] = []
    for jar in sorted(PLUGINS.glob("*.jar")):
        meta = read_plugin_yml(jar)
        if meta is None:
            infos.append(PluginInfo(jar.name, jar.stem, "?"))
        else:
            infos.append(PluginInfo(jar.name, meta[0], meta[1]))

    results: list[UpdateHit] = []
    updates: list[UpdateHit] = []
    for i, info in enumerate(infos):
        hit = check_plugin(info, sources)
        results.append(hit)
        if hit.status == "update":
            updates.append(hit)
        print(f"[{i+1}/{len(infos)}] {info.name}: {info.version} -> {hit.status} {hit.latest or '-'}", flush=True)
        time.sleep(0.05)

    manual = [h for h in results if h.status == "manual"]
    review = [h for h in results if h.status == "review"]
    same = [h for h in results if h.status == "same"]

    lines = [
        "MCWWS 插件更新报告（仅已配置 Modrinth/Hangar 源）",
        f"扫描: {len(infos)} 个 jar",
        f"可自动更新: {len(updates)}",
        f"已是最新: {len(same)}",
        f"需人工确认: {len(review)}",
        f"无更新源/Slimefun 等: {len(manual)}",
        "",
        "=== 可下载更新 ===",
    ]
    for h in updates:
        lines.append(f"{h.name} | {h.current} -> {h.latest} | {h.platform}:{h.project} | {h.filename}")

    lines.extend(["", "=== 建议人工查看（有新版但版本格式特殊）==="])
    for h in review:
        lines.append(f"{h.name} | {h.current} -> {h.latest} | {h.note}")

    lines.extend(["", "=== 未配置源（需 Spigot/Slimefun 构建站/手动）==="])
    for h in manual[:50]:
        lines.append(f"{h.name} | {h.current} | {h.jar}")
    if len(manual) > 50:
        lines.append(f"... 另有 {len(manual)-50} 个")

    SUMMARY.write_text("\n".join(lines) + "\n", encoding="utf-8")
    REPORT.write_text(json.dumps([asdict(h) for h in results], ensure_ascii=False, indent=2), encoding="utf-8")

    if download:
        print("\n下载到 plugins/待更新 ...", flush=True)
        for h in updates:
            dest = OUT / h.filename
            ok = http_download(h.download_url, dest)
            print(f"  {'OK' if ok else 'FAIL'} {h.filename}", flush=True)

    print(f"\n报告: {SUMMARY}", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
