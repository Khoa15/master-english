package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.GameWord;

import java.util.List;

/** Cuts meanings to what fits a game tile. */
public final class MeaningShortener {

    /** Longest meaning that fits a game tile. */
    static final int MAX_MEANING = 60;

    private MeaningShortener() {
    }

    /** Cuts each meaning to its first clause so a long English definition still fits a tile. */
    public static List<GameWord> shorten(List<GameWord> words) {
        for (GameWord w : words) {
            if (w.meaning == null) {
                continue;
            }
            String m = w.meaning.trim();
            int cut = m.length();
            for (char sep : new char[]{';', '.', '('}) {
                int i = m.indexOf(sep);
                if (i > 3 && i < cut) {
                    cut = i;
                }
            }
            m = m.substring(0, cut).trim();
            if (m.length() > MAX_MEANING) {
                String head = m.substring(0, MAX_MEANING);
                int space = head.lastIndexOf(' ');
                m = (space > 20 ? head.substring(0, space) : head).trim() + "…";
            }
            w.meaning = m;
        }
        return words;
    }
}
