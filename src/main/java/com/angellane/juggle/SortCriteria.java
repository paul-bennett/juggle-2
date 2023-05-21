package com.angellane.juggle;

import com.angellane.juggle.candidate.MemberCandidate;
import com.angellane.juggle.candidate.TypeCandidate;
import com.angellane.juggle.comparator.*;
import com.angellane.juggle.match.Match;
import com.angellane.juggle.query.MemberQuery;
import com.angellane.juggle.query.TypeQuery;

import java.util.Comparator;
import java.util.function.Function;

/**
 * The various different ways of sorting Members.
 * <p>
 * Enumerands are functions that return a Comparator<Member> suitably configured from the passed-in Main object.
 * (Main is useful because it ultimately contains the command-line args.)  Enumerand names are legal values for
 * Juggle's -s option.
 * <p>
 * To add a new sort criteria:
 *   1. Create a new class that implements java.util.Comparator<Member>
 *   2. Add a new enumerand to this enumeration:
 *       - name is the option to enable the criteria on the command-line
 *       - constructor arg is a Function<Main, Comparator<Member>>
 */

enum SortCriteria {
    SCORE   (j -> new ByScore<>(), j -> new ByScore<>()),
    ACCESS  (j -> new ByAccessibility<>(), j -> new ByAccessibility<>()),
    PACKAGE (j -> new ByPackage<>(j.getImportedPackageNames()),
             j -> new ByPackage<>(j.getImportedPackageNames())),
    TEXT    (j -> new ByString<>(), j -> new ByString<>()),
    NAME    (j -> new ByDeclarationName<>(), j -> new ByDeclarationName<>());

    private final Function<
            Juggler,
            Comparator<Match<TypeCandidate, TypeQuery>>
            > typeComparatorGenerator;
    private final Function<
            Juggler,
            Comparator<Match<MemberCandidate, MemberQuery>>
            > memberComparatorGenerator;

    SortCriteria(
            Function<Juggler, Comparator<Match<TypeCandidate,   TypeQuery>>>
                    typeComparatorGenerator,
            Function<Juggler, Comparator<Match<MemberCandidate, MemberQuery>>>
                    memberComparatorGenerator
    ) {
        this.typeComparatorGenerator   = typeComparatorGenerator;
        this.memberComparatorGenerator = memberComparatorGenerator;
    }

    Comparator<Match<TypeCandidate,TypeQuery>>
    getTypeComparator(Juggler j) {
        return typeComparatorGenerator.apply(j);
    }
    Comparator<Match<MemberCandidate,MemberQuery>>
    getMemberComparator(Juggler j) {
        return memberComparatorGenerator.apply(j);
    }
}
