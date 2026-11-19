package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.CountRow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CountRows {

    private CountRows() {
    }

    /** {@code ref -> n} of rows returned by the "count per ..." queries. */
    static Map<String, Integer> toMap(List<CountRow> rows) {
        Map<String, Integer> map = new HashMap<>();
        for (CountRow r : rows) {
            map.put(r.ref, r.n);
        }
        return map;
    }
}
