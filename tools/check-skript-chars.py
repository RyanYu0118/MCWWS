import os
import sys

root = sys.argv[1] if len(sys.argv) > 1 else r"plugins/Skript/scripts"

for dirpath, _, files in os.walk(root):
    for fname in files:
        if not fname.endswith(".sk"):
            continue
        path = os.path.join(dirpath, fname)
        text = open(path, encoding="utf-8").read()
        for n, line in enumerate(text.splitlines(), 1):
            for i, ch in enumerate(line):
                o = ord(ch)
                if ch == "'" and o != 0x27:
                    print(f"SMART APOST {path}:{n}:{i} U+{o:04X} {line!r}")
                elif ch in ('"', '"', '"') and o not in (0x22,):
                    print(f"SMART QUOTE {path}:{n}:{i} U+{o:04X} {line!r}")
                elif o == 0x27 and i > 0 and line[i - 1] == "}":
                    # possessive after variable - verify ASCII
                    pass
