#!/usr/bin/env python3
"""Builds app/src/main/assets/data/vocabulary.json from the Oxford source dictionaries in data/source/.

Output shape (the app seeds its words, word_senses, packs and pack_words tables from it):
  {"packs": [{id, title, cefr_level, position, words: [word ids in pack order]}],
   "words": [{id, term, ipa, cefr_level, rank, senses: [{pos, definition}]}]}

One word row per headword. A headword the source tags "B2/C1" is one word that belongs to both the B2 and
the C1 pack. The 1000 most common words (all1000.json) are their own pack, "COMMON", in frequency order. They are never
used to build the CEFR packs. A common word that is also an Oxford headword is linked to that word (one card, one
set of definitions); the others become words of their own with level "COMMON".
definition_overrides.json fills the headwords the dictionaries had no entry for.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data" / "source"
OUT = ROOT / "app" / "src" / "main" / "assets" / "data" / "vocabulary.json"
LEVELS = ["A1", "A2", "B1", "B2", "C1"]
COMMON = "COMMON"
LEVEL_NAMES = {"A1": "Breakthrough", "A2": "Waystage", "B1": "Threshold", "B2": "Vantage", "C1": "Effective proficiency"}
MAX_SENSES = 6


def slug(term):
    return re.sub(r"[^a-z0-9]+", "_", term.lower()).strip("_")


def short_pos(pos):
    base = re.split(r"[ (]", pos.strip(), maxsplit=1)[0].lower()
    return base or pos.strip().lower()


LABEL = re.compile(r"^[A-Z][A-Za-z]*(?:[ ,/&]+[A-Za-z]+){0,3}\.$")


def senses_of(entry, dictionary_com=False):
    """Up to two definitions per part of speech. dictionary.com sometimes gives a bare field label ("Chemistry.")
    as the first definition; it is folded into the next one ("Chemistry: a substance with a sour taste.")."""
    out = []
    for p in entry.get("pos", []):
        defs = [d.strip() for d in p["def_en"] if d and d.strip()]
        if dictionary_com and len(defs) >= 2 and len(defs[0].split()) <= 3 and LABEL.match(defs[0]):
            defs = [defs[0][:-1] + ": " + defs[1]] + defs[2:]
        for d in defs[:2]:
            out.append({"pos": short_pos(p["pos"]), "definition": d})
    return out[:MAX_SENSES]


def main():
    a = json.loads((SRC / "oxford3000_dictionary.json").read_text())["entries"]
    b = json.loads((SRC / "oxford_b2_c1_dictionary.json").read_text())["entries"]
    fixes = json.loads((SRC / "definition_overrides.json").read_text())

    raw = []  # (levels, term, entry, order)
    for term, e in a.items():
        # "per cent" has no level in the source; its neighbours in the list are all A2.
        raw.append(([e["cefr"] or "A2"], term, e, e["list_index"], False))
    for term, e in b.items():
        raw.append((["B2", "C1"] if e["cefr"] == "B2/C1" else [e["cefr"]], term, e, 10000 + e["rank"], True))

    words, used = [], set()
    for levels, term, e, order, from_dc in sorted(raw, key=lambda x: (LEVELS.index(x[0][0]), x[3], x[1])):
        wid = slug(term) or "word"
        base, n = wid, 2
        while wid in used:
            wid, n = f"{base}_{n}", n + 1
        used.add(wid)
        senses, ipa = senses_of(e, from_dc), (e.get("ipa_us") or "").strip() or None
        if term in fixes:
            f = fixes[term]
            senses = [{"pos": f["pos"], "definition": f["definition"]}] + [s for s in senses if s["definition"] != f["definition"]]
            ipa = ipa or f.get("ipa_us")
        words.append({"id": wid, "term": term, "ipa": ipa, "cefr_level": levels[0], "packs": levels, "senses": senses})
    for level in LEVELS:
        for i, w in enumerate((w for w in words if w["cefr_level"] == level), 1):
            w["rank"] = i
    packs = [{"id": l, "title": f"{l}: {LEVEL_NAMES[l]}", "cefr_level": l, "position": i, "words": []}
             for i, l in enumerate(LEVELS)]
    by_id = {p["id"]: p for p in packs}
    for w in words:
        for level in w.pop("packs"):
            by_id[level]["words"].append(w["id"])

    # The 1000 most common words.
    by_term = {}
    for w in words:
        by_term.setdefault(w["term"].lower(), []).append(w)
    common = json.loads((SRC / "all1000.json").read_text())["entries"]
    common_ids, added = [], 0
    for e in sorted(common, key=lambda x: x["rank"]):
        term = e["word"].strip()
        hits = by_term.get(term.lower())
        if hits:
            hit = next((h for h in hits if h["term"] == term), hits[0])
            common_ids.append(hit["id"])
            continue
        wid = slug(term) or "word"
        base, n = wid, 2
        while wid in used:
            wid, n = f"{base}_{n}", n + 1
        used.add(wid)
        senses = []
        for p in e.get("parts_of_speech", []):
            for d in [d.strip() for d in p["definitions"] if d.strip()][:2]:
                senses.append({"pos": short_pos(p["pos"]), "definition": d})
        added += 1
        words.append({"id": wid, "term": term, "ipa": (e.get("pronunciation_us") or "").strip() or None,
                      "cefr_level": COMMON, "rank": e["rank"], "senses": senses[:MAX_SENSES]})
        common_ids.append(wid)
    packs.append({"id": COMMON, "title": "1000 Common Words", "cefr_level": COMMON, "position": len(packs),
                  "words": common_ids})

    OUT.write_text(json.dumps({"packs": packs, "words": words}, ensure_ascii=False, separators=(",", ":")))
    for p in packs:
        print(p["id"], len(p["words"]))
    print("common-only words", added)
    print("words", len(words), "senses", sum(len(w["senses"]) for w in words),
          "without definition", sum(1 for w in words if not w["senses"]))


if __name__ == "__main__":
    main()
