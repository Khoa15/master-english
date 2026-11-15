#!/usr/bin/env python3
"""Builds app/src/main/assets/data/vocabulary.json from the Oxford source dictionaries in data/source/.

One row per (word, CEFR level). A word the source tags "B2/C1" gets a row in both levels.
The 1000-common-words list (all1000.json) is deliberately not used.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data" / "source"
OUT = ROOT / "app" / "src" / "main" / "assets" / "data" / "vocabulary.json"
LEVELS = ["A1", "A2", "B1", "B2", "C1"]


def slug(term):
    return re.sub(r"[^a-z0-9]+", "_", term.lower()).strip("_")


def short_pos(pos):
    base = re.split(r"[ (]", pos.strip(), maxsplit=1)[0].lower()
    return {"noun": "noun", "verb": "verb", "adjective": "adjective", "adverb": "adverb"}.get(base, pos.split("(")[0].strip().lower())


def definition(entry):
    defs = [d.strip() for p in entry["pos"] for d in p["def_en"] if d and d.strip()]
    if not defs:
        return ""
    first = defs[0]
    return first


def rows():
    a = json.loads((SRC / "oxford3000_dictionary.json").read_text())["entries"]
    b = json.loads((SRC / "oxford_b2_c1_dictionary.json").read_text())["entries"]
    out = []
    for term, e in a.items():
        # "per cent" has no level in the source; its neighbours in the list are all A2.
        out.append((e["cefr"] or "A2", term, e, e["list_index"]))
    for term, e in b.items():
        levels = ["B2", "C1"] if e["cefr"] == "B2/C1" else [e["cefr"]]
        for level in levels:
            out.append((level, term, e, 10000 + e["rank"]))
    result = []
    for level in LEVELS:
        items = sorted((x for x in out if x[0] == level), key=lambda x: (x[3], x[1]))
        for n, (lvl, term, e, _) in enumerate(items, 1):
            pos = e["pos"][0]["pos"] if e["pos"] else ""
            result.append({
                "id": f"ox_{lvl.lower()}_{slug(term)}",
                "term": term,
                "ipa": (e.get("ipa_us") or "").strip() or None,
                "part_of_speech": short_pos(pos) if pos else None,
                "cefr_level": lvl,
                "definition": definition(e),
                "order": n,
            })
    return result


if __name__ == "__main__":
    data = rows()
    ids = [r["id"] for r in data]
    assert len(ids) == len(set(ids)), "duplicate ids"
    OUT.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    for level in LEVELS:
        print(level, sum(1 for r in data if r["cefr_level"] == level))
    print("total", len(data))
