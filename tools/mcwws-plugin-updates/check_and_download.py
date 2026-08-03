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
    "skript-placeholders": {"platform": "modrinth", "slug": "skript-placeholders-fork"},
    "skript-reflect": {"platform": "modrinth", "slug": "skript-reflect"},
    "skript-yaml": {"platform": "modrinth", "slug": "skript-yaml"},
    "protocolib": {"platform": "modrinth", "slug": "protocollib"},
    "citizens": {"platform": "hangar", "author": "Citizens", "slug": "Citizens"},
    "denizen": {"platform": "modrinth", "slug": "denizen"},
    "bluemap": {"platform": "modrinth", "slug": "bluemap"},
    "train_carts": {"platform": "modrinth", "slug": "traincarts"},
    "bkcommonlib": {"platform": "modrinth", "slug": "bkcommonlib"},
    "smoothcoasters": {"platform": "hangar", "author": "TrainCarts", "slug": "SmoothCoasters"},
    "tccoasters": {"platform": "hangar", "author": "TrainCarts", "slug": "TCCoasters"},
    "gsit": {"platform": "modrinth", "slug": "gsit"},
    "playerparticles": {"platform": "modrinth", "slug": "playerparticles"},
    "inventoryrollbackplus": {"platform": "modrinth", "slug": "inventoryrollbackplus"},
    "decentholograms": {"platform": "modrinth", "slug": "decentholograms"},
    "ultimateshop": {"platform": "modrinth", "slug": "ultimateshop"},
    "ultimatetimber": {"platform": "modrinth", "slug": "ultimatetimber"},
    "veinminer": {"platform": "modrinth", "slug": "veinminer"},
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


def _file_loader_score(filename: str, ver_loaders: list[str]) -> int:
    """Higher = better match for Paper server."""
    name = filename.lower()
    if "paper" in name and "spigot" not in name:
        return 100
    if "paper-plugin" in name:
        return 95
    if "folia" in name:
        return 80
    if "purpur" in name:
        return 70
    if "bukkit" in name:
        return 40
    if "spigot" in name:
        return 30
    loaders = {x.lower() for x in (ver_loaders or [])}
    if "paper" in loaders:
        return 60
    if loaders & {"folia", "purpur"}:
        return 55
    if "bukkit" in loaders:
        return 45
    if "spigot" in loaders:
        return 35
    return 50


def pick_modrinth_file(ver: dict) -> dict | None:
    files = ver.get("files") or []
    if not files:
        return None
    loaders = ver.get("loaders") or []
    primary = [f for f in files if f.get("primary")]
    pool = primary or files
    return max(pool, key=lambda f: _file_loader_score(f.get("filename", ""), loaders))


def modrinth_latest(slug: str) -> tuple[str, str, str, str] | None:
    enc = urllib.parse.quote(slug)
    loader_sets = [
        ["paper"],
        ["paper", "folia", "purpur"],
        ["paper", "bukkit", "spigot"],
    ]
    data: list | None = None
    for loaders in loader_sets:
        lv = urllib.parse.quote(json.dumps(loaders))
        data = http_json(
            f"https://api.modrinth.com/v2/project/{enc}/version"
            f"?loaders={lv}"
            f"&game_versions=[%221.21.11%22,%221.21.10%22,%221.21%22]"
        )
        if isinstance(data, list) and data:
            break
    if not isinstance(data, list) or not data:
        data = http_json(f"https://api.modrinth.com/v2/project/{enc}/version")
    if not isinstance(data, list) or not data:
        return None
    best_ver = None
    best_file = None
    best_score = -1
    for ver in data[:5]:
        picked = pick_modrinth_file(ver)
        if not picked:
            continue
        score = _file_loader_score(picked.get("filename", ""), ver.get("loaders") or [])
        if score > best_score:
            best_score = score
            best_ver = ver
            best_file = picked
    if not best_ver or not best_file:
        return None
    fname = best_file.get("filename", "")
    note = ""
    if "spigot" in fname.lower() and "paper" not in fname.lower():
        note = "仅 Spigot 命名，Paper 可用"
    elif "bukkit" in fname.lower() and "paper" not in fname.lower():
        note = "仅 Bukkit 命名，Paper 可用"
    return best_ver.get("version_number", ""), best_file.get("url", ""), note, fname


def hangar_latest(author: str, slug: str) -> tuple[str, str, str] | None:
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
        jar_name = ""
        for key in ("PAPER", "PAPER_PLUGIN"):
            entry = downloads.get(key)
            if isinstance(entry, dict):
                url = entry.get("downloadUrl") or entry.get("externalUrl")
                jar_name = (entry.get("fileInfo") or {}).get("name") or jar_name
            elif isinstance(entry, str) and entry.endswith(".jar"):
                url = entry
            if url:
                break
        if not url:
            for entry in downloads.values():
                if isinstance(entry, dict):
                    url = entry.get("downloadUrl") or entry.get("externalUrl")
                    jar_name = jar_name or (entry.get("fileInfo") or {}).get("name", "")
                elif isinstance(entry, str) and entry.endswith(".jar"):
                    url = entry
                if url:
                    break
        if url:
            if url.startswith("/"):
                url = "https://hangar.papermc.io" + url
            if not jar_name:
                jar_name = pick_filename(url, f"{slug}.jar", ver_name)
            return ver_name, url, jar_name
    return None


def pick_filename(url: str, jar: str, latest: str, preferred: str = "") -> str:
    if preferred and preferred.endswith(".jar"):
        return preferred
    base = urllib.parse.unquote(Path(urllib.parse.urlparse(url).path).name)
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
    note = ""
    preferred_fname = ""
    if platform == "modrinth":
        slug = src["slug"]
        got = modrinth_latest(slug)
        project = slug
        if got:
            latest, url, loader_note, preferred_fname = got
            if loader_note and not note:
                note = loader_note
    elif platform == "hangar":
        got = hangar_latest(src["author"], src["slug"])
        project = f"{src['author']}/{src['slug']}"
        if got:
            latest, url, preferred_fname = got
    elif platform == "manual":
        project = src.get("url", "manual")
        manual_note = src.get("note") or "需手动下载"
        if src.get("url"):
            manual_note = f"{manual_note} | {src['url']}"
        return UpdateHit(info.jar, info.name, info.version, "", "manual", project, "", "", "manual", manual_note)

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
        return UpdateHit(info.jar, info.name, info.version, latest, platform, project, url, pick_filename(url, info.jar, latest, preferred_fname), status, note)

    fname = pick_filename(url, info.jar, latest, preferred_fname)
    return UpdateHit(info.jar, info.name, info.version, latest, platform, project, url, fname, status, note)


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
        "MCWWS 插件更新报告（优先 Paper 构建）",
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

    lines.extend(["", "=== 手动更新源（论坛/作者站，见 plugin-sources.json）==="])
    manual_known = [h for h in results if h.status == "manual" and h.platform == "manual"]
    manual_other = [h for h in manual if h not in manual_known]
    for h in manual_known:
        lines.append(f"{h.name} | {h.current} | {h.note}")
    lines.extend(["", "=== 未配置源（需 Spigot/Slimefun 构建站/手动）==="])
    for h in manual_other[:50]:
        lines.append(f"{h.name} | {h.current} | {h.jar}")
    if len(manual_other) > 50:
        lines.append(f"... 另有 {len(manual_other)-50} 个")

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
