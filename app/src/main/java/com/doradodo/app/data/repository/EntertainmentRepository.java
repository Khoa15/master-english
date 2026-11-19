package com.doradodo.app.data.repository;

import androidx.annotation.Nullable;

import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPrizeEntity;
import com.doradodo.app.data.local.entity.ContestRegistrationEntity;
import com.doradodo.app.data.local.entity.CountRow;
import com.doradodo.app.data.local.entity.EntTextEntity;
import com.doradodo.app.data.local.entity.EventEntity;
import com.doradodo.app.data.local.entity.GamePlayEntity;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.PackWordEntity;
import com.doradodo.app.data.local.entity.SynonymRow;
import com.doradodo.app.data.local.prefs.AppPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Everything behind the Entertainment page and its games. All calls hit Room, so they must run off the main
 * thread. The games and practice modes themselves live in the UI; this class only supplies their data.
 */
@Singleton
public class EntertainmentRepository {


    public enum ContestState { REGISTRATION_SOON, REGISTRATION, LIVE, ENDED }

    /** An activity with how many learners opened it. {@code learners} is 0 when nobody has. */
    public static final class ActivityCard {
        public final ActivityEntity activity;
        public final int learners;
        /** Extra count shown with the subtitle (e.g. the words the learner often gets wrong), or -1. */
        public final int count;

        ActivityCard(ActivityEntity activity, int learners, int count) {
            this.activity = activity;
            this.learners = learners;
            this.count = count;
        }
    }

    public static final class ContestCard {
        public final ContestEntity contest;
        public final ContestState state;
        public final boolean registered;
        public final int participants;
        public final int topPrizeGems;

        ContestCard(ContestEntity contest, ContestState state, boolean registered, int participants, int topPrizeGems) {
            this.contest = contest;
            this.state = state;
            this.registered = registered;
            this.participants = participants;
            this.topPrizeGems = topPrizeGems;
        }
    }

    public static final class Home {
        public final List<BannerEntity> banners;
        @Nullable public final EventEntity event;
        public final List<ContestCard> contests;
        public final List<ActivityCard> games;
        public final List<ActivityCard> practice;

        Home(List<BannerEntity> banners, @Nullable EventEntity event, List<ContestCard> contests,
             List<ActivityCard> games, List<ActivityCard> practice) {
            this.banners = banners;
            this.event = event;
            this.contests = contests;
            this.games = games;
            this.practice = practice;
        }
    }

    public static final class ContestDetail {
        public final ContestCard card;
        public final List<ContestPrizeEntity> prizes;

        ContestDetail(ContestCard card, List<ContestPrizeEntity> prizes) {
            this.card = card;
            this.prizes = prizes;
        }
    }

    /** Two words of a synonym group to be matched against each other. */
    public static final class SynonymPair {
        public final String left;
        public final String right;

        SynonymPair(String left, String right) {
            this.left = left;
            this.right = right;
        }
    }

    /** A pack (or the learner's own studied words) that a practice mode can read from. */
    public static final class WordSource {
        public static final String LEARNED = "LEARNED";
        public final String id;
        public final String title;
        public final int count;

        WordSource(String id, String title, int count) {
            this.id = id;
            this.title = title;
            this.count = count;
        }
    }

    private final EntertainmentDao dao;
    private final WordDao wordDao;
    private final AppPreferences preferences;
    private final LanguageRepository languages;
    private final Random random = new Random();

    @Inject
    public EntertainmentRepository(EntertainmentDao dao, WordDao wordDao, AppPreferences preferences,
                                   LanguageRepository languages) {
        this.dao = dao;
        this.wordDao = wordDao;
        this.preferences = preferences;
        this.languages = languages;
    }

    /** The language being learned: words, games, synonyms, packs and contests follow it. */
    public String learningLanguage() {
        return learning();
    }

    private String learning() {
        return languages.learningLanguage();
    }

    /** The language the app is shown in: meanings and catalogue texts follow it. */
    private String display() {
        return languages.displayLanguage();
    }

    // Ngữ cảnh only: public accessor, commented out with the context feature.
    // public String displayLanguage() {
    //     return languages.displayLanguage();
    // }

    /**
     * Replaces the default (English) texts of rows by their translation in the display language, when there is one.
     * The rows are plain objects loaded for this call, so they are changed in place.
     */
    private void localize(List<ActivityEntity> activities, List<BannerEntity> banners, List<EventEntity> events,
                          List<ContestEntity> contests) {
        String lang = display();
        Map<String, String> t = new HashMap<>();
        for (EntTextEntity row : dao.getTexts(lang)) {
            t.put(row.ownerType + "|" + row.ownerId + "|" + row.field, row.text);
        }
        if (t.isEmpty()) {
            return;
        }
        for (ActivityEntity a : activities) {
            a.title = t.getOrDefault("ACTIVITY|" + a.id + "|title", a.title);
            a.subtitle = t.getOrDefault("ACTIVITY|" + a.id + "|subtitle", a.subtitle);
        }
        for (BannerEntity b : banners) {
            b.tag = t.getOrDefault("BANNER|" + b.id + "|tag", b.tag);
            b.title = t.getOrDefault("BANNER|" + b.id + "|title", b.title);
            b.subtitle = t.getOrDefault("BANNER|" + b.id + "|subtitle", b.subtitle);
            b.buttonLabel = t.getOrDefault("BANNER|" + b.id + "|button_label", b.buttonLabel);
        }
        for (EventEntity e : events) {
            e.tag = t.getOrDefault("EVENT|" + e.id + "|tag", e.tag);
            e.title = t.getOrDefault("EVENT|" + e.id + "|title", e.title);
            e.description = t.getOrDefault("EVENT|" + e.id + "|description", e.description);
        }
        for (ContestEntity c : contests) {
            c.title = t.getOrDefault("CONTEST|" + c.id + "|title", c.title);
            c.description = t.getOrDefault("CONTEST|" + c.id + "|description", c.description);
        }
    }

    @Nullable
    private String userId() {
        return preferences.getCurrentUserId();
    }

    /** Only the activities designed for the language being learned (ent_activities.design_for). */
    private List<ActivityEntity> offered(String section, String lang) {
        List<ActivityEntity> out = new ArrayList<>();
        for (ActivityEntity a : dao.getActivities(section)) {
            if (a.designFor.contains(lang)) {
                out.add(a);
            }
        }
        localize(out, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        return out;
    }

    private String uid() {
        String id = userId();
        return id == null ? "" : id;
    }

    // ----- The page --------------------------------------------------------

    public Home loadHome() {
        long now = System.currentTimeMillis();
        String lang = learning();
        Map<String, Integer> learners = counts(dao.countLearnersPerActivity());
        int mistakes = dao.countMistakeWords(uid(), lang);
        Set<String> available = new HashSet<>();
        List<ActivityCard> games = new ArrayList<>();
        for (ActivityEntity a : offered(ActivityEntity.GAME, lang)) {
            available.add(a.code);
            games.add(new ActivityCard(a, learners.getOrDefault(a.code, 0), -1));
        }
        List<ActivityCard> practice = new ArrayList<>();
        for (ActivityEntity a : offered(ActivityEntity.PRACTICE, lang)) {
            available.add(a.code);
            practice.add(new ActivityCard(a, 0, "MISTAKES".equals(a.code) ? mistakes : -1));
        }
        Map<String, Integer> registrations = counts(dao.countRegistrations());
        List<ContestCard> contests = new ArrayList<>();
        List<ContestEntity> openContests = new ArrayList<>();
        for (ContestEntity c : dao.getOpenContests(now)) {
            // A contest is for the languages of the packs it draws from.
            if (dao.getContestLanguages(c.id).contains(lang)) {
                openContests.add(c);
            }
        }
        // A banner that opens a game this language is not designed for would lead nowhere.
        List<BannerEntity> banners = new ArrayList<>();
        for (BannerEntity b : dao.getBanners(now)) {
            boolean forLanguage = b.audienceLang == null || b.audienceLang.equals(lang);
            boolean opens = !BannerEntity.ACTIVITY.equals(b.actionType) || available.contains(b.actionTarget);
            if (forLanguage && opens) {
                banners.add(b);
            }
        }
        EventEntity event = dao.getCurrentEvent(now);
        if (event != null && event.audienceLang != null && !event.audienceLang.equals(lang)) {
            event = null;
        }
        List<EventEntity> eventList = new ArrayList<>();
        if (event != null) {
            eventList.add(event);
        }
        localize(new ArrayList<>(), banners, eventList, openContests);
        for (ContestEntity c : openContests) {
            contests.add(card(c, now, registrations.getOrDefault(c.id, 0)));
        }
        return new Home(banners, event, contests, games, practice);
    }

    public void recordPlay(String activityCode) {
        String id = userId();
        if (id == null) {
            return;
        }
        GamePlayEntity play = new GamePlayEntity();
        play.userId = id;
        play.activityCode = activityCode;
        play.playedAt = System.currentTimeMillis();
        dao.insertPlay(play);
    }

    public List<ActivityCard> loadGames() {
        Map<String, Integer> learners = counts(dao.countLearnersPerActivity());
        List<ActivityCard> games = new ArrayList<>();
        for (ActivityEntity a : offered(ActivityEntity.GAME, learning())) {
            games.add(new ActivityCard(a, learners.getOrDefault(a.code, 0), -1));
        }
        return games;
    }

    private static Map<String, Integer> counts(List<CountRow> rows) {
        Map<String, Integer> map = new HashMap<>();
        for (CountRow r : rows) {
            map.put(r.ref, r.n);
        }
        return map;
    }

    // ----- Contests --------------------------------------------------------

    public static ContestState stateOf(ContestEntity c, long now) {
        if (now >= c.endsAt) {
            return ContestState.ENDED;
        }
        if (now >= c.startsAt) {
            return ContestState.LIVE;
        }
        return now < c.registrationOpensAt ? ContestState.REGISTRATION_SOON : ContestState.REGISTRATION;
    }

    private ContestCard card(ContestEntity c, long now, int participants) {
        int top = 0;
        for (ContestPrizeEntity p : dao.getPrizes(c.id)) {
            top = Math.max(top, p.rewardGems);
        }
        return new ContestCard(c, stateOf(c, now), dao.isRegistered(c.id, uid()) > 0, participants, top);
    }

    @Nullable
    public ContestDetail loadContest(String contestId) {
        ContestEntity c = dao.getContest(contestId);
        if (c == null) {
            return null;
        }
        int participants = counts(dao.countRegistrations()).getOrDefault(c.id, 0);
        List<ContestEntity> one = new ArrayList<>();
        one.add(c);
        localize(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), one);
        return new ContestDetail(card(c, System.currentTimeMillis(), participants),
                dao.getPrizes(c.id));
    }

    /** True when the registration window is open right now. */
    public static boolean canRegister(ContestEntity c, long now) {
        return now >= c.registrationOpensAt && now < c.registrationClosesAt && now < c.endsAt;
    }

    /** Registers the learner; false when the window is closed or there is no learner. */
    public boolean register(String contestId) {
        String id = userId();
        ContestEntity c = dao.getContest(contestId);
        long now = System.currentTimeMillis();
        if (id == null || c == null || !canRegister(c, now)) {
            return false;
        }
        ContestRegistrationEntity r = new ContestRegistrationEntity();
        r.contestId = contestId;
        r.userId = id;
        r.registeredAt = now;
        dao.register(r);
        return true;
    }

    /** Questions of a contest: words of its packs, in random order. */
    public List<GameWord> contestWords(String contestId, int limit) {
        List<String> packs = dao.getContestPackIds(contestId);
        if (packs.isEmpty()) {
            return new ArrayList<>();
        }
        return shorten(dao.getPackWords(uid(), packs, display(), limit));
    }

    // ----- Words for the games ---------------------------------------------

    public int learnedCount() {
        return dao.countLearned(uid(), learning());
    }

    /** Studied words that have a meaning (and, if asked, a definition), random order. */
    public List<GameWord> learnedWords(int limit, boolean needDefinition) {
        return shorten(dao.getLearnedWords(uid(), learning(), display(), false, limit));
    }

    public List<GameWord> mistakeWords(int limit) {
        return shorten(dao.getMistakeWords(uid(), learning(), display(), limit));
    }

    /** Words to use as wrong answers; never one of {@code exclude}. */
    public List<GameWord> distractors(Set<String> exclude, int limit) {
        List<GameWord> out = new ArrayList<>();
        for (GameWord w : shorten(dao.getRandomWords(uid(), learning(), display(), limit + exclude.size() + 8))) {
            if (!exclude.contains(w.id) && out.size() < limit) {
                out.add(w);
            }
        }
        return out;
    }

    /**
     * Pairs of synonyms in the language the learner is learning: each pair is two members of one group. Synonymy never
     * crosses languages, so both words are in that one language. Groups with a word the learner studied come
     * first, and no text appears twice, so every answer is unambiguous.
     */
    public List<SynonymPair> synonymPairs(int limit) {
        Map<Long, List<SynonymRow>> groups = new LinkedHashMap<>();
        for (SynonymRow r : dao.getSynonymMembers(uid(), learning())) {
            groups.computeIfAbsent(r.groupId, k -> new ArrayList<>()).add(r);
        }
        List<List<SynonymRow>> usable = new ArrayList<>();
        for (List<SynonymRow> members : groups.values()) {
            if (members.size() >= 2) {
                usable.add(members);
            }
        }
        Collections.shuffle(usable, random);
        // Stable sort: groups the learner knows move to the front, the shuffle order is kept inside each half.
        Collections.sort(usable, (a, b) -> Boolean.compare(knows(b), knows(a)));
        List<SynonymPair> pairs = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (List<SynonymRow> members : usable) {
            if (pairs.size() >= limit) {
                break;
            }
            List<SynonymRow> shuffled = new ArrayList<>(members);
            Collections.shuffle(shuffled, random);
            // Prefer a studied word on the left.
            Collections.sort(shuffled, (a, b) -> Boolean.compare(b.learned, a.learned));
            SynonymRow left = null;
            SynonymRow right = null;
            for (SynonymRow m : shuffled) {
                if (used.contains(m.text.toLowerCase())) {
                    continue;
                }
                if (left == null) {
                    left = m;
                } else {
                    right = m;
                    break;
                }
            }
            if (left != null && right != null) {
                used.add(left.text.toLowerCase());
                used.add(right.text.toLowerCase());
                pairs.add(new SynonymPair(left.text, right.text));
            }
        }
        return pairs;
    }

    private static boolean knows(List<SynonymRow> members) {
        for (SynonymRow m : members) {
            if (m.learned) {
                return true;
            }
        }
        return false;
    }

    /** Longest meaning that fits a game tile. */
    private static final int MAX_MEANING = 60;

    /** Cuts each meaning to its first clause so a long English definition still fits a tile. */
    private static List<GameWord> shorten(List<GameWord> words) {
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

    // ----- Chanting and sleeping -------------------------------------------

    /** The learner's own studied words first, then every pack. */
    public List<WordSource> wordSources() {
        List<WordSource> out = new ArrayList<>();
        int learned = dao.countLearned(uid(), learning());
        if (learned > 0) {
            out.add(new WordSource(WordSource.LEARNED, "", learned));
        }
        Map<String, Integer> sizes = new HashMap<>();
        for (PackWordEntity pw : wordDao.getPackWords(learning())) {
            sizes.merge(pw.packId, 1, Integer::sum);
        }
        for (PackEntity p : wordDao.getPacks(learning())) {
            out.add(new WordSource(p.id, p.title, sizes.getOrDefault(p.id, 0)));
        }
        return out;
    }

    /** Words of a source with their meaning; a pack keeps its list order, studied words come shuffled. */
    public List<GameWord> sourceWords(String sourceId, int limit) {
        if (WordSource.LEARNED.equals(sourceId)) {
            return shorten(dao.getLearnedWords(uid(), learning(), display(), false, limit));
        }
        return shorten(dao.getPackWordsInOrder(uid(), sourceId, display(), limit));
    }
}
