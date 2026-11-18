"""Plural strings: key -> {lang: {quantity: text}}. Every language needs "other"; add the quantities its
CLDR plural rules use (ru: one/few/many)."""

PLURALS = {
 "plural_words": {
  "en": {"one": "%1$d word", "other": "%1$d words"},
  "vi": {"other": "%1$d từ"},
  "zh": {"other": "%1$d 个单词"},
  "hi": {"one": "%1$d शब्द", "other": "%1$d शब्द"},
  "es": {"one": "%1$d palabra", "many": "%1$d de palabras", "other": "%1$d palabras"},
  "fr": {"one": "%1$d mot", "many": "%1$d de mots", "other": "%1$d mots"},
  "pt": {"one": "%1$d palavra", "many": "%1$d de palavras", "other": "%1$d palavras"},
  "ru": {"one": "%1$d слово", "few": "%1$d слова", "many": "%1$d слов", "other": "%1$d слова"},
  "id": {"other": "%1$d kata"},
 },
 "plural_packs": {
  "en": {"one": "%1$d pack", "other": "%1$d packs"},
  "vi": {"other": "%1$d bộ"},
  "zh": {"other": "%1$d 个词库"},
  "hi": {"one": "%1$d पैक", "other": "%1$d पैक"},
  "es": {"one": "%1$d paquete", "many": "%1$d de paquetes", "other": "%1$d paquetes"},
  "fr": {"one": "%1$d paquet", "many": "%1$d de paquets", "other": "%1$d paquets"},
  "pt": {"one": "%1$d pacote", "many": "%1$d de pacotes", "other": "%1$d pacotes"},
  "ru": {"one": "%1$d набор", "few": "%1$d набора", "many": "%1$d наборов", "other": "%1$d набора"},
  "id": {"other": "%1$d paket"},
 },
 "plural_stars": {
  "en": {"one": "%1$d star", "other": "%1$d stars"},
  "vi": {"other": "%1$d sao"},
  "zh": {"other": "%1$d 星"},
  "hi": {"one": "%1$d सितारा", "other": "%1$d सितारे"},
  "es": {"one": "%1$d estrella", "many": "%1$d de estrellas", "other": "%1$d estrellas"},
  "fr": {"one": "%1$d étoile", "many": "%1$d d'étoiles", "other": "%1$d étoiles"},
  "pt": {"one": "%1$d estrela", "many": "%1$d de estrelas", "other": "%1$d estrelas"},
  "ru": {"one": "%1$d звезда", "few": "%1$d звезды", "many": "%1$d звёзд", "other": "%1$d звезды"},
  "id": {"other": "%1$d bintang"},
 },
}
