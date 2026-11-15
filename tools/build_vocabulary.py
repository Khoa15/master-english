#!/usr/bin/env python3
"""Builds app/src/main/assets/data/vocabulary.json from the Oxford source dictionaries in data/source/.

Output shape (the app seeds its words, word_senses, packs and pack_words tables from it):
  {"packs": [{id, title, cefr_level, position}],
   "words": [{id, term, ipa, cefr_level, rank, packs: [pack ids], senses: [{pos, definition}]}]}

One word row per headword. A headword the source tags "B2/C1" is one word that belongs to both the B2 and
the C1 pack. The 1000-common-words list (all1000.json) is deliberately not used.
definition_overrides.json fills the headwords the dictionaries had no entry for.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data" / "source"
OUT = ROOT / "app" / "src" / "main" / "assets" / "data" / "vocabulary.json"
LEVELS = ["A1", "A2", "B1", "B2", "C1"]
LEVEL_NAMES = {"A1": "Breakthrough", "A2": "Waystage", "B1": "Threshold", "B2": "Vantage", "C1": "Effective proficiency"}
MAX_SENSES = 6


def slug(term):
    return re.sub(r"[^a-z0-9]+", "_", term.lower()).strip("_")


def short_pos(pos):
    base = re.split(r"[ (]", pos.strip(), maxsplit=1)[0].lower()
    return base or pos.strip().lower()


def senses_of(entry):
    out = []
    for p in entry.get("pos", []):
        for d in p["def_en"][:2]:
            d = d.strip()
            if d:
                out.append({"pos": short_pos(p["pos"]), "definition": d})
    return out[:MAX_SENSES]


def main():
    a = json.loads((SRC / "oxford3000_dictionary.json").read_text())["entries"]
    b = json.loads((SRC / "oxford_b2_c1_dictionary.json").read_text())["entries"]
    fixes = json.loads((SRC / "definition_overrides.json").read_text())

    raw = []  # (levels, term, entry, order)
    for term, e in a.items():
        # "per cent" has no level in the source; its neighbours in the list are all A2.
        raw.append(([e["cefr"] or "A2"], term, e, e["list_index"]))
    for term, e in b.items():
        raw.append((["B2", "C1"] if e["cefr"] == "B2/C1" else [e["cefr"]], term, e, 10000 + e["rank"]))

    words, used = [], set()
    for levels, term, e, order in sorted(raw, key=lambda x: (LEVELS.index(x[0][0]), x[3], x[1])):
        wid = slug(term) or "word"
        base, n = wid, 2
        while wid in used:
            wid, n = f"{base}_{n}", n + 1
        used.add(wid)
        senses, ipa = senses_of(e), (e.get("ipa_us") or "").strip() or None
        if term in fixes:
            f = fixes[term]
            senses = [{"pos": f["pos"], "definition": f["definition"]}] + [s for s in senses if s["definition"] != f["definition"]]
            ipa = ipa or f.get("ipa_us")
        words.append({"id": wid, "term": term, "ipa": ipa, "cefr_level": levels[0], "packs": levels, "senses": senses})
    for level in LEVELS:
        for i, w in enumerate((w for w in words if w["cefr_level"] == level), 1):
            w["rank"] = i
    packs = [{"id": l, "title": f"{l}: {LEVEL_NAMES[l]}", "cefr_level": l, "position": i} for i, l in enumerate(LEVELS)]

    OUT.write_text(json.dumps({"packs": packs, "words": words}, ensure_ascii=False, separators=(",", ":")))
    for l in LEVELS:
        print(l, sum(1 for w in words if l in w["packs"]))
    print("words", len(words), "senses", sum(len(w["senses"]) for w in words),
          "without definition", sum(1 for w in words if not w["senses"]))


if __name__ == "__main__":
    main()
