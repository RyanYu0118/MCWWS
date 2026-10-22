# 行星发动机点火.litematic 长宽高各放大 16 倍。
# 只写非空气方块，不写实体、不写方块实体 NBT。
# 与领地相交的 16³ 整格不写入领地内部。
import argparse
import json
import re
import urllib.request
from pathlib import Path

import nbtlib
import numpy as np

SCHEM = Path(r"D:\Minecraft\游戏主体\.minecraft\schematics\行星发动机点火.litematic")
RES_FILE = Path(r"D:\Minecraft\服务器\26.2\plugins\Residence\Save\Worlds\res_world.yml")
TOKEN_FILE = Path(r"D:\Minecraft\服务器\26.2\plugins\MCWWS_BuildBridge\config.yml")
OUT_DIR = Path(r"D:\Minecraft\服务器\26.2\logs\engine-16x")
JOBS = OUT_DIR / "jobs.jsonl"
PROGRESS = OUT_DIR / "progress.json"
SUMMARY = OUT_DIR / "summary.json"

ORIGIN_X = -3500
ORIGIN_Y = 1
SOUTH_Z = 4300
SCALE = 16
MAX_BOX = 25_000_000
MAX_JOBS = 4000
API = "http://127.0.0.1:8765/fawe_bulk"


def cell_bounds(x0, y0, z0, x1, y1, z1, size_z):
    wx1 = ORIGIN_X + x0 * SCALE
    wx2 = ORIGIN_X + (x1 + 1) * SCALE - 1
    wy1 = ORIGIN_Y + y0 * SCALE
    wy2 = ORIGIN_Y + (y1 + 1) * SCALE - 1
    wz2 = SOUTH_Z - (size_z - 1 - z1) * SCALE
    wz1 = SOUTH_Z - (size_z - 1 - z0) * SCALE - (SCALE - 1)
    return wx1, wy1, wz1, wx2, wy2, wz2


def normalize(box):
    x1, y1, z1, x2, y2, z2 = box
    return (min(x1, x2), min(y1, y2), min(z1, z2), max(x1, x2), max(y1, y2), max(z1, z2))


def overlaps(a, b):
    return a[0] <= b[3] and a[3] >= b[0] and a[1] <= b[4] and a[4] >= b[1] and a[2] <= b[5] and a[5] >= b[2]


def covers(cut, box):
    return cut[0] <= box[0] and cut[3] >= box[3] and cut[1] <= box[1] and cut[4] >= box[4] and cut[2] <= box[2] and cut[5] >= box[5]


def subtract(box, cut):
    ix1, iy1, iz1 = max(box[0], cut[0]), max(box[1], cut[1]), max(box[2], cut[2])
    ix2, iy2, iz2 = min(box[3], cut[3]), min(box[4], cut[4]), min(box[5], cut[5])
    if ix1 > ix2 or iy1 > iy2 or iz1 > iz2:
        return [box]
    parts = []
    if box[0] < ix1:
        parts.append((box[0], box[1], box[2], ix1 - 1, box[4], box[5]))
    if ix2 < box[3]:
        parts.append((ix2 + 1, box[1], box[2], box[3], box[4], box[5]))
    if box[1] < iy1:
        parts.append((ix1, box[1], box[2], ix2, iy1 - 1, box[5]))
    if iy2 < box[4]:
        parts.append((ix1, iy2 + 1, box[2], ix2, box[4], box[5]))
    if box[2] < iz1:
        parts.append((ix1, iy1, box[2], ix2, iy2, iz1 - 1))
    if iz2 < box[5]:
        parts.append((ix1, iy1, iz2 + 1, ix2, iy2, box[5]))
    return parts


def volume(box):
    return (box[3] - box[0] + 1) * (box[4] - box[1] + 1) * (box[5] - box[2] + 1)


def split_box(box):
    if volume(box) <= MAX_BOX:
        return [box]
    dx = box[3] - box[0] + 1
    dy = box[4] - box[1] + 1
    dz = box[5] - box[2] + 1
    if dx >= dy and dx >= dz and dx > 1:
        mid = box[0] + dx // 2 - 1
        left = (box[0], box[1], box[2], mid, box[4], box[5])
        right = (mid + 1, box[1], box[2], box[3], box[4], box[5])
    elif dy >= dz and dy > 1:
        mid = box[1] + dy // 2 - 1
        left = (box[0], box[1], box[2], box[3], mid, box[5])
        right = (box[0], mid + 1, box[2], box[3], box[4], box[5])
    else:
        mid = box[2] + dz // 2 - 1
        left = (box[0], box[1], box[2], box[3], box[4], mid)
        right = (box[0], box[1], mid + 1, box[3], box[4], box[5])
    return split_box(left) + split_box(right)


def load_residences():
    text = RES_FILE.read_text(encoding="utf-8")
    body = text.split("\nResidences:\n", 1)[1]
    current = None
    found = []
    key_re = re.compile(r"^  '?([^\s':]+)'?:\s*$")
    area_re = re.compile(r"^\s+\S+:\s*(-?\d+):(-?\d+):(-?\d+):(-?\d+):(-?\d+):(-?\d+)\s*$")
    for line in body.splitlines():
        key = key_re.match(line)
        if key:
            current = key.group(1)
            continue
        area = area_re.match(line)
        if area and current:
            found.append((current, normalize(tuple(int(n) for n in area.groups()))))
    return found


def load_volume():
    nbt = nbtlib.load(SCHEM)
    region = nbt["Regions"]["1"]
    size = region["Size"]
    sx, sy, sz = abs(int(size["x"])), abs(int(size["y"])), abs(int(size["z"]))
    palette = []
    for entry in region["BlockStatePalette"]:
        name = str(entry["Name"])
        if "Properties" in entry:
            props = ",".join(f"{key}={entry['Properties'][key]}" for key in entry["Properties"].keys())
            palette.append(f"{name}[{props}]")
        else:
            palette.append(name)
    air = {i for i, name in enumerate(palette) if name == "minecraft:air"}
    states = np.fromiter((int(v) & 0xFFFFFFFFFFFFFFFF for v in region["BlockStates"]), dtype=np.uint64)
    raw = np.empty(states.size * 8, dtype=np.uint8)
    for shift in range(0, 64, 8):
        raw[shift // 8 :: 8] = (states >> np.uint64(shift)) & np.uint64(0xFF)
    count = sx * sy * sz
    vol = raw[:count].reshape((sy, sz, sx)).transpose(2, 0, 1)
    for idx in air:
        vol[vol == idx] = 0
    entities = len(region["Entities"]) if "Entities" in region else 0
    return vol, palette, (sx, sy, sz), entities


def mark_blocked(shape, claims):
    sx, sy, sz = shape
    blocked = np.zeros(shape, dtype=np.bool_)
    engine = cell_bounds(0, 0, 0, sx - 1, sy - 1, sz - 1, sz)
    hit_names = []
    for name, claim in claims:
        if not overlaps(engine, claim):
            continue
        if name not in hit_names:
            hit_names.append(name)
        xs, ys, zs = [], [], []
        for x in range(sx):
            box = cell_bounds(x, 0, 0, x, 0, 0, sz)
            if box[0] <= claim[3] and box[3] >= claim[0]:
                xs.append(x)
        for y in range(sy):
            box = cell_bounds(0, y, 0, 0, y, 0, sz)
            if box[1] <= claim[4] and box[4] >= claim[1]:
                ys.append(y)
        for z in range(sz):
            box = cell_bounds(0, 0, z, 0, 0, z, sz)
            if box[2] <= claim[5] and box[5] >= claim[2]:
                zs.append(z)
        if xs and ys and zs:
            blocked[xs[0] : xs[-1] + 1, ys[0] : ys[-1] + 1, zs[0] : zs[-1] + 1] = True
    return blocked, hit_names, engine


def generate():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    vol, palette, shape, entities = load_volume()
    sx, sy, sz = shape
    claims = load_residences()
    blocked, hit_names, engine = mark_blocked(shape, claims)
    claim_boxes = [box for name, box in claims if name in hit_names]
    work = vol.copy()
    work[blocked] = 0
    jobs = []
    placed = 0
    coords = np.argwhere(work != 0)
    for x, y, z in coords:
        pid = int(work[x, y, z])
        if pid == 0:
            continue
        x1 = x + 1
        while x1 < sx and int(work[x1, y, z]) == pid:
            x1 += 1
        z1 = z + 1
        while z1 < sz and np.all(work[x:x1, y, z1] == pid):
            z1 += 1
        y1 = y + 1
        while y1 < sy and np.all(work[x:x1, y1, z:z1] == pid):
            y1 += 1
        work[x:x1, y:y1, z:z1] = 0
        world = cell_bounds(int(x), int(y), int(z), int(x1 - 1), int(y1 - 1), int(z1 - 1), sz)
        block = palette[pid]
        for part in split_box(world):
            if any(overlaps(part, claim) for claim in claim_boxes):
                raise SystemExit(f"greedy box hits residence {part}")
            jobs.append((part, block))
            placed += volume(part)
    if np.count_nonzero(work) != 0:
        raise SystemExit("greedy left unplaced cells")

    partial = np.argwhere((vol != 0) & blocked)
    skipped = 0
    for x, y, z in partial:
        world = cell_bounds(int(x), int(y), int(z), int(x), int(y), int(z), sz)
        if any(covers(claim, world) for claim in claim_boxes):
            skipped += volume(world)
            continue
        parts = [world]
        for claim in claim_boxes:
            nxt = []
            for part in parts:
                nxt.extend(subtract(part, claim))
            parts = nxt
        kept = 0
        block = palette[int(vol[x, y, z])]
        for part in parts:
            if any(overlaps(part, claim) for claim in claim_boxes):
                raise SystemExit(f"clipped box still hits residence {part}")
            for piece in split_box(part):
                jobs.append((piece, block))
                kept += volume(piece)
        if kept > volume(world):
            raise SystemExit(f"clipped cell grew {world}")
        placed += kept
        skipped += volume(world) - kept

    solid_cells = int(np.count_nonzero(vol))
    expect = solid_cells * SCALE ** 3
    if placed + skipped != expect:
        raise SystemExit(f"volume mismatch placed={placed} skipped={skipped} expect={expect}")

    with JOBS.open("w", encoding="utf-8") as handle:
        for box, block in jobs:
            handle.write(json.dumps({
                "x1": box[0], "y1": box[1], "z1": box[2],
                "x2": box[3], "y2": box[4], "z2": box[5],
                "block": block,
            }, ensure_ascii=False) + "\n")
    summary = {
        "engine": list(engine),
        "solid_cells": solid_cells,
        "entities_skipped": entities,
        "jobs": len(jobs),
        "placed_blocks": placed,
        "skipped_residence_blocks": skipped,
        "residences": hit_names,
        "residence_areas": len(claim_boxes),
        "palette_non_air": [name for name in palette if name != "minecraft:air"],
    }
    SUMMARY.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    PROGRESS.write_text(json.dumps({"next": 0, "jobs": len(jobs)}), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


def token():
    match = re.search(r"token:\s*(\S+)", TOKEN_FILE.read_text(encoding="utf-8"))
    if not match:
        raise SystemExit("missing build bridge token")
    return match.group(1)


def place():
    lines = JOBS.read_text(encoding="utf-8").splitlines()
    state = json.loads(PROGRESS.read_text(encoding="utf-8")) if PROGRESS.exists() else {"next": 0}
    index = int(state.get("next", 0))
    auth = token()
    total = len(lines)
    while index < total:
        batch = []
        volume_sum = 0
        start = index
        while index < total and len(batch) < MAX_JOBS:
            job = json.loads(lines[index])
            box_volume = (abs(job["x2"] - job["x1"]) + 1) * (abs(job["y2"] - job["y1"]) + 1) * (abs(job["z2"] - job["z1"]) + 1)
            if batch and volume_sum + box_volume > MAX_BOX:
                break
            batch.append(job)
            volume_sum += box_volume
            index += 1
        body = json.dumps({"world": "world", "jobs": batch}).encode("utf-8")
        req = urllib.request.Request(API, data=body, method="POST", headers={
            "Authorization": f"Bearer {auth}",
            "Content-Type": "application/json",
        })
        try:
            with urllib.request.urlopen(req, timeout=3600) as resp:
                payload = json.loads(resp.read().decode("utf-8"))
        except Exception as exc:
            PROGRESS.write_text(json.dumps({"next": start, "jobs": total, "error": str(exc)}), encoding="utf-8")
            raise
        if not payload.get("ok"):
            PROGRESS.write_text(json.dumps({"next": start, "jobs": total, "error": payload}), encoding="utf-8")
            raise SystemExit(payload)
        PROGRESS.write_text(json.dumps({
            "next": index,
            "jobs": total,
            "changed": payload.get("changed"),
            "last_volume": volume_sum,
        }), encoding="utf-8")
        print(f"{index}/{total} changed={payload.get('changed')} volume={volume_sum}", flush=True)
    print("done", flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("generate", "place", "all"))
    args = parser.parse_args()
    if args.mode in ("generate", "all"):
        generate()
    if args.mode in ("place", "all"):
        place()


if __name__ == "__main__":
    main()
