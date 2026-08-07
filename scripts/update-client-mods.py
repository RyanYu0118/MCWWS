#!/usr/bin/env python3
"""Download Minecraft 26.2 Fabric updates for currently ENABLED client mods."""

from __future__ import annotations

import hashlib
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

MODS_DIR = Path(r"D:\Minecraft\游戏主体\.minecraft\mods")
OUT_DIR = MODS_DIR / "待更新"
# Keep reports out of the jar dump so copy/replace won't drag them into mods/
REPORT_DIR = OUT_DIR / "报告"
GAME_VERSION = "26.2"
LOADERS = ["fabric"]
UA = {"User-Agent": "MCWWS-mod-updater/1.0 (contact: local)"}
# Subdirs are never loaded by Fabric as enabled mods
SKIP_DIR_NAMES = {"待更新", "屏蔽", "禁用", "旧版", "备份", "报告"}

# Known Modrinth slug overrides when filename matching is ambiguous
SLUG_HINTS = {
    "AmbientSounds": "ambientsounds",
    "appleskin": "appleskin",
    "Axiom": "axiom",
    "c2me": "c2me",
    "chat_heads": "chat-heads",
    "Chunky": "chunky",
    "cloth-config": "cloth-config",
    "continuity": "continuity",
    "CreativeCore": "creativecore",
    "eg_particle_interactions": "eg-particle-interactions",
    "emotecraft": "emotecraft",
    "entity_model_features": "entity-model-features",
    "entity_texture_features": "entity-texture-features",
    "entityculling": "entityculling",
    "Essential": None,  # proprietary / Essential CDN
    "fabric-api": "fabric-api",
    "fabric-language-kotlin": "fabric-language-kotlin",
    "FancyBlockParticles": "fbp-renewed",
    "ferritecore": "ferrite-core",
    "fzzy_config": "fzzy-config",
    "IAS": "ias",
    "ImmediatelyFast": "immediatelyfast",
    "iris": "iris",
    "Jade": "jade",
    "jei": "jei",
    "journeymap": "journeymap",
    "litematica": "litematica",
    "lithium": "lithium",
    "malilib": "malilib",
    "modmenu": "modmenu",
    "moreculling": "moreculling",
    "notenoughanimations": "not-enough-animations",
    "physics-mod": "physicsmod",
    "PlayerAnimationLib": "playeranimator",
    "reeses-sodium-options": "reeses-sodium-options",
    "Resourcify": "resourcify",
    "schematicpreview": "schematicpreview",
    "skinlayers3d": "skinlayers3d",
    "smoothgui": "smoothgui",
    "smoothscroll": "smooth-scroll",
    "sodium-extra": "sodium-extra",
    "sodium": "sodium",
    "sound-physics-remastered": "sound-physics-remastered",
    "SubtleEffects": "subtle-effects",
    "tweakeroo": "tweakeroo",
    "voxy": "voxy",
    "WI-Zoom": "wi-zoom",
    "worldedit": "worldedit",
    "yet_another_config_lib": "yacl",
    "zmusic": "zmusic",
}


def sha512_file(path: Path) -> str:
    h = hashlib.sha512()
    with path.open("rb") as f:
        while True:
            chunk = f.read(1024 * 1024)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()


def api_request(method: str, url: str, body: dict | None = None) -> object | None:
    data = None
    headers = dict(UA)
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise


def enabled_jars() -> list[Path]:
    """Top-level files with suffix exactly '.jar' (Fabric only loads these).

    Excludes: *.jar.old / *.jar.disabled (suffix is .old/.disabled), and all subfolders.
    """
    jars: list[Path] = []
    for p in sorted(MODS_DIR.iterdir(), key=lambda x: x.name.lower()):
        if p.is_file() and p.suffix.lower() == ".jar":
            jars.append(p)
    return jars


def hint_slug(filename: str) -> str | None:
    for prefix, slug in SLUG_HINTS.items():
        if filename.startswith(prefix) or filename.lower().startswith(prefix.lower()):
            return slug
    return None


def latest_from_hash(file_hash: str) -> dict | None:
    url = f"https://api.modrinth.com/v2/version_file/{file_hash}/update?algorithm=sha512"
    body = {"loaders": LOADERS, "game_versions": [GAME_VERSION]}
    result = api_request("POST", url, body)
    return result if isinstance(result, dict) else None


def latest_from_slug(slug: str) -> dict | None:
    q = urllib.parse.urlencode(
        {
            "loaders": json.dumps(LOADERS),
            "game_versions": json.dumps([GAME_VERSION]),
        }
    )
    url = f"https://api.modrinth.com/v2/project/{urllib.parse.quote(slug)}/version?{q}"
    result = api_request("GET", url)
    if isinstance(result, list) and result:
        # Prefer newest by date_published
        result.sort(key=lambda v: v.get("date_published", ""), reverse=True)
        return result[0]
    return None


def primary_file(version: dict) -> dict | None:
    files = version.get("files") or []
    for f in files:
        if f.get("primary"):
            return f
    return files[0] if files else None


def download(url: str, dest: Path) -> bool:
    req = urllib.request.Request(url, headers=UA)
    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            data = resp.read()
        if len(data) < 1000:
            return False
        dest.write_bytes(data)
        return True
    except Exception as e:
        print(f"  download fail: {e}")
        return False


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    jars = enabled_jars()
    # Inventory for clarifying "82 items" miscounts
    all_files = [p for p in MODS_DIR.iterdir() if p.is_file()]
    n_old = sum(1 for p in all_files if p.name.lower().endswith(".jar.old"))
    n_dis = sum(1 for p in all_files if p.name.lower().endswith(".jar.disabled"))
    n_dirs = sum(1 for p in MODS_DIR.iterdir() if p.is_dir())
    print(f"Enabled jars (.jar only): {len(jars)}")
    print(f"Also present: .jar.old={n_old}, .jar.disabled={n_dis}, subdirs={n_dirs}")
    print(f"Target: Minecraft {GAME_VERSION} / {LOADERS}")
    print(f"Output jars: {OUT_DIR}")
    print(f"Output reports: {REPORT_DIR}")
    print()

    report: list[dict] = []

    for jar in jars:
        print(f"== {jar.name}")
        entry = {
            "current": jar.name,
            "status": "unknown",
            "new_file": None,
            "version": None,
            "project": None,
            "note": "",
        }
        try:
            file_hash = sha512_file(jar)
            version = latest_from_hash(file_hash)
            source = "hash-update"

            if version is None:
                slug = hint_slug(jar.name)
                if slug:
                    version = latest_from_slug(slug)
                    source = f"slug:{slug}"
                    entry["project"] = slug
                else:
                    entry["status"] = "no-match"
                    entry["note"] = "Modrinth 未识别且无 slug 提示"
                    report.append(entry)
                    print("  NO MATCH")
                    continue

            if version is None:
                entry["status"] = "no-26.2"
                entry["note"] = f"找到项目但无 {GAME_VERSION} Fabric 版本 ({source})"
                report.append(entry)
                print(f"  NO {GAME_VERSION} VERSION ({source})")
                continue

            entry["project"] = version.get("project_id") or entry.get("project")
            entry["version"] = version.get("version_number") or version.get("name")
            f = primary_file(version)
            if not f:
                entry["status"] = "no-file"
                entry["note"] = "版本无文件"
                report.append(entry)
                print("  NO FILE")
                continue

            new_name = f.get("filename") or jar.name
            # Same filename and likely same build
            if new_name == jar.name:
                # Compare hashes if available
                hashes = f.get("hashes") or {}
                if hashes.get("sha512") == file_hash:
                    entry["status"] = "up-to-date"
                    entry["note"] = "已是目标版本/相同文件"
                    report.append(entry)
                    print("  UP TO DATE")
                    continue

            dest = OUT_DIR / new_name
            if dest.exists() and dest.stat().st_size > 1000:
                entry["status"] = "downloaded"
                entry["new_file"] = new_name
                entry["note"] = f"已存在于待更新 ({source})"
                report.append(entry)
                print(f"  EXISTS {new_name}")
                continue

            url = f.get("url")
            print(f"  GET {new_name} ({entry['version']})")
            if url and download(url, dest):
                entry["status"] = "downloaded"
                entry["new_file"] = new_name
                entry["note"] = source
                print(f"  OK {dest.stat().st_size} bytes")
            else:
                entry["status"] = "download-failed"
                entry["note"] = source
                print("  FAIL")
            report.append(entry)
            time.sleep(0.15)
        except Exception as e:
            entry["status"] = "error"
            entry["note"] = str(e)
            report.append(entry)
            print(f"  ERROR {e}")

    # Write reports into subdirectory (not mixed with jars)
    (REPORT_DIR / "update-report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    summary: dict[str, list[str]] = {}
    for e in report:
        summary.setdefault(e["status"], []).append(e["current"])

    lines = [
        f"模组更新扫描目标：Minecraft {GAME_VERSION} / Fabric",
        f"启用判定：仅 mods 根目录、后缀恰好为 .jar 的文件（Path.suffix == '.jar'）",
        f"不处理：.jar.old / .jar.disabled / 子目录内文件",
        f"启用数量：{len(jars)}",
        f"同目录其它项：.jar.old={n_old}, .jar.disabled={n_dis}, 子目录={n_dirs}",
        f"若把 jar+old+disabled+子目录全加起来 = {len(jars)+n_old+n_dis+n_dirs}（易被当成 82）",
        "",
    ]
    order = [
        "downloaded",
        "up-to-date",
        "no-26.2",
        "no-match",
        "no-file",
        "download-failed",
        "error",
        "unknown",
    ]
    labels = {
        "downloaded": "已下载到待更新",
        "up-to-date": "无需更新/已是同文件",
        "no-26.2": f"暂无 {GAME_VERSION} Fabric 版",
        "no-match": "Modrinth 未匹配",
        "no-file": "版本无文件",
        "download-failed": "下载失败",
        "error": "出错",
        "unknown": "未知",
    }
    for key in order:
        items = summary.get(key) or []
        if not items:
            continue
        lines.append(f"[{labels.get(key, key)}] ({len(items)})")
        for name in items:
            detail = next(e for e in report if e["current"] == name)
            extra = ""
            if detail.get("new_file"):
                extra = f" -> {detail['new_file']}"
            elif detail.get("note"):
                extra = f" ({detail['note']})"
            lines.append(f"  - {name}{extra}")
        lines.append("")

    lines.append("说明：")
    lines.append("- 报告写在 待更新/报告/，避免整夹复制进 mods 时带上 txt/json")
    lines.append("- .jar.old 是旧版备份，不是启用模组；.jar.disabled 是未启用")
    lines.append("- 当前 versions/26.2 若仍是原版档案，需先装 Fabric Loader 26.2")
    lines.append("- 替换：关游戏 -> 旧 jar 移走/备份 -> 待更新内 jar 移入 mods 根目录")

    report_txt = REPORT_DIR / "update-report.txt"
    report_txt.write_text("\n".join(lines), encoding="utf-8")
    print("\n" + "\n".join(lines))
    print(f"\nWrote {report_txt}")


if __name__ == "__main__":
    main()
