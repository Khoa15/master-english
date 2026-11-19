package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;

import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.EntTextEntity;
import com.doradodo.app.data.local.entity.EventEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class EntTextLocalizerTest {

    private static EntTextEntity text(String type, String id, String field, String value) {
        EntTextEntity t = new EntTextEntity();
        t.ownerType = type;
        t.ownerId = id;
        t.field = field;
        t.lang = "vi";
        t.text = value;
        return t;
    }

    private static ActivityEntity activity(String id) {
        ActivityEntity a = new ActivityEntity();
        a.id = id;
        a.title = "Title " + id;
        a.subtitle = "Sub " + id;
        return a;
    }

    @Test
    public void translatesOnlyTheFieldsThatHaveATranslation() {
        EntTextLocalizer l = new EntTextLocalizer(Arrays.asList(text("ACTIVITY", "a1", "title", "Tiêu đề")));
        ActivityEntity a = activity("a1");
        l.localize(Collections.singletonList(a), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        assertEquals("Tiêu đề", a.title);
        assertEquals("Sub a1", a.subtitle);
    }

    @Test
    public void doesNotMixOwnersOfTheSameId() {
        EntTextLocalizer l = new EntTextLocalizer(Arrays.asList(text("BANNER", "x", "title", "Banner vi")));
        ActivityEntity a = activity("x");
        BannerEntity b = new BannerEntity();
        b.id = "x";
        b.title = "Banner";
        l.localize(Collections.singletonList(a), Collections.singletonList(b), new ArrayList<>(), new ArrayList<>());
        assertEquals("Title x", a.title);
        assertEquals("Banner vi", b.title);
    }

    @Test
    public void translatesEventsAndContests() {
        EntTextLocalizer l = new EntTextLocalizer(Arrays.asList(
                text("EVENT", "e", "description", "Mô tả"), text("CONTEST", "c", "title", "Cuộc thi")));
        EventEntity e = new EventEntity();
        e.id = "e";
        e.description = "desc";
        ContestEntity c = new ContestEntity();
        c.id = "c";
        c.title = "Contest";
        l.localize(new ArrayList<>(), new ArrayList<>(), Collections.singletonList(e), Collections.singletonList(c));
        assertEquals("Mô tả", e.description);
        assertEquals("Cuộc thi", c.title);
    }

    @Test
    public void noTranslationsChangesNothing() {
        EntTextLocalizer l = new EntTextLocalizer(new ArrayList<>());
        List<ActivityEntity> list = Collections.singletonList(activity("a"));
        l.localize(list, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        assertEquals("Title a", list.get(0).title);
    }

    @Test
    public void aWeeklyContestUsesTheTranslationOfItsTemplate() {
        EntTextLocalizer l = new EntTextLocalizer(Arrays.asList(text("CONTEST", "weekly_cup", "title", "Cúp tuần")));
        ContestEntity c = new ContestEntity();
        c.id = "weekly_cup@2026-W47";
        c.title = "Weekly Cup";
        l.localize(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Collections.singletonList(c));
        assertEquals("Cúp tuần", c.title);
    }
}
