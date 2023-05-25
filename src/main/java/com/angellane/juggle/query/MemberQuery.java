package com.angellane.juggle.query;

import com.angellane.juggle.candidate.MemberCandidate;
import com.angellane.juggle.candidate.Param;
import com.angellane.juggle.match.Match;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.angellane.juggle.util.Decomposer.decomposeIntoParts;

/**
 * This class represents a declaration query -- the result of parsing a
 * pseudo-Java declaration that's subsequently used as a template against which
 * to match.
 */
public final class MemberQuery extends Query<MemberCandidate> {
    public BoundedType      returnType  = null;

    public List<ParamSpec>  params      = null;
    public Set<BoundedType> exceptions  = null;

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        else if (!(other instanceof MemberQuery q))
            return false;
        else
            return super.equals(q)
                    && Objects.equals(returnType,   q.returnType)
                    && Objects.equals(params,       q.params)
                    && Objects.equals(exceptions,   q.exceptions)
                    ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode()
                , returnType
                , params
                , exceptions
        );
    }

    @Override
    public String toString() {
        return "DeclQuery{" +
                "annotationTypes=" + annotationTypes +
                ", accessibility=" + accessibility +
                ", modifierMask=" + modifierMask +
                ", modifiers=" + modifiers +
                ", returnType=" + returnType +
                ", declarationPattern=" + declarationPattern +
                ", params=" + params +
                ", exceptions=" + exceptions +
                '}';
    }

    @Override
    public
    <Q extends Query<MemberCandidate>, M extends Match<MemberCandidate,Q>>
    Stream<M> match(MemberCandidate candidate) {
        OptionalInt  optScore = scoreCandidate(candidate);

        if (optScore.isPresent()) {
            @SuppressWarnings("unchecked")
            M m = (M)new Match<>(candidate, this, optScore.getAsInt());
            return Stream.of(m);
        }
        else
            return Stream.empty();
    }

    OptionalInt  scoreCandidate(MemberCandidate cm) {
        return totalScore(List.of(
                scoreAnnotations(cm.annotationTypes())
                , scoreAccessibility(cm.accessibility())
                , scoreModifiers(cm.otherModifiers())
                , scoreReturn(cm.returnType())
                , scoreName(cm.declarationName())
                , scoreParams(cm.params())
                , scoreExceptions(cm.throwTypes())
                ));
    }

    OptionalInt scoreReturn(Class<?> returnType) {
        return this.returnType == null
                || this.returnType.matchesClass(returnType)
                ? OptionalInt.of(0) : OptionalInt.empty();
    }

    OptionalInt scoreExceptions(Set<Class<?>> candidateExceptions) {
        // Need to check both ways:
        //  1. Is everything thrown by the query also thrown by the candidate?
        //  2. Is everything thrown by the candidate also thrown by the query?
        return this.exceptions == null
                || this.exceptions.stream()
                .allMatch(ex -> candidateExceptions.stream()
                        .anyMatch(ex::matchesClass)
                )
                && candidateExceptions.stream()
                .allMatch(ex1 -> this.exceptions.stream()
                        .anyMatch(ex2 -> ex2.matchesClass(ex1))
                )
                ? OptionalInt.of(0) : OptionalInt.empty();
    }

    OptionalInt scoreParams(List<Param> candidateParams) {
        if (params == null)
            return OptionalInt.of(0);

        // params :: [ParamSpec]
        // type ParamSpec = ZeroOrMoreParams | SingleParam name type

        // Intent is to construct a number of alternative queries of type
        // List<SingleParam> by replacing the ZeroOrMoreParams objects with
        // a number of wildcard SingleParam objects.

        int numParamSpecs = (int)params.stream()
                .filter(p -> p instanceof SingleParam).count();
        int numEllipses = params.size() - numParamSpecs;
        int spareParams = candidateParams.size() - numParamSpecs;

        if (spareParams == 0)
            // No ellipses, correct #params
            return scoreParamSpecs(
                    params.stream()
                            .filter(ps -> ps instanceof SingleParam)
                            .map(ps -> (SingleParam)ps)
                            .toList(),
                    candidateParams
            );
        else if (numEllipses == 0)
            // No ellipses over which to distribute spare params
            return OptionalInt.empty();
        else if (spareParams < 0)
            // More specified params than candidate params
            return OptionalInt.empty();
        else {
            // Nasty: using a 1-element array so we can set inside lambda
            final OptionalInt [] ret = new OptionalInt[]{ OptionalInt.empty() };

            decomposeIntoParts(spareParams, numEllipses, distribution -> {
                int ix = 0;  // index into distribution
                List<SingleParam> queryParams = new ArrayList<>();

                for (ParamSpec ps : params) {
                    if (ps instanceof SingleParam singleParam)
                        queryParams.add(singleParam);
                    else
                        for (int numWildcards = distribution[ix++];
                             numWildcards > 0; numWildcards--)
                            queryParams.add(ParamSpec.wildcard());
                }

                OptionalInt  thisScore =
                        scoreParamSpecs(queryParams, candidateParams);

                ret[0] = IntStream.concat(ret[0].stream(), thisScore.stream())
                        .max();
            });

            return ret[0];
        }
    }

    private OptionalInt scoreParamSpecs(
            List<SingleParam> queryParams,
            List<Param>       candidateParams
    ) {
        if (queryParams.size() != candidateParams.size())
            return OptionalInt.empty();
        else {
            Iterator<? extends Param> actualParamIter =
                    candidateParams.iterator();

            return totalScore(
                    queryParams.stream()
                            .map(ps -> {
                                Pattern namePat = ps.paramName();
                                BoundedType bounds = ps.paramType();

                                Param actualParam = actualParamIter.next();
                                Class<?> actualType = actualParam.type();
                                String actualName = actualParam.name();

                                return namePat.matcher(actualName).find()
                                        ? bounds.scoreMatch(actualType)
                                        : OptionalInt.empty();
                            })
                            .toList()
            );
        }
    }
}

