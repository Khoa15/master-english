#!/usr/bin/env python3
"""Builds app/src/main/assets/data/vocabulary.en.json from the pack/2 files in data/source/ (see its README.md).

Output shape (the app seeds its words, word_senses, translations, examples, packs and pack_words tables from it):
  {"packs": [{id, title, titles, cefr_level, position, words: [word ids in pack order]}],
   "words": [{id, term, ipa, cefr_level, rank, senses: [{pos, definition, definition_vi?, example?, example_vi?,
                                                         translations?, examples?}]}]}

One word row per entry id. A word can be in several packs: an Oxford headword tagged "B2/C1" is in both packs, and a
common word that is also an Oxford headword is only linked into the COMMON pack (one card, one set of definitions).
Pack order is the order of PACK_FILES. tools/convert_oxford_v2.py makes oxford.json from the dumps in data/source/raw/.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data" / "source"
OUT = ROOT / "app" / "src" / "main" / "assets" / "data" / "vocabulary.en.json"
MAX_SENSES = 6
# pack/2 files in pack order: (file, max definitions per part of speech, max senses per word).
# The two crawler-based files keep at most MAX_SENSES senses per word; all1000.json also has 3 definitions per part of
# speech, of which the app keeps 2.
PACK_FILES = [("oxford.json", None, MAX_SENSES), ("all1000.json", 2, MAX_SENSES),
              ("christmas.json", None, None), ("tet.json", None, None)]


def short_pos(pos):
    base = re.split(r"[ (]", pos.strip(), maxsplit=1)[0].lower()
    return base or pos.strip().lower()


def add_pack(words, packs, used, filename, max_defs=None, max_senses=None):
    """A pack/2 file: one pack ("pack") or several ("packs", each entry then lists its pack ids in "packs").
    Entries carry their own ids, so progress survives renames. An entry whose id is already a word (a common word
    that is also an Oxford headword) is only linked into its packs: the word keeps the content it already has.
    Senses keep the legacy keys (definition, definition_vi, example, example_vi, taken from the vi translation) and
    also carry the full translations / examples."""
    doc = json.loads((SRC / filename).read_text())
    assert doc.get("format") == "masterenglish.pack/2", filename
    metas = doc.get("packs") or [doc["pack"]]
    members = {m["id"]: [] for m in metas}
    level = {m["id"]: m["cefr_level"] for m in metas}
    by_id = {w["id"]: w for w in words}
    added = 0
    for e in doc["entries"]:
        pack_ids = e.get("packs") or [metas[0]["id"]]
        for pid in pack_ids:
            members[pid].append(e["id"])
        if e["id"] in by_id:
            continue
        used.add(e["id"])
        senses, per_pos = [], {}
        for s in e["senses"]:
            definition = s["definition"]["en"].strip()
            pos = short_pos(s["pos"])
            if not definition or (max_defs and per_pos.get(s["pos"], 0) >= max_defs):
                continue
            per_pos[s["pos"]] = per_pos.get(s["pos"], 0) + 1
            tr, exs = s.get("translations", {}), s.get("examples", [])
            assert all(x["text"] for x in exs), e["id"]
            sense = {"pos": pos, "definition": definition}
            if tr.get("vi"):
                sense["definition_vi"] = tr["vi"]
            if exs:
                sense["example"] = exs[0]["text"]
                if exs[0].get("translations", {}).get("vi"):
                    sense["example_vi"] = exs[0]["translations"]["vi"]
            if tr:
                sense["translations"] = tr
            if exs:
                sense["examples"] = exs
            senses.append(sense)
        word = {"id": e["id"], "term": e["term"], "ipa": (e.get("pronunciation", {}).get("us") or "").strip() or None,
                "cefr_level": level[pack_ids[0]], "rank": e["rank"], "senses": senses[:max_senses]}
        words.append(word)
        by_id[e["id"]] = word
        added += 1
    for m in metas:
        packs.append({"id": m["id"], "title": m["title"]["en"], "titles": m["title"], "cefr_level": m["cefr_level"],
                      "position": len(packs), "words": members[m["id"]]})
        print(m["id"], len(members[m["id"]]))
    print(filename, "words added", added)


def main():
    words, packs, used = [], [], set()
    for filename, max_defs, max_senses in PACK_FILES:
        add_pack(words, packs, used, filename, max_defs, max_senses)

    OUT.write_text(json.dumps({"packs": packs, "words": words}, ensure_ascii=False, separators=(",", ":")))
    print("words", len(words), "senses", sum(len(w["senses"]) for w in words),
          "without definition", sum(1 for w in words if not w["senses"]))


if __name__ == "__main__":
    main()
