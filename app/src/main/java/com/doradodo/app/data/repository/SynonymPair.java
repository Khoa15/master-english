package com.doradodo.app.data.repository;

/** Two words of a synonym group to be matched against each other. */
public final class SynonymPair {
    public final String left;
    public final String right;

    SynonymPair(String left, String right) {
        this.left = left;
        this.right = right;
    }
}
