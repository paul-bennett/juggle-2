package com.angellane.juggle.comparator;

import com.angellane.juggle.candidate.Candidate;
import com.angellane.juggle.match.Match;
import com.angellane.juggle.query.Query;

import java.util.Comparator;

/**
 * Compares two Matches based on the score that was computed when their
 * Candidate was evaluated against the Query.  Lower scores (better fits)
 * are ordered first.
 */
public class ByScore<
        C extends Candidate, Q extends Query<C>, M extends Match<C,Q>
        >
        implements Comparator<M> {
    @Override
    public int compare(M m1, M m2) {
        return Integer.compare(m1.score(), m2.score());
    }
}
