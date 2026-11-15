#!/usr/bin/env python3
"""Builds data/source/oxford.json (pack/2, five CEFR packs in one file) from the crawler dumps in data/source/raw/:
oxford3000_dictionary.json (A1-B2), oxford_b2_c1_dictionary.json (B2, C1 and the 16 headwords tagged "B2/C1")
and definition_overrides.json (hand-written definitions for headwords the dictionaries had no entry for).

Word ids are slug(term), with a _2, _3 ... suffix on a clash, assigned in list order: level by level, then the
position in the source list, then the term. These are the ids the app has been using, so progress survives. They are
written into the file; from here on the file is the source and the build script no longer generates them.
What is cleaned on the way: a bare field label dictionary.com gave as the first definition ("Chemistry.") is folded
into the next definition; the overrides are put first; the ipa falls back to the override's.
The raw dumps are kept in raw/ for their provenance and attribution (dictionaryapi.dev entries are CC BY-SA 3.0)."""
import json
import re
from pathlib import Path

SRC = Path(__file__).resolve().parent.parent / "data" / "source"
RAW = SRC / "raw"
LEVELS = ["A1", "A2", "B1", "B2", "C1"]
LEVEL_NAMES = {"A1": "Breakthrough", "A2": "Waystage", "B1": "Threshold", "B2": "Vantage", "C1": "Effective proficiency"}
LABEL = re.compile(r"^[A-Z][A-Za-z]*(?:[ ,/&]+[A-Za-z]+){0,3}\.$")


def slug(term):
    return re.sub(r"[^a-z0-9]+", "_", term.lower()).strip("_")


def senses_of(entry, dictionary_com):
    out = []
    for p in entry.get("pos", []):
        defs = [d.strip() for d in p["def_en"] if d and d.strip()]
        if dictionary_com and len(defs) >= 2 and len(defs[0].split()) <= 3 and LABEL.match(defs[0]):
            defs = [defs[0][:-1] + ": " + defs[1]] + defs[2:]
        out.extend({"pos": p["pos"].strip(), "definition": {"en": d}} for d in defs)
    return out


def main():
    a_doc = json.loads((RAW / "oxford3000_dictionary.json").read_text())
    b_doc = json.loads((RAW / "oxford_b2_c1_dictionary.json").read_text())
    fixes = json.loads((RAW / "definition_overrides.json").read_text())

    raw = []  # (levels, term, entry, order, from_dictionary_com)
    for term, e in a_doc["entries"].items():
        # "per cent" has no level in the source; its neighbours in the list are all A2.
        raw.append(([e["cefr"] or "A2"], term, e, e["list_index"], False))
    for term, e in b_doc["entries"].items():
        raw.append((["B2", "C1"] if e["cefr"] == "B2/C1" else [e["cefr"]], term, e, 10000 + e["rank"], True))

    entries, used, rank = [], set(), dict.fromkeys(LEVELS, 0)
    for levels, term, e, order, from_dc in sorted(raw, key=lambda x: (LEVELS.index(x[0][0]), x[3], x[1])):
        wid = slug(term) or "word"
        base, n = wid, 2
        while wid in used:
            wid, n = f"{base}_{n}", n + 1
        used.add(wid)
        senses, ipa, source = senses_of(e, from_dc), (e.get("ipa_us") or "").strip() or None, e.get("source")
        if term in fixes:
            f = fixes[term]
            senses = [{"pos": f["pos"], "definition": {"en": f["definition"]}}] + \
                     [s for s in senses if s["definition"]["en"] != f["definition"]]
            ipa, source = ipa or f.get("ipa_us"), f.get("source") or source
        rank[levels[0]] += 1
        entry = {"id": wid, "term": term, "lang": "en", "rank": rank[levels[0]], "packs": levels, "senses": senses}
        if ipa:
            entry["pronunciation"] = {"us": ipa}
        if e.get("pdf_pos"):
            entry["list_pos"] = e["pdf_pos"]  # the part of speech the Oxford list gives the headword
        if source:
            entry["source"] = source
        if e.get("needs_review"):
            entry["needs_review"] = True
        entries.append(entry)

    doc = {"format": "masterenglish.pack/2",
           "packs": [{"id": l, "title": {"en": f"{l}: {LEVEL_NAMES[l]}"}, "cefr_level": l} for l in LEVELS],
           "sources": [{"file": "raw/oxford3000_dictionary.json", **a_doc["_meta"]},
                       {"file": "raw/oxford_b2_c1_dictionary.json", **b_doc["_meta"]},
                       {"file": "raw/definition_overrides.json", "note": "Hand-written definitions for headwords "
                        "the dictionaries had no entry for; they come first in their word."}],
           "entries": entries}
    (SRC / "oxford.json").write_text(json.dumps(doc, ensure_ascii=False, indent=1) + "\n")
    print(len(entries), "entries", {l: rank[l] for l in LEVELS})


if __name__ == "__main__":
    main()
