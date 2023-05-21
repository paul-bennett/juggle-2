package com.angellane.juggle.comparator;

import com.angellane.juggle.candidate.Candidate;
import com.angellane.juggle.match.Match;
import com.angellane.juggle.query.Query;

import java.util.Comparator;

/**
 * Compares two Matches based on their Candidates' canonical name.
 */
public class ByDeclarationName<
        C extends Candidate, Q extends Query<C>, M extends Match<C,Q>
        >
        implements Comparator<M> {
    @Override
    public int compare(M m1, M m2) {
        return m1.candidate().declarationName()
                .compareTo(m2.candidate().declarationName());
    }
}
