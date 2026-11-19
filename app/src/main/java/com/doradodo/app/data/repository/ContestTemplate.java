package com.doradodo.app.data.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * A contest that runs every week at the same moments of the week (UTC). The four moments are minutes counted from
 * Monday 00:00 UTC; {@link ContestSchedule} turns them into dated contests.
 */
public final class ContestTemplate {

    public final String id;
    public final int registrationOpensMinute;
    public final int registrationClosesMinute;
    public final int startsMinute;
    public final int endsMinute;

    public String title = "";
    public String description;
    public String image;
    public String accent = "AMBER";
    public int questionCount;
    public int durationMinutes;
    /** Packs the questions come from: the contest is for the languages of these packs. */
    public List<String> packs = new ArrayList<>();
    /** Prizes as {rankFrom, rankTo (0 = open ended), gems}. */
    public List<int[]> prizes = new ArrayList<>();

    public ContestTemplate(String id, int registrationOpensMinute, int registrationClosesMinute, int startsMinute,
                           int endsMinute) {
        this.id = id;
        this.registrationOpensMinute = registrationOpensMinute;
        this.registrationClosesMinute = registrationClosesMinute;
        this.startsMinute = startsMinute;
        this.endsMinute = endsMinute;
    }
}
