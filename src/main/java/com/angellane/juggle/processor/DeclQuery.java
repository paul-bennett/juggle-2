package com.angellane.juggle.processor;

import com.angellane.juggle.Accessibility;
import com.angellane.juggle.CandidateMember;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a declaration query -- the result of parsing a pseudo-Java declaration that's
 * subsequently used as a template against which to match.
 */
public class DeclQuery {
    public record BoundedType(
            Set<Class<?>> upperBound,   // extends/implements
            Class<?> lowerBound         // super
    ) {
        public static BoundedType exactType(Class<?> c) {
            return new BoundedType(Set.of(c), c);
        }
        public static BoundedType subtypeOf(Class<?> c) {
            return new BoundedType(Set.of(c), null);
        }

        public static BoundedType supertypeOf(Class<?> c) {
            return new BoundedType(null, c);
        }

        public boolean matchesClass(Class<?> candidate) {
            // TODO: consider what to do about conversions here (esp boxing/unboxing)
            return (lowerBound == null || lowerBound.isAssignableFrom(candidate)) &&
                    (upperBound == null || upperBound.stream().allMatch(candidate::isAssignableFrom));
        }
    }

    public sealed interface ParamSpec permits Ellipsis, SingleParam {
        static ParamSpec ellipsis() { return new Ellipsis(); }
        static ParamSpec param(String name, Class<?> type) {
            return new SingleParam(Pattern.compile("^" + name + "$"),
                    new BoundedType(Set.of(type), type));
        }

    }
    public record Ellipsis() implements ParamSpec {}
    public record SingleParam(
            Pattern paramName,
            BoundedType paramType
    ) implements ParamSpec {}

    public Set<Class<?>> annotationTypes = null;    // TODO: consider Set<Class<? extends Annotation>>
    public Accessibility accessibility = null;
    public int modifierMask = 0, modifiers = 0;
    public BoundedType returnType = null;

    public Pattern declarationName = null;

    public List<ParamSpec> params = null;
    public Set<BoundedType> exceptions = null;

    public boolean isMatchForCandidate(CandidateMember cm) {
        return cm.matchesAccessibility(accessibility)
                && cm.matchesModifiers(modifierMask, modifiers)
                && matchesAnnotations(cm)
                && matchesReturn(cm)
                && matchesName(cm)
                && matchesParams(cm)
                && matchesExceptions(cm)
        ;
    }

    boolean matchesAnnotations(CandidateMember cm) {
        return this.annotationTypes == null
                || cm.annotationTypes().containsAll(this.annotationTypes);
    }

    boolean matchesReturn(CandidateMember cm) {
        return this.returnType == null
                || this.returnType.matchesClass(cm.returnType());
    }

    boolean matchesName(CandidateMember cm) {
        return this.declarationName == null
                || this.declarationName.matcher(cm.member().getName()).matches();
    }

    boolean matchesParams(CandidateMember cm) {
        return this.params == null
                || true;            // TODO: implement
    }

    boolean matchesExceptions(CandidateMember cm) {
        return this.exceptions == null
                || this.exceptions.stream().allMatch(ex -> cm.throwTypes().stream().anyMatch(ex::matchesClass));
    }

    DeclQuery() {}

    public DeclQuery(final String declString) {
        if (!declString.isEmpty())
            System.err.println("QUERY STRING: `" + declString + "'");

        // TODO: parse declString and fill out fields of this
    }
}
