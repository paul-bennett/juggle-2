package com.angellane.juggle;

import java.io.IOException;
import java.lang.module.*;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Juggler {
    private static final String CLASS_SUFFIX = ".class";
    private static final String MODULE_INFO  = "module-info";
    private static final String BASE_MODULE  = "java.base";

    private final Configuration modConf;
    private final ResolvingURLClassLoader loader;
    private final Collection<Class<?>> classesToSearch;
    private final List<String> importedPackageNames;

    public Juggler(List<String> jars, List<String> mods, List<String> importedPackageNames) {
        this.importedPackageNames = importedPackageNames;

        URL[] urls = jars.stream()
                .flatMap(path -> {
                    try {
                        return Stream.of(Path.of(path).toUri().toURL());
                    } catch (MalformedURLException ex) {
                        return Stream.empty();
                    }
                })
                .toArray(URL[]::new);

        this.loader = new ResolvingURLClassLoader(urls);

        if (mods == null || mods.isEmpty())
            mods = List.of(BASE_MODULE);

        this.modConf = ModuleLayer.boot().configuration().resolve(
                ModuleFinder.ofSystem(),
                ModuleFinder.of(Path.of(".")),
                mods);

        classesToSearch = Stream.of( mods.stream().flatMap(this::moduleClassStream)
                                   , jars.stream().flatMap(this::jarClassStream)
                                   )
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }


    // Returns stream of class names within a JAR.  Note: these class names might not be valid Java identifiers,
    // especially in the case of inner classes or JAR files generated by something other than the Java compiler.
    public Stream<Class<?>> jarClassStream(String filename) {
        try (JarFile file = new JarFile(filename)) {
            return file.stream()
                    .filter(Predicate.not(JarEntry::isDirectory))
                    .map(JarEntry::getName)
                    .filter(s -> s.endsWith(CLASS_SUFFIX))
                    .map(s -> s.substring(0, s.length() - CLASS_SUFFIX.length()))
                    .map(s -> s.replace('/', '.'))
                    .map(this::loadClassByName)
                    .flatMap(Optional::stream);
        } catch (IOException e) {
            System.err.println("Couldn't read JAR file: " + filename + "; ignoring.");
            return Stream.empty();
        }
    }

    public Stream<Class<?>> moduleClassStream(String moduleName) {
        Optional<ResolvedModule> maybeMod = this.modConf.findModule(moduleName);

        if (maybeMod.isEmpty())
            System.err.println("Warning: couldn't find module " + moduleName);
        else {
            try (ModuleReader reader = maybeMod.get().reference().open()) {
                return reader.list()
                        .filter(s -> s.endsWith(CLASS_SUFFIX))
                        .map(s -> s.substring(0, s.length() - CLASS_SUFFIX.length()))
                        .filter(s -> !s.equals(MODULE_INFO))
                        .map(s -> s.replace('/', '.'))
                        .map(this::loadClassByName)
                        .flatMap(Optional::stream);
            }
            catch (IOException e) {
                System.err.println("Warning: error opening module " + moduleName);
            }
        }
        return Stream.empty();
    }

    private Optional<Class<?>> loadClassByName(String className) {
        try {
            Class<?> cls = loader.loadClass(className);
            loader.linkClass(cls);
            return Optional.of(cls);
        } catch (ClassNotFoundException ex) {
            return Optional.empty();
        } catch (NoClassDefFoundError e) {
            // This might be thrown if the class file references other classes that can't be loaded.
            // Maybe it depends on another JAR that hasn't been specified on the command-line with -j.
            System.err.println("*** Ignoring class " + className + ": " + e);
            return Optional.empty();
        }
    }

    public Class<?> classForTypename(String typename) {
        final String ARRAY_SUFFIX = "[]";

        // If this is an array, work out how many dimensions are involved, and strip []s from typename
        int arrayDimension;
        String baseTypename = typename;
        for (arrayDimension = 0; baseTypename.endsWith(ARRAY_SUFFIX); ++arrayDimension)
            baseTypename = baseTypename.substring(0, baseTypename.length() - ARRAY_SUFFIX.length()).stripTrailing();

        // TODO: think about Generics

        // Start with the base type
        Class<?> ret = Object.class;
        switch (typename) {
            case "void":        ret = Void.TYPE;        break;
            case "boolean":     ret = Boolean.TYPE;     break;
            case "char":        ret = Character.TYPE;   break;
            case "byte":        ret = Byte.TYPE;        break;
            case "short":       ret = Short.TYPE;       break;
            case "int":         ret = Integer.TYPE;     break;
            case "long":        ret = Long.TYPE;        break;
            case "float":       ret = Float.TYPE;       break;
            case "double":      ret = Double.TYPE;      break;
            default:
                // Actually now want to try typename plainly, then prefixed by each import in turn
                // Default to Object if we can't find any match
                String finalTypename = baseTypename;
                Optional<Class<?>> opt =
                        Stream.of(Stream.of(""), importedPackageNames.stream().map(pkg -> pkg + "."))
                                .flatMap(Function.identity())
                                .map(prefix -> prefix + finalTypename)
                                .map(this::loadClassByName)
                                .flatMap(Optional::stream)
                                .findFirst();

                if (opt.isPresent())
                    ret = opt.get();
                else
                    // If we get here, the class wasn't found, either naked or with any imported package prefix
                    System.err.println("Warning: couldn't find class: " + baseTypename + "; using " + ret + " instead");
        }

        // Now add the array dimension
        for ( ; arrayDimension > 0; --arrayDimension)
            ret = ret.arrayType();

        return ret;
    }

    public Stream<CandidateMember> allCandidates() {
        return classesToSearch.stream()
                .flatMap(c -> Stream.of(
                                  Arrays.stream(c.getDeclaredFields())
                                        .map(CandidateMember::membersFromField)
                                        .flatMap(List::stream)
                                , Arrays.stream(c.getDeclaredConstructors())
                                        .map(CandidateMember::memberFromConstructor)
                                , Arrays.stream(c.getDeclaredMethods())
                                        .map(CandidateMember::memberFromMethod)
                                )
                        .flatMap(Function.identity())
                );
    }

    public Member[] findMembers(Accessibility minAccess, TypeSignature query) {
        return allCandidates()
                .filter(m -> !m.getMember().getDeclaringClass().isAnonymousClass())     // anon and local classes ...
                .filter(m -> !m.getMember().getDeclaringClass().isLocalClass())         // ... are unutterable anyway
                .filter(m -> query.paramTypes == null || m.matchesParams(query.paramTypes, true))
                .filter(m -> query.returnType == null || m.matchesReturn(query.returnType))
                .filter(m -> query.throwTypes == null || m.matchesThrows(query.throwTypes))
                .map(CandidateMember::getMember)
                .filter(m -> Accessibility.fromModifiers(m.getModifiers()).isAtLastAsAccessibleAsOther(minAccess))
                .distinct()
                .toArray(Member[]::new);
    }
}
