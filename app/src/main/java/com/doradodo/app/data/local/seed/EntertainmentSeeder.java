package com.doradodo.app.data.local.seed;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.doradodo.app.data.local.AppDatabase;
import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPackEntity;
import com.doradodo.app.data.local.entity.ContestPrizeEntity;
import com.doradodo.app.data.local.entity.EntTextEntity;
import com.doradodo.app.data.local.entity.EventEntity;
import com.doradodo.app.data.local.entity.SynonymGroupEntity;
import com.doradodo.app.data.local.entity.SynonymMemberEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Fills the Entertainment tables from assets/data/entertainment.json: the activity catalogue, carousel, events,
 * contests and, from assets/data/synonyms.<lang>.json (one file per language), the synonym groups. Rows are upserted and the ones missing from the file removed, so a content
 * update keeps learners' contest registrations. Times in the file are offsets in hours from the moment of seeding.
 * Must run after {@link LocalDataSeeder}: contests point at word packs and synonyms link to words.
 */
@Singleton
public class EntertainmentSeeder {

    /** Raise this when assets/data/entertainment.json or a synonyms.<lang>.json changes. */
    public static final int CONTENT_VERSION = 6;

    private static final String TAG = "EntertainmentSeeder";
    private static final String PREFS = "entertainment_seed";
    private static final String KEY_VERSION = "version";
    private static final long HOUR_MS = 3_600_000L;

    private final Context context;
    private final AppDatabase database;
    private final Gson gson = new Gson();

    @Inject
    public EntertainmentSeeder(@ApplicationContext Context context, AppDatabase database) {
        this.context = context;
        this.database = database;
    }

    public synchronized void seedIfNeeded() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getInt(KEY_VERSION, 0) == CONTENT_VERSION) {
            return;
        }
        try (InputStream stream = context.getAssets().open("data/entertainment.json");
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            File file = gson.fromJson(reader, File.class);
            if (file == null) {
                throw new IOException("entertainment.json is empty");
            }
            readSynonyms(file);
            readTexts(file);
            long now = System.currentTimeMillis();
            database.runInTransaction(() -> write(database.entertainmentDao(), file, now));
            prefs.edit().putInt(KEY_VERSION, CONTENT_VERSION).apply();
            Log.i(TAG, "Seeded entertainment content v" + CONTENT_VERSION);
        } catch (IOException e) {
            Log.e(TAG, "Could not read bundled entertainment content", e);
        }
    }

    private static final java.util.regex.Pattern SYNONYM_FILE = java.util.regex.Pattern.compile("synonyms\\.([A-Za-z-]+)\\.json");
    private static final java.util.regex.Pattern TEXT_FILE = java.util.regex.Pattern.compile("ent_texts\\.([A-Za-z-]+)\\.json");

    /** Reads every asset named by the pattern, one file per language, and hands each with its language code. */
    private <F> void readPerLanguage(java.util.regex.Pattern pattern, Class<F> type,
                                     java.util.function.BiConsumer<F, String> consumer) throws IOException {
        String[] names = context.getAssets().list("data");
        java.util.Arrays.sort(names == null ? new String[0] : names);
        for (String name : names == null ? new String[0] : names) {
            java.util.regex.Matcher m = pattern.matcher(name);
            if (!m.matches()) {
                continue;
            }
            try (InputStream stream = context.getAssets().open("data/" + name);
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                F parsed = gson.fromJson(reader, type);
                if (parsed == null) {
                    throw new IOException(name + " is empty");
                }
                consumer.accept(parsed, m.group(1));
            }
        }
    }

    /** synonyms.<lang>.json: the language comes from the file name unless the file states it. */
    private void readSynonyms(File file) throws IOException {
        readPerLanguage(SYNONYM_FILE, SynonymFile.class, (sf, fileLang) -> {
            String lang = sf.lang == null || sf.lang.isEmpty() ? fileLang : sf.lang;
            for (SynonymJson g : sf.groups) {
                g.lang = lang;
                file.synonyms.add(g);
            }
        });
    }

    /** ent_texts.<lang>.json: the catalogue's text for one display language. */
    private void readTexts(File file) throws IOException {
        readPerLanguage(TEXT_FILE, TextFile.class, (tf, fileLang) -> {
            String lang = tf.lang == null || tf.lang.isEmpty() ? fileLang : tf.lang;
            for (TextJson row : tf.texts) {
                row.lang = lang;
                file.texts.add(row);
            }
        });
    }

    static final class TextFile {
        String lang;
        List<TextJson> texts = new ArrayList<>();
    }

    static final class SynonymFile {
        String lang;
        List<SynonymJson> groups = new ArrayList<>();
    }

    private void write(EntertainmentDao dao, File file, long now) {
        List<String> ids = new ArrayList<>();
        List<ActivityEntity> activities = new ArrayList<>();
        for (ActivityJson a : file.activities) {
            ActivityEntity e = new ActivityEntity();
            e.id = a.id;
            e.section = a.section;
            e.code = a.code;
            e.title = a.title;
            e.subtitle = a.subtitle;
            e.image = a.image;
            e.accent = a.accent;
            e.designFor = a.designFor == null || a.designFor.isEmpty()
                    ? new ArrayList<>(java.util.Collections.singletonList("en")) : a.designFor;
            e.position = a.position;
            activities.add(e);
            ids.add(e.id);
        }
        // The unique index on code means a renamed row must go before its replacement comes in.
        dao.deleteActivitiesExcept(ids);
        dao.upsertActivities(activities);

        ids = new ArrayList<>();
        List<BannerEntity> banners = new ArrayList<>();
        for (BannerJson b : file.banners) {
            BannerEntity e = new BannerEntity();
            e.id = b.id;
            e.tag = b.tag;
            e.title = b.title;
            e.subtitle = b.subtitle;
            e.buttonLabel = b.buttonLabel;
            e.image = b.image;
            e.accent = b.accent;
            e.actionType = b.actionType;
            e.actionTarget = b.actionTarget;
            e.position = b.position;
            banners.add(e);
            ids.add(e.id);
        }
        dao.deleteBannersExcept(ids);
        dao.upsertBanners(banners);

        ids = new ArrayList<>();
        List<EventEntity> events = new ArrayList<>();
        for (EventJson ev : file.events) {
            EventEntity e = new EventEntity();
            e.id = ev.id;
            e.tag = ev.tag;
            e.title = ev.title;
            e.description = ev.description;
            e.image = ev.image;
            e.accent = ev.accent;
            e.startsAt = now + ev.startsOffsetHours * HOUR_MS;
            e.endsAt = now + ev.endsOffsetHours * HOUR_MS;
            events.add(e);
            ids.add(e.id);
        }
        dao.deleteEventsExcept(ids);
        dao.upsertEvents(events);

        Set<String> packIds = new HashSet<>(database.wordDao().getPackIds());
        ids = new ArrayList<>();
        List<ContestEntity> contests = new ArrayList<>();
        List<ContestPackEntity> packs = new ArrayList<>();
        List<ContestPrizeEntity> prizes = new ArrayList<>();
        for (ContestJson c : file.contests) {
            ContestEntity e = new ContestEntity();
            e.id = c.id;
            e.title = c.title;
            e.description = c.description;
            e.image = c.image;
            e.accent = c.accent;
            e.registrationOpensAt = now + c.registrationOpensHours * HOUR_MS;
            e.registrationClosesAt = now + c.registrationClosesHours * HOUR_MS;
            e.startsAt = now + c.startsHours * HOUR_MS;
            e.endsAt = now + c.endsHours * HOUR_MS;
            e.questionCount = c.questionCount;
            e.durationMinutes = c.durationMinutes;
            contests.add(e);
            ids.add(e.id);
            int position = 0;
            for (String packId : c.packs) {
                if (packIds.contains(packId)) {
                    ContestPackEntity p = new ContestPackEntity();
                    p.contestId = c.id;
                    p.packId = packId;
                    p.position = position++;
                    packs.add(p);
                }
            }
            for (int[] pr : toRows(c.prizes)) {
                ContestPrizeEntity p = new ContestPrizeEntity();
                p.contestId = c.id;
                p.rankFrom = pr[0];
                p.rankTo = pr[1] == 0 ? null : pr[1];
                p.rewardGems = pr[2];
                prizes.add(p);
            }
        }
        dao.deleteContestsExcept(ids);
        dao.upsertContests(contests);
        dao.deleteContestPacks();
        dao.deleteContestPrizes();
        dao.insertContestPacks(packs);
        dao.insertContestPrizes(prizes);

        // Texts per language: the columns above hold the English default, these override it per display language.
        List<EntTextEntity> texts = new ArrayList<>();
        for (TextJson t : file.texts) {
            EntTextEntity row = new EntTextEntity();
            row.ownerType = t.type;
            row.ownerId = t.id;
            row.lang = t.lang;
            row.field = t.field;
            row.text = t.text;
            texts.add(row);
        }
        dao.deleteTexts();
        dao.insertTexts(texts);

        dao.deleteSynonymGroups();
        for (SynonymJson group : file.synonyms) {
            SynonymGroupEntity g = new SynonymGroupEntity();
            g.lang = group.lang;
            g.note = group.note;
            long groupId = dao.insertSynonymGroup(g);
            List<SynonymMemberEntity> members = new ArrayList<>();
            for (String text : group.words) {
                SynonymMemberEntity m = new SynonymMemberEntity();
                m.groupId = groupId;
                m.text = text;
                // Only English text can match a headword of the vocabulary today.
                m.wordId = "en".equals(group.lang) ? dao.findWordId(text) : null;
                members.add(m);
            }
            dao.insertSynonymMembers(members);
        }
    }

    /** Prize rows as {from, to (0 = open ended), gems}. */
    private static List<int[]> toRows(List<List<Integer>> prizes) {
        List<int[]> rows = new ArrayList<>();
        for (List<Integer> p : prizes) {
            rows.add(new int[]{p.get(0), p.get(1) == null ? 0 : p.get(1), p.get(2)});
        }
        return rows;
    }

    // ----- File shape --------------------------------------------------------

    static final class File {
        List<ActivityJson> activities = new ArrayList<>();
        List<BannerJson> banners = new ArrayList<>();
        List<EventJson> events = new ArrayList<>();
        List<ContestJson> contests = new ArrayList<>();
        List<SynonymJson> synonyms = new ArrayList<>();
        List<TextJson> texts = new ArrayList<>();
    }

    static final class TextJson {
        String type;
        String id;
        String lang;
        String field;
        String text;
    }

    static final class SynonymJson {
        String lang;
        String note;
        List<String> words = new ArrayList<>();
    }

    static final class ActivityJson {
        String id;
        String section;
        String code;
        String title;
        String subtitle;
        String image;
        String accent;
        @SerializedName("design_for") List<String> designFor;
        int position;
    }

    static final class BannerJson {
        String id;
        String tag;
        String title;
        String subtitle;
        @SerializedName("button_label") String buttonLabel;
        String image;
        String accent;
        @SerializedName("action_type") String actionType;
        @SerializedName("action_target") String actionTarget;
        int position;
    }

    static final class EventJson {
        String id;
        String tag;
        String title;
        String description;
        String image;
        String accent;
        @SerializedName("starts_offset_hours") long startsOffsetHours;
        @SerializedName("ends_offset_hours") long endsOffsetHours;
    }

    static final class ContestJson {
        String id;
        String title;
        String description;
        String image;
        String accent;
        @SerializedName("registration_opens_h") long registrationOpensHours;
        @SerializedName("registration_closes_h") long registrationClosesHours;
        @SerializedName("starts_h") long startsHours;
        @SerializedName("ends_h") long endsHours;
        @SerializedName("question_count") int questionCount;
        @SerializedName("duration_minutes") int durationMinutes;
        List<String> packs = new ArrayList<>();
        List<List<Integer>> prizes = new ArrayList<>();
    }
}
