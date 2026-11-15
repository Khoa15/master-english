#!/usr/bin/env python3
"""Fills senses[].definition.en of data/source/christmas.json from https://api.dictionaryapi.dev.

christmas.json is a pack/2 file (see data/source/README.md). The curated fields (term, pronunciation, translations,
examples) are never touched; only senses[0].definition is (re)written. PICK says how to choose them per word:
  query          what to look up when it differs from the word
  hint           prefer an API definition containing this text
  max_defs       default 2
  fallback       used when the API has no entry or the entry is about another sense (stocking = a sock, carol = a nook)
  fallback_only  skip the API altogether
"""
import json, subprocess, time, urllib.parse
from pathlib import Path

FILE = Path(__file__).resolve().parent.parent / "data" / "source" / "christmas.json"
PICK = {
    "Father Christmas": {"query": "santa claus", "fallback": "The legendary old man in a red suit who brings presents to children at Christmas."},
    "chimney": {"max_defs": 1},
    "reindeer": {"max_defs": 1},
    "ornament": {"hint": "Christmas"},
    "Christmas stocking": {"query": "stocking", "fallback_only": True, "fallback": "A large sock hung up at Christmas to be filled with small presents."},
    "mistletoe": {"max_defs": 1},
    "Christmas Eve": {"fallback": "The evening or day before Christmas Day, 24 December."},
    "Christmas carol": {"query": "carol", "fallback_only": True, "fallback": "A traditional song sung at Christmas."},
    "snowman": {"max_defs": 1},
}


def fetch(q):
    url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + urllib.parse.quote(q)
    # curl rather than urllib: python.org builds on macOS ship without CA certificates.
    r = subprocess.run(["curl", "-s", "--retry", "3", "--retry-all-errors", "--max-time", "30", "-w", "\n%{http_code}", url],
                       capture_output=True, text=True, check=True)
    body, code = r.stdout.rsplit("\n", 1)
    if code == "404":
        return {}
    if code != "200":
        raise RuntimeError(f"{q}: HTTP {code}")
    out = {}
    for entry in json.loads(body):
        for m in entry.get("meanings", []):
            out.setdefault(m["partOfSpeech"], []).extend(d["definition"] for d in m["definitions"])
    return out


def main():
    doc = json.loads(FILE.read_text())
    for e in doc["entries"]:
        cfg = PICK.get(e["term"], {})
        first = e["senses"][0]
        pos = first["pos"]
        pool = [] if cfg.get("fallback_only") else fetch(cfg.get("query", e["term"].lower())).get(pos, [])
        hinted = [d for d in pool if cfg.get("hint") and cfg["hint"] in d]
        defs = list(dict.fromkeys(hinted + pool))[: cfg.get("max_defs", 2)] or [cfg["fallback"]]
        # One sense per definition; the translations and examples stay on the first one.
        e["senses"] = [dict(first, definition={"en": defs[0]})] + [{"pos": pos, "definition": {"en": d}} for d in defs[1:]]
        e["source_url"] = "https://api.dictionaryapi.dev/api/v2/entries/en/" + urllib.parse.quote(cfg.get("query", e["term"].lower()))
        print(e["term"], len(defs), "(API)" if pool else "(fallback)")
        time.sleep(0.5)
    FILE.write_text(json.dumps(doc, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
