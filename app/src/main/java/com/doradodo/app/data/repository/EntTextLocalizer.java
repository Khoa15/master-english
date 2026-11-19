package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.EntTextEntity;
import com.doradodo.app.data.local.entity.EventEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replaces the default (English) texts of catalogue rows by their translation, when there is one. The rows are
 * plain objects loaded for the call, so they are changed in place.
 */
public final class EntTextLocalizer {

    private final Map<String, String> texts = new HashMap<>();

    /** @param rows the translations in one language */
    public EntTextLocalizer(List<EntTextEntity> rows) {
        for (EntTextEntity row : rows) {
            texts.put(row.ownerType + "|" + row.ownerId + "|" + row.field, row.text);
        }
    }

    public void localize(List<ActivityEntity> activities, List<BannerEntity> banners, List<EventEntity> events,
                         List<ContestEntity> contests) {
        if (texts.isEmpty()) {
            return;
        }
        for (ActivityEntity a : activities) {
            a.title = text("ACTIVITY", a.id, "title", a.title);
            a.subtitle = text("ACTIVITY", a.id, "subtitle", a.subtitle);
        }
        for (BannerEntity b : banners) {
            b.tag = text("BANNER", b.id, "tag", b.tag);
            b.title = text("BANNER", b.id, "title", b.title);
            b.subtitle = text("BANNER", b.id, "subtitle", b.subtitle);
            b.buttonLabel = text("BANNER", b.id, "button_label", b.buttonLabel);
        }
        for (EventEntity e : events) {
            e.tag = text("EVENT", e.id, "tag", e.tag);
            e.title = text("EVENT", e.id, "title", e.title);
            e.description = text("EVENT", e.id, "description", e.description);
        }
        for (ContestEntity c : contests) {
            // A weekly contest is "<template id>@<week>": every week shares the template's translation.
            String template = c.id.contains("@") ? c.id.substring(0, c.id.indexOf('@')) : c.id;
            c.title = text("CONTEST", template, "title", c.title);
            c.description = text("CONTEST", template, "description", c.description);
        }
    }

    private String text(String owner, String id, String field, String fallback) {
        return texts.getOrDefault(owner + "|" + id + "|" + field, fallback);
    }
}
