package com.angellane.juggle.query;

import com.angellane.juggle.match.Accessibility;
import com.angellane.juggle.candidate.MemberCandidate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class runs tests against the method
 *    `protected java.net.URL ClassLoader.findResource(String name)`
 * using various forms of DeclQuery
 */
public class TestMemberDecl {
    static MemberCandidate cm;

    @BeforeAll
    public static void createCandidateMember()
            throws NoSuchMethodException {
        Class<?> c = ClassLoader.class;
        Method m = c.getDeclaredMethod("findResource", String.class);
        cm = MemberCandidate.memberFromMethod(m);
    }

    private static void matchQueryAndCandidate(MemberQuery q, MemberCandidate cm) {
        assertTrue(q.matchesAnnotations(cm.annotationTypes())
                , "Match annotations");
        assertTrue(q.matchesModifiers(cm.otherModifiers())
                , "Match modifiers");
        assertTrue(q.matchesAccessibility(cm.accessibility())
                , "Match accessibility");
        assertEquals(OptionalInt.of(0), q.scoreReturn(cm.returnType())      , "Match return type");
        assertTrue(q.matchesName(cm.declarationName())                      , "Match method name");
        assertEquals(OptionalInt.of(0), q.scoreParams(cm.params())          , "Match parameters");
        assertEquals(OptionalInt.of(0), q.scoreExceptions(cm.throwTypes())  , "Match exceptions");

        assertTrue(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testAllAttributes() {
        MemberQuery q = new MemberQuery();

        q.annotationTypes = Set.of();
        q.modifierMask = Modifier.STATIC;
        q.accessibility = Accessibility.PROTECTED;
        q.returnType = BoundedType.exactType(java.net.URL.class);
        q.declarationPattern = Pattern.compile("^findResource$");
        q.params = List.of(
                ParamSpec.param("this", ClassLoader.class),
                ParamSpec.param("arg0", String.class));
        q.exceptions = Set.of();

        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testEllipsis() {
        MemberQuery q = new MemberQuery();

        q.accessibility = Accessibility.PROTECTED;
        q.declarationPattern = Pattern.compile("^findResource$");
        q.params = List.of(ParamSpec.ellipsis());

        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testTooManyArgs() {
        MemberQuery q = new MemberQuery();

        q.declarationPattern = Pattern.compile("^findResource$");
        q.params = List.of(
                ParamSpec.param(null, ClassLoader.class),
                ParamSpec.param("name", String.class),
                ParamSpec.param("name", String.class),
                ParamSpec.ellipsis());

        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testTooFewArgs() {
        MemberQuery q = new MemberQuery();

        q.declarationPattern = Pattern.compile("^findResource$");
        q.params = List.of(ParamSpec.param(null, ClassLoader.class));

        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testNoAttributes() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PROTECTED;
        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testCorrectAnnotations() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PROTECTED;
        q.annotationTypes = Set.of();
        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testCorrectModifiers() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PROTECTED;
        q.modifierMask = Modifier.STATIC;
        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testCorrectAccessibility() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PROTECTED;
        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testCorrectReturnType() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PROTECTED;
        q.returnType = BoundedType.exactType(java.net.URL.class);
        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testCorrectDeclarationName() {
        MemberQuery q = new MemberQuery();
        q.declarationPattern = Pattern.compile("^findResource$");
        q.accessibility = Accessibility.PROTECTED;
        matchQueryAndCandidate(q, cm);
    }

    @Test
    @Disabled("Param name matching not implemented yet")
    public void testCorrectNamedParams() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PROTECTED;
        q.params = List.of(ParamSpec.param("name", String.class));
        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testCorrectExceptions() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PROTECTED;
        q.exceptions = Set.of();
        matchQueryAndCandidate(q, cm);
    }

    @Test
    public void testWrongAnnotations() {
        MemberQuery q = new MemberQuery();
        q.annotationTypes = Set.of(Override.class);
        assertFalse(q.matchesAnnotations(cm.annotationTypes()),
                "Match annotations");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongModifiers() {
        MemberQuery q = new MemberQuery();

        q.modifierMask = Modifier.STATIC;
        q.modifiers = Modifier.STATIC;

        assertFalse(q.matchesModifiers(cm.otherModifiers()),
                "Match modifiers");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongAccessibility() {
        MemberQuery q = new MemberQuery();
        q.accessibility = Accessibility.PUBLIC;
        assertFalse(q.matchesAccessibility(cm.accessibility()),
                "Match accessibility");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongReturnType() {
        MemberQuery q = new MemberQuery();
        q.returnType = BoundedType.exactType(Integer.TYPE);
        assertEquals(OptionalInt.empty(), q.scoreReturn(cm.returnType()), "Match return");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongDeclarationName() {
        MemberQuery q = new MemberQuery();
        q.declarationPattern = Pattern.compile("^barf$");
        assertFalse(q.matchesName(cm.declarationName()), "Match name");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongParamName() {
        MemberQuery q = new MemberQuery();
        q.params = List.of(ParamSpec.param("foo", String.class));
        assertEquals(OptionalInt.empty(), q.scoreParams(cm.params()), "Match params");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongParamType() {
        MemberQuery q = new MemberQuery();
        q.params = List.of(ParamSpec.param("name", Integer.class));
        assertEquals(OptionalInt.empty(), q.scoreParams(cm.params()), "Match params");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongParamArity() {
        MemberQuery q = new MemberQuery();
        q.params = List.of(
                ParamSpec.param("name", String.class),
                ParamSpec.param("name", String.class)
        );
        assertEquals(OptionalInt.empty(), q.scoreParams(cm.params()), "Match params");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }

    @Test
    public void testWrongExceptions() {
        MemberQuery q = new MemberQuery();
        q.exceptions =
                Set.of(BoundedType.exactType(NoSuchMethodException.class));
        assertEquals(OptionalInt.empty(), q.scoreExceptions(cm.throwTypes()), "Match exceptions");
        assertFalse(q.scoreCandidate(cm).isPresent(),
                "Match entire declaration");
    }
}
