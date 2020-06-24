package com.angellane.juggle;

import com.angellane.juggle.com.angellane.juggle.comparator.ByCanonicalName;
import com.angellane.juggle.com.angellane.juggle.comparator.ByPackage;

import java.lang.reflect.Member;
import java.util.Comparator;
import java.util.function.Function;

/**
 * The various different ways of sorting Members.
 *
 * Enumerands are functions that return a Comparator<Member> suitably configured from the passed-in Main object.
 * (Main is useful because it ultimately contains the command-line args.)  Enumerand names are legal values for
 * Juggle's -s option.
 *
 * To add a new sort criterium:
 *   1. Create a new class that implements java.util.Comparator<Member>
 *   2. Add a new enumerand to this enumeration:
 *       - name is the option to enable this criterium on the command-line
 *       - constructor arg is a Function<Main, Comparator<Member>>
 */
enum SortCriteria {
    PACKAGE (m -> new ByPackage(m.importPackageNames)),
    NAME    (m -> new ByCanonicalName());

    private Function<Main, Comparator<Member>> comparatorGenerator;

    SortCriteria(Function<Main, Comparator<Member>> comparatorGenerator) {
        this.comparatorGenerator = comparatorGenerator;
    }

    Comparator<Member> getComparator(Main m) {
        return comparatorGenerator.apply(m);
    }
}
