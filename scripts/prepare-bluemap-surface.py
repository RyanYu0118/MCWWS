import argparse
from concurrent.futures import ProcessPoolExecutor
import gzip
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zlib
from zipfile import ZipFile

import nbtlib


ROOT = Path(__file__).resolve().parents[1]
REGIONS = Path("dimensions/minecraft/overworld/region")
SECTOR = 4096
FIRST_REMOVED_SECTION = 320 // 16
LEGACY_OVERWORLD_MIN_SECTION = -64 // 16
REGION_NAME = re.compile(r"r\.(-?\d+)\.(-?\d+)\.mca")
EXTERNAL_NAME = re.compile(r"c\.(-?\d+)\.(-?\d+)\.mcc")
MANIFEST = "surface-manifest.json"
MANIFEST_VERSION = 1


def region_coords(name):
    return tuple(map(int, REGION_NAME.fullmatch(name).groups()))


def region_files(folder):
    grouped = {}
    for path in folder.iterdir():
        if not path.is_file():
            continue
        if REGION_NAME.fullmatch(path.name):
            grouped.setdefault(path.name, []).append(path)
        elif match := EXTERNAL_NAME.fullmatch(path.name):
            chunk_x, chunk_z = map(int, match.groups())
            grouped.setdefault(f"r.{chunk_x // 32}.{chunk_z // 32}.mca", []).append(path)
    return {name: sorted(paths) for name, paths in grouped.items() if (folder / name).is_file()}


def fingerprint(paths, root):
    digest = hashlib.sha256()
    for path in sorted(paths):
        digest.update(path.relative_to(root).as_posix().encode("utf-8") + b"\0")
        with path.open("rb") as reader:
            digest.update(hashlib.file_digest(reader, "sha256").digest())
    return digest.hexdigest()


def add_neighbors(names):
    result = set()
    for name in names:
        region_x, region_z = region_coords(name)
        for delta_x in (-1, 0, 1):
            for delta_z in (-1, 0, 1):
                result.add(f"r.{region_x + delta_x}.{region_z + delta_z}.mca")
    return result


def read_region(path):
    region_x, region_z = region_coords(path.name)
    with path.open("rb") as region:
        header = region.read(2 * SECTOR)
        if len(header) != 2 * SECTOR:
            raise ValueError(f"Truncated region header: {path}")
        for index in range(1024):
            offset = index * 4
            sector = int.from_bytes(header[offset:offset + 3], "big")
            if not sector:
                continue
            chunk_x, chunk_z = region_x * 32 + index % 32, region_z * 32 + index // 32
            region.seek(sector * SECTOR)
            size = int.from_bytes(region.read(4), "big")
            if not 1 <= size <= header[offset + 3] * SECTOR - 4:
                raise ValueError(f"Invalid chunk length in {path} at {chunk_x}, {chunk_z}")
            method = region.read(1)[0]
            payload = region.read(size - 1)
            if method & 128:
                payload = path.with_name(f"c.{chunk_x}.{chunk_z}.mcc").read_bytes()
                method &= 127
            decode = {1: gzip.decompress, 2: zlib.decompress, 3: bytes}.get(method)
            if decode is None:
                raise ValueError(f"Unsupported region compression {method}: {path}")
            chunk = nbtlib.File.parse(io.BytesIO(decode(payload)))
            if (int(chunk["xPos"]), int(chunk["zPos"])) != (chunk_x, chunk_z):
                raise ValueError(f"Unexpected chunk coordinates in {path}")
            yield index, chunk_x, chunk_z, chunk


def clip_chunk(chunk):
    for key in ("Heightmaps", "isLightOn", "starlight.light_version"):
        chunk.pop(key, None)
    chunk["sections"][:] = [section for section in chunk["sections"]
                            if int(section["Y"]) < FIRST_REMOVED_SECTION]
    for section in chunk["sections"]:
        for key in ("SkyLight", "BlockLight", "starlight.blocklight_state", "starlight.skylight_state"):
            section.pop(key, None)
    for key in ("block_entities", "block_ticks", "fluid_ticks"):
        if key in chunk:
            chunk[key][:] = [item for item in chunk[key] if int(item["y"]) < 320]
    if "PostProcessing" in chunk:
        first_section = int(chunk.get("yPos", LEGACY_OVERWORLD_MIN_SECTION))
        del chunk["PostProcessing"][max(0, FIRST_REMOVED_SECTION - first_section):]


def stage_region(source, destination, plan):
    header = bytearray(2 * SECTOR)
    with destination.open("wb") as output:
        output.write(header)
        for index, chunk_x, chunk_z, chunk in read_region(source):
            if str(chunk["Status"]) not in ("minecraft:full", "full"):
                continue
            clip_chunk(chunk)
            buffer = io.BytesIO()
            chunk.write(buffer)
            payload = b"\x02" + zlib.compress(buffer.getvalue())
            record = len(payload).to_bytes(4, "big") + payload
            sectors = (len(record) + SECTOR - 1) // SECTOR
            if sectors > 255:
                destination.with_name(f"c.{chunk_x}.{chunk_z}.mcc").write_bytes(payload[1:])
                record = b"\x00\x00\x00\x01\x82"
                sectors = 1
            offset = index * 4
            header[offset:offset + 4] = (output.tell() // SECTOR * 256 + sectors).to_bytes(4, "big")
            output.write(record.ljust(sectors * SECTOR, b"\x00"))
            plan.write(f"{chunk_x} {chunk_z}\n")
        output.seek(0)
        output.write(header)


def surface_fingerprint(path):
    digest = hashlib.sha256()
    for index, _, _, chunk in read_region(path):
        if str(chunk["Status"]) not in ("minecraft:full", "full"):
            continue
        clip_chunk(chunk)
        sections = nbtlib.List[nbtlib.Compound](
            nbtlib.Compound({key: section[key] for key in ("Y", "block_states", "biomes") if key in section})
            for section in sorted(chunk["sections"], key=lambda section: int(section["Y"]))
            if "block_states" in section or "biomes" in section
        )
        relevant = nbtlib.File(nbtlib.Compound({
            "DataVersion": chunk["DataVersion"],
            "sections": sections,
            "block_entities": chunk.get("block_entities", nbtlib.List[nbtlib.Compound]()),
        }))
        buffer = io.BytesIO()
        relevant.write(buffer)
        digest.update(index.to_bytes(2, "big"))
        digest.update(hashlib.sha256(buffer.getbuffer()).digest())
    return digest.hexdigest()


def copy_region_files(files, destination, *, reuse=False):
    for source in files:
        target = destination / source.name
        if reuse:
            try:
                target.hardlink_to(source)
                continue
            except OSError:
                pass
        shutil.copy2(source, target)


def scan_region(task):
    name, paths, source_folder, old = task
    raw = fingerprint(paths, source_folder)
    if old and old["raw"] == raw:
        return name, old
    return name, {"raw": raw, "surface": surface_fingerprint(source_folder / name)}


def collect_scan(results, total):
    current = {}
    for index, (name, record) in enumerate(results, 1):
        current[name] = record
        if index % 25 == 0 or index == total:
            print(f"[{index}/{total}] Scanned regions", flush=True)
    return current


def find_changes(source_regions, source_folder, previous, workers):
    tasks = [(name, paths, source_folder, previous.get(name)) for name, paths in sorted(source_regions.items())]
    if workers == 1 or len(tasks) < 2:
        current = collect_scan(map(scan_region, tasks), len(tasks))
    else:
        with ProcessPoolExecutor(max_workers=min(workers, len(tasks))) as pool:
            current = collect_scan(pool.map(scan_region, tasks), len(tasks))
    changed = set(previous) ^ set(current)
    changed.update(name for name in previous.keys() & current.keys()
                   if previous[name]["surface"] != current[name]["surface"])
    return current, changed


def install_relight_plugin(stage, java_bin):
    classes = stage / "relight-classes"
    classes.mkdir()
    plugins = stage / "plugins"
    plugins.mkdir(exist_ok=True)
    dependencies = [ROOT / "versions/26.2/paper-26.2.jar"]
    dependencies.extend((ROOT / "libraries").rglob("*.jar"))
    classpath = os.pathsep.join(path.as_posix() for path in dependencies)
    arguments = stage / "javac.args"
    arguments.write_text(f'-encoding UTF-8 -cp "{classpath}" -d "{classes.as_posix()}" '
                         f'"{Path(__file__).with_name("SurfaceRelight.java").as_posix()}"',
                         encoding="utf-8")
    subprocess.run([java_bin / "javac.exe", "-J-Dfile.encoding=UTF-8", f"@{arguments}"], check=True)
    with ZipFile(plugins / "SurfaceRelight.jar", "w") as plugin:
        plugin.write(classes / "SurfaceRelight.class", "SurfaceRelight.class")
        plugin.writestr("plugin.yml", 'name: SurfaceRelight\nversion: 1\nmain: SurfaceRelight\napi-version: "26.2"\n')


def copy_world_metadata(source, destination, metadata):
    shutil.copy2(source / "level.dat", destination)
    for path in metadata:
        target = destination / path.relative_to(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)


def relight(work, java_bin, paper, resume=False, batch_size=16384):
    plan = work / "surface-chunks.txt"
    full_plan = work / "surface-chunks.full.txt"
    checkpoint = work / "surface-relight.checkpoint"
    done = work / "surface-relight.done"
    if resume:
        for name in ("relight-classes", "plugins/SurfaceRelight.jar"):
            path = work / name
            if path.is_dir() and path.resolve().is_relative_to(work.resolve()):
                shutil.rmtree(path)
            elif path.is_file():
                path.unlink()
    if not full_plan.exists():
        plan.replace(full_plan)
    install_relight_plugin(work, java_bin)
    if not (work / "cache").exists():
        shutil.copytree(ROOT / "cache", work / "cache")
    if not (work / paper.name).exists():
        shutil.copy2(paper, work)
    (work / "eula.txt").write_text((ROOT / "eula.txt").read_text(encoding="utf-8"), encoding="utf-8")
    (work / "server.properties").write_text(
        "level-name=world\nserver-ip=127.0.0.1\nserver-port=0\n"
        "enable-query=false\nenable-rcon=false\nallow-nether=false\nview-distance=2\nsimulation-distance=2\n",
        encoding="ascii")
    (work / "bukkit.yml").write_text("settings:\n  allow-end: false\n", encoding="ascii")
    config = work / "config/paper-global.yml"
    config.parent.mkdir(exist_ok=True)
    if config.exists():
        settings = config.read_text(encoding="utf-8")
        settings = re.sub(r"(?m)(^spark:\n(?:  [^\n]+\n)*?  enabled: )true$", r"\g<1>false", settings)
    else:
        settings = "spark:\n  enabled: false\n"
    config.write_text(settings, encoding="utf-8")
    positions = full_plan.read_text(encoding="ascii").splitlines()
    cursor = int(checkpoint.read_text()) if checkpoint.exists() else 0
    if not 0 <= cursor <= len(positions):
        raise ValueError("Invalid relight checkpoint")
    while cursor < len(positions):
        end = min(cursor + batch_size, len(positions))
        plan.write_text("\n".join(positions[cursor:end]) + "\n", encoding="ascii")
        done.unlink(missing_ok=True)
        print(f"[{cursor}/{len(positions)}] Starting light pass through chunk {end}", flush=True)
        subprocess.run([java_bin / "java.exe", "-Xmx12G", "-XX:+ExitOnOutOfMemoryError",
                        "-jar", paper.name, "--nogui", "--noconsole"],
                       cwd=work, stdin=subprocess.DEVNULL, check=True)
        if not done.exists() or int(done.read_text()) != end - cursor:
            raise RuntimeError("Paper did not finish relighting this batch; checkpoint retained.")
        pending = checkpoint.with_suffix(".tmp")
        pending.write_text(str(end), encoding="ascii")
        pending.replace(checkpoint)
        cursor = end
        print(f"[{cursor}/{len(positions)}] Light checkpoint saved", flush=True)


def main():
    parser = argparse.ArgumentParser(description="Build a clipped, relit BlueMap world in a new directory.")
    parser.add_argument("stage", type=Path)
    parser.add_argument("--source", type=Path, default=ROOT / "world", help="Saved overworld or its backup.")
    parser.add_argument("--previous", type=Path, help="Completed generation to reuse for an incremental build.")
    parser.add_argument("--workers", type=int, default=1, help="Number of CPU processes for scanning regions.")
    parser.add_argument("--resume-light", action="store_true", help="Reuse a fully clipped baseline in the staging directory.")
    parser.add_argument("--light-batch-size", type=int, default=16384, help="Chunks per temporary Paper run.")
    parser.add_argument("--region", action="append", help="Limit to specific region names for testing; repeatable.")
    parser.add_argument("--java-home", type=Path, default=Path(os.environ.get("JAVA_HOME")
                        or "C:/Program Files/Java/jdk-25.0.2"))
    args = parser.parse_args()
    if args.light_batch_size < 1:
        parser.error("--light-batch-size must be positive.")
    if not 1 <= args.workers <= min(61, os.cpu_count() or 1):
        parser.error("--workers must be between 1 and the available logical CPU count (max 61).")
    source, stage = args.source.resolve(), args.stage.resolve()
    live = ROOT / "world"
    previous = args.previous.resolve() if args.previous else None
    if (stage.is_relative_to(source) or stage.is_relative_to(live) or (stage.exists() and not args.resume_light)
            or (previous and (stage.is_relative_to(previous) or source.is_relative_to(previous)))):
        parser.error("Use separate input and output directories; the output must be new.")
    if previous and source.is_relative_to(live):
        parser.error("For incremental builds, --source must be a consistent backup outside the live world.")
    source_folder = source / REGIONS
    regions = region_files(source_folder)
    scope = sorted(set(args.region)) if args.region else None
    if scope:
        if any(not REGION_NAME.fullmatch(name) for name in scope):
            parser.error("Invalid test region name.")
        regions = {name: files for name, files in regions.items() if name in scope}
    if not regions:
        parser.error("No matching source regions.")

    paper = max(ROOT.glob("paper-26.2-*.jar"), key=lambda path: int(path.stem.rsplit("-", 1)[1]))
    metadata = [path for path in (source / "datapacks").rglob("*") if path.is_file()]
    metadata.extend(path for name in ("minecraft/world_gen_settings.dat", "paper/metadata.dat", "paper/level_overrides.dat")
                    if (path := source / REGIONS.parent / "data" / name).is_file())
    settings = {"version": MANIFEST_VERSION, "paper": paper.name, "scope": scope,
                "metadata": fingerprint(metadata, source)}
    old_regions = {}
    old_outputs = {}
    if previous:
        old = json.loads((previous / MANIFEST).read_text(encoding="utf-8"))
        if old["settings"] != settings:
            parser.error("Snapshot settings or test scope changed; rebuild without --previous.")
        old_regions = old["regions"]
        old_outputs = region_files(previous / "world" / REGIONS)
        if not old_regions.keys() <= old_outputs.keys():
            parser.error("The previous snapshot is missing published region files.")
    work = stage / "work"
    built = work / "world" / REGIONS
    if args.resume_light:
        if (previous or scope or (stage / MANIFEST).exists() or (stage / "world").exists()
                or not ((work / "surface-chunks.txt").is_file() or (work / "surface-chunks.full.txt").is_file())
                or region_files(built).keys() != regions.keys()):
            parser.error("--resume-light requires a fully clipped, unpublished baseline from the same source.")
        relight(work, args.java_home / "bin", paper, resume=True, batch_size=args.light_batch_size)
    current, changes = find_changes(regions, source_folder, old_regions, args.workers)
    affected = set(regions) if not previous else add_neighbors(changes) & regions.keys()
    context = (add_neighbors(affected) - affected) & old_outputs.keys() & regions.keys()
    print(f"{len(changes)} changed, {len(affected)} to relight, {len(regions) - len(affected)} reused.", flush=True)

    if affected and not args.resume_light:
        built.mkdir(parents=True)
        copy_world_metadata(source, work / "world", metadata)
        for name in sorted(context):
            copy_region_files(old_outputs[name], built)
        plan_path = work / "surface-chunks.txt"
        with plan_path.open("w", encoding="ascii") as plan:
            for index, name in enumerate(sorted(affected), 1):
                print(f"[{index}/{len(affected)}] Clipping {name}", flush=True)
                stage_region(source_folder / name, built / name, plan)
        if plan_path.stat().st_size:
            relight(work, args.java_home / "bin", paper, batch_size=args.light_batch_size)

    snapshot = stage / "world"
    destination = snapshot / REGIONS
    destination.mkdir(parents=True)
    copy_world_metadata(source, snapshot, metadata)
    new_outputs = region_files(built) if affected else {}
    for name in sorted(regions):
        if name in affected:
            for file in new_outputs[name]:
                file.replace(destination / file.name)
        else:
            copy_region_files(old_outputs[name], destination, reuse=True)
    if work.exists():
        if work.resolve() != work:
            raise RuntimeError(f"Refusing to clean up a redirected work directory: {work}")
        log = work / "logs/latest.log"
        if log.is_file():
            shutil.copy2(log, stage / "surface-relight.log")
        shutil.rmtree(work)
    result = {"settings": settings, "regions": current, "changed": sorted(changes),
              "relit": sorted(affected)}
    pending = stage / (MANIFEST + ".tmp")
    pending.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    pending.replace(stage / MANIFEST)
    print(f"Completed snapshot: {snapshot}")


if __name__ == "__main__":
    main()
