package com.doradodo.app.data.local.entity;

/** One member of a synonym group of one language, with whether the learner already studied the linked vocabulary word. */
public class SynonymRow {
    public long groupId;
    public String text;
    public boolean learned;
}
