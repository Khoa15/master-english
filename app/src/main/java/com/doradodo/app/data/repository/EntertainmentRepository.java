package com.doradodo.app.data.repository;

import androidx.annotation.Nullable;

import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.EventEntity;
import com.doradodo.app.data.local.entity.GamePlayEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The Entertainment page: its activity catalogue, banners, event and contests. Contests live in
 * {@link ContestRepository} and the games' words in {@link GameWordRepository}. All calls hit Room, so they must
 * run off the main thread.
 */
@Singleton
public class EntertainmentRepository {

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

    private final EntertainmentDao dao;
    private final LearnerContext learner;
    private final ContestRepository contests;

    @Inject
    public EntertainmentRepository(EntertainmentDao dao, LearnerContext learner, ContestRepository contests) {
        this.dao = dao;
        this.learner = learner;
        this.contests = contests;
    }

    /** The language being learned: words, games, synonyms, packs and contests follow it. */
    public String learningLanguage() {
        return learner.learningLanguage();
    }

    private EntTextLocalizer localizer() {
        return new EntTextLocalizer(dao.getTexts(learner.displayLanguage()));
    }

    /** Only the activities designed for the language being learned (ent_activities.design_for). */
    private List<ActivityEntity> offered(String section, String lang) {
        List<ActivityEntity> out = new ArrayList<>();
        for (ActivityEntity a : dao.getActivities(section)) {
            if (a.designFor.contains(lang)) {
                out.add(a);
            }
        }
        localizer().localize(out, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        return out;
    }

    public Home loadHome() {
        long now = System.currentTimeMillis();
        String lang = learningLanguage();
        Map<String, Integer> learners = CountRows.toMap(dao.countLearnersPerActivity());
        int mistakes = dao.countMistakeWords(learner.userId(), lang);
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
        localizer().localize(new ArrayList<>(), banners, eventList, new ArrayList<>());
        return new Home(banners, event, contests.openCards(lang, now), games, practice);
    }

    public void recordPlay(String activityCode) {
        String id = learner.currentUserId();
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
        Map<String, Integer> learners = CountRows.toMap(dao.countLearnersPerActivity());
        List<ActivityCard> games = new ArrayList<>();
        for (ActivityEntity a : offered(ActivityEntity.GAME, learningLanguage())) {
            games.add(new ActivityCard(a, learners.getOrDefault(a.code, 0), -1));
        }
        return games;
    }
}
