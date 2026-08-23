#!/usr/bin/env python3
"""Minimal Minecraft RCON client for one-off BlueMap purge."""
from __future__ import annotations

import argparse
import socket
import struct
import time


def _pack(req_id: int, req_type: int, payload: str) -> bytes:
    body = payload.encode("utf-8") + b"\x00\x00"
    return struct.pack("<iii", 4 + 4 + len(body), req_id, req_type) + body


def _recv_packet(sock: socket.socket) -> tuple[int, int, str]:
    header = sock.recv(12)
    if len(header) < 12:
        raise ConnectionError("rcon short header")
    length, req_id, req_type = struct.unpack("<iii", header)
    payload_len = length - 8
    payload = b""
    while len(payload) < payload_len:
        chunk = sock.recv(payload_len - len(payload))
        if not chunk:
            break
        payload += chunk
    text = payload[:-2].decode("utf-8", errors="replace") if payload_len >= 2 else ""
    return req_id, req_type, text


def rcon(host: str, port: int, password: str, command: str, timeout: float = 10.0) -> str:
    with socket.create_connection((host, port), timeout=timeout) as sock:
        sock.sendall(_pack(1, 3, password))  # LOGIN
        rid, rtype, _ = _recv_packet(sock)
        if rid == -1:
            raise PermissionError("rcon auth failed")
        sock.sendall(_pack(2, 2, command))  # COMMAND
        _, _, text = _recv_packet(sock)
        return text


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25575)
    ap.add_argument("--password", required=True)
    ap.add_argument("command")
    args = ap.parse_args()
    # retry briefly while server boots rcon
    last_err: Exception | None = None
    for _ in range(30):
        try:
            out = rcon(args.host, args.port, args.password, args.command)
            print(out)
            return 0
        except Exception as exc:  # noqa: BLE001
            last_err = exc
            time.sleep(2)
    print(f"FAILED: {last_err}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
