#!/usr/bin/env python3
"""Converts a pack file in the old all1000-style shape (vietnamese / example / example_vi as flat fields) to the
pack/2 format described in data/source/README.md. Usage:
  convert_pack_v2.py <file> --pack-id TET --id-prefix tet_ --title-en "Tet Holiday" --title-vi "Tết" [--cefr-level TET]
The file is rewritten in place. Entry ids are <prefix><slug of the word>, the same ids build_vocabulary.py used to
derive, so learner progress survives the switch.

--ids-from-pack <PACK> takes the ids from that pack of the current app/src/main/assets/data/vocabulary.en.json instead
(entries sorted by rank, in the pack's order). Use it for a pack whose ids were generated at build time and that may
share words with other packs (all1000.json): run it before the build script is changed to read the new file."""
import argparse, json, re
from pathlib import Path


def slug(term):
    return re.sub(r"[^a-z0-9]+", "_", term.lower()).strip("_")


def pack_ids(pack):
    vocab = json.loads((Path(__file__).resolve().parent.parent / "app/src/main/assets/data/vocabulary.en.json").read_text())
    by_id = {w["id"]: w for w in vocab["words"]}
    ids = next(p for p in vocab["packs"] if p["id"] == pack)["words"]
    return ids, by_id


def convert(old, a):
    entries = []
    ids = by_id = None
    old_entries = old["entries"]
    if a.ids_from_pack:
        ids, by_id = pack_ids(a.ids_from_pack)
        old_entries = sorted(old_entries, key=lambda x: x["rank"])
        assert len(ids) == len(old_entries), (len(ids), len(old_entries))
    for n, e in enumerate(old_entries):
        senses = []
        for p in e["parts_of_speech"]:
            for d in p["definitions"]:
                senses.append({"pos": p["pos"], "definition": {"en": d}})
        # The flat Vietnamese meaning and the example belonged to the first sense.
        if e.get("vietnamese"):
            senses[0]["translations"] = {"vi": e["vietnamese"]}
        if e.get("example"):
            ex = {"text": e["example"]}
            if e.get("example_vi"):
                ex["translations"] = {"vi": e["example_vi"]}
            senses[0]["examples"] = [ex]
        wid = ids[n] if ids else a.id_prefix + slug(e["word"])
        if ids:
            # The id must point at the word the build used to make for this entry (same headword).
            assert by_id[wid]["term"].lower() == e["word"].strip().lower(), (wid, e["word"])
        out = {"id": wid, "term": e["word"].strip(), "lang": "en", "rank": e["rank"], "senses": senses}
        if e.get("pronunciation_us") and e["pronunciation_us"].strip():
            out["pronunciation"] = {"us": e["pronunciation_us"].strip()}
        if e.get("source_url"):
            out["source_url"] = e["source_url"]
        if e.get("also_called"):
            out["also_called"] = [e["also_called"]]
        entries.append(out)
    src = old.get("source_list", {})
    return {"format": "masterenglish.pack/2",
            "pack": {"id": a.pack_id, "title": {"en": a.title_en, "vi": a.title_vi},
                     "cefr_level": a.cefr_level or a.pack_id,
                     "source": {"name": src.get("name"), "url": src.get("url"),
                                "dictionary": old.get("dictionary_source"), "note": old.get("note")}},
            "entries": entries}


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("--pack-id", required=True)
    ap.add_argument("--id-prefix", default="")
    ap.add_argument("--ids-from-pack")
    ap.add_argument("--title-en", required=True)
    ap.add_argument("--title-vi", required=True)
    ap.add_argument("--cefr-level")
    a = ap.parse_args()
    p = Path(a.file)
    old = json.loads(p.read_text())
    if old.get("format") == "masterenglish.pack/2":
        raise SystemExit("already pack/2")
    p.write_text(json.dumps(convert(old, a), ensure_ascii=False, indent=1) + "\n")
