"""THE list of UI languages: the only place a language is added, removed or reordered.

Everything else is derived from LANGUAGES -- the column order of every strings_*.py table, the res/values*/ folders,
res/xml/locales_config.xml and res/values/languages.xml (read by AppLanguages.java for the onboarding list, the
Profile picker and validation). After editing, run build_strings.py and add or drop that column in each table.

Indonesian uses the legacy "in" folder name, which every Android version understands.
"""
# (BCP-47 code, name written in the language itself, res folder)
LANGUAGES = [
    ("en", "English", "values"),
    ("vi", "Tiếng Việt", "values-vi"),
    ("zh", "中文", "values-zh"),
    ("hi", "हिन्दी", "values-hi"),
    ("es", "Español", "values-es"),
    ("fr", "Français", "values-fr"),
    ("pt", "Português", "values-pt"),
    ("ru", "Русский", "values-ru"),
    ("id", "Bahasa Indonesia", "values-in"),
]

LANGS = [code for code, _, _ in LANGUAGES]
NAMES = {code: name for code, name, _ in LANGUAGES}
FOLDERS = {code: folder for code, _, folder in LANGUAGES}


def S(key, *texts):
    """One string: its key and one text per language, in LANGS order."""
    assert len(texts) == len(LANGS), (key, len(texts))
    return key, dict(zip(LANGS, texts))
