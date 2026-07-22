/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Signature;
import ch.njol.skript.util.Utils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.lang.invoke.TypeDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.util.Registry;

@ApiStatus.Internal
public final class FunctionRegistry
implements Registry<Function<?>> {
    private static FunctionRegistry registry;
    static final Pattern FUNCTION_NAME_PATTERN;
    private final NamespaceIdentifier GLOBAL_NAMESPACE = new NamespaceIdentifier(null);
    private final Map<NamespaceIdentifier, Namespace> namespaces = new ConcurrentHashMap<NamespaceIdentifier, Namespace>();

    public static FunctionRegistry getRegistry() {
        if (registry == null) {
            registry = new FunctionRegistry();
        }
        return registry;
    }

    @Override
    public @Unmodifiable @NotNull Collection<Function<?>> elements() {
        HashSet functions = new HashSet();
        for (Namespace namespace : this.namespaces.values()) {
            functions.addAll(namespace.functions.values());
        }
        return Collections.unmodifiableSet(functions);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void register(@Nullable String namespace, @NotNull Signature<?> signature) {
        Preconditions.checkNotNull(signature, (Object)"signature cannot be null");
        if (signature.isLocal() && namespace == null) {
            throw new IllegalArgumentException("Cannot register a local signature in the global namespace");
        }
        if (!signature.isLocal() && namespace != null) {
            throw new IllegalArgumentException("Cannot register a global signature in a local namespace");
        }
        Skript.debug("Registering signature '%s'", signature.getName());
        NamespaceIdentifier namespaceId = namespace != null ? new NamespaceIdentifier(namespace) : this.GLOBAL_NAMESPACE;
        Namespace ns = this.namespaces.computeIfAbsent(namespaceId, n -> new Namespace());
        FunctionIdentifier identifier = FunctionIdentifier.of(signature);
        Namespace namespace2 = ns;
        synchronized (namespace2) {
            Set identifiersWithName = ns.identifiers.computeIfAbsent(identifier.name, s -> new HashSet());
            boolean exists = identifiersWithName.add(identifier);
            if (!exists) {
                FunctionRegistry.alreadyRegisteredError(signature.getName(), identifier, namespaceId);
            }
        }
        Signature<?> existing = ns.signatures.putIfAbsent(identifier, signature);
        if (existing != null) {
            FunctionRegistry.alreadyRegisteredError(signature.getName(), identifier, namespaceId);
        }
    }

    public void register(@Nullable String namespace, @NotNull Function<?> function) {
        FunctionIdentifier identifier;
        Preconditions.checkNotNull(function, (Object)"function cannot be null");
        if (function.getSignature().isLocal() && namespace == null) {
            throw new IllegalArgumentException("Cannot register a local function in the global namespace");
        }
        if (!function.getSignature().isLocal() && namespace != null) {
            throw new IllegalArgumentException("Cannot register a global function in a local namespace");
        }
        Skript.debug("Registering function '%s'", function.getName());
        String name = function.getName();
        if (!FUNCTION_NAME_PATTERN.matcher(name).matches()) {
            throw new SkriptAPIException("Invalid function name '" + name + "'");
        }
        NamespaceIdentifier namespaceId = namespace != null ? new NamespaceIdentifier(namespace) : this.GLOBAL_NAMESPACE;
        if (!this.signatureExists(namespaceId, identifier = FunctionIdentifier.of(function.getSignature()))) {
            this.register(namespace, function.getSignature());
        }
        Namespace ns = this.namespaces.computeIfAbsent(namespaceId, n -> new Namespace());
        Function<?> existing = ns.functions.putIfAbsent(identifier, function);
        if (existing != null) {
            FunctionRegistry.alreadyRegisteredError(name, identifier, namespaceId);
        }
    }

    private static void alreadyRegisteredError(String name, FunctionIdentifier identifier, NamespaceIdentifier namespace) {
        throw new SkriptAPIException("Function '%s' with parameters %s is already registered in %s".formatted(name, Arrays.toString(Arrays.stream(identifier.args).map(Class::getSimpleName).toArray()), namespace));
    }

    private boolean signatureExists(@NotNull NamespaceIdentifier namespace, @NotNull FunctionIdentifier identifier) {
        Preconditions.checkNotNull((Object)namespace, (Object)"namespace cannot be null");
        Preconditions.checkNotNull((Object)identifier, (Object)"identifier cannot be null");
        Namespace ns = this.namespaces.get(namespace);
        if (ns == null) {
            return false;
        }
        if (!ns.identifiers.containsKey(identifier.name)) {
            return false;
        }
        for (FunctionIdentifier other : ns.identifiers.get(identifier.name)) {
            if (!identifier.equals(other)) continue;
            return true;
        }
        return false;
    }

    @NotNull
    public Retrieval<Function<?>> getFunction(@Nullable String namespace, @NotNull String name, Class<?> ... args) {
        Retrieval<Function<?>> attempt = null;
        if (namespace != null) {
            attempt = this.getFunction(new NamespaceIdentifier(namespace), FunctionIdentifier.of(name, true, args));
        }
        if (attempt == null || attempt.result() == RetrievalResult.NOT_REGISTERED) {
            attempt = this.getFunction(this.GLOBAL_NAMESPACE, FunctionIdentifier.of(name, false, args));
        }
        return attempt;
    }

    @NotNull
    private Retrieval<Function<?>> getFunction(@NotNull NamespaceIdentifier namespace, @NotNull FunctionIdentifier provided) {
        Preconditions.checkNotNull((Object)namespace, (Object)"namespace cannot be null");
        Preconditions.checkNotNull((Object)provided, (Object)"provided cannot be null");
        Namespace ns = this.namespaces.getOrDefault(namespace, new Namespace());
        Set<FunctionIdentifier> existing = ns.identifiers.get(provided.name);
        if (existing == null) {
            Skript.debug("No functions named '%s' exist in the '%s' namespace", provided.name, namespace.name);
            return new Retrieval<Object>(RetrievalResult.NOT_REGISTERED, null, null);
        }
        Set<FunctionIdentifier> candidates = FunctionRegistry.candidates(provided, existing, false);
        if (candidates.isEmpty()) {
            Skript.debug("Failed to find a function for '%s'", provided.name);
            return new Retrieval<Object>(RetrievalResult.NOT_REGISTERED, null, null);
        }
        if (candidates.size() == 1) {
            if (Skript.debug()) {
                Skript.debug("Matched function for '%s': %s", provided.name, candidates.stream().findAny().orElse(null));
            }
            return new Retrieval(RetrievalResult.EXACT, ns.functions.get(candidates.stream().findAny().orElse(null)), null);
        }
        if (Skript.debug()) {
            String options = candidates.stream().map(Record::toString).collect(Collectors.joining(", "));
            Skript.debug("Failed to match an exact function for '%s'", provided.name);
            Skript.debug("Identifier: %s", provided);
            Skript.debug("Options: %s", options);
        }
        return new Retrieval<Object>(RetrievalResult.AMBIGUOUS, null, (Class[][])candidates.stream().map(FunctionIdentifier::args).toArray(x$0 -> new Class[x$0][]));
    }

    public Retrieval<Signature<?>> getSignature(@Nullable String namespace, @NotNull String name, Class<?> ... args) {
        Retrieval<Signature<?>> attempt = null;
        if (namespace != null) {
            attempt = this.getSignature(new NamespaceIdentifier(namespace), FunctionIdentifier.of(name, true, args), false);
        }
        if (attempt == null || attempt.result() == RetrievalResult.NOT_REGISTERED) {
            attempt = this.getSignature(this.GLOBAL_NAMESPACE, FunctionIdentifier.of(name, false, args), false);
        }
        return attempt;
    }

    Retrieval<Signature<?>> getExactSignature(@Nullable String namespace, @NotNull String name, Class<?> ... args) {
        Retrieval<Signature<?>> attempt = null;
        if (namespace != null) {
            attempt = this.getSignature(new NamespaceIdentifier(namespace), FunctionIdentifier.of(name, true, args), true);
        }
        if (attempt == null || attempt.result() == RetrievalResult.NOT_REGISTERED) {
            attempt = this.getSignature(this.GLOBAL_NAMESPACE, FunctionIdentifier.of(name, false, args), true);
        }
        return attempt;
    }

    public @Unmodifiable @NotNull Set<Signature<?>> getSignatures(@Nullable String namespace, @NotNull String name) {
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        HashMap total = new HashMap();
        if (namespace != null) {
            Namespace local = this.namespaces.getOrDefault(new NamespaceIdentifier(namespace), new Namespace());
            for (FunctionIdentifier identifier : local.identifiers.getOrDefault(name, Collections.emptySet())) {
                total.putIfAbsent(identifier, local.signatures.get(identifier));
            }
        }
        Namespace global = this.namespaces.getOrDefault(this.GLOBAL_NAMESPACE, new Namespace());
        for (FunctionIdentifier identifier : global.identifiers.getOrDefault(name, Collections.emptySet())) {
            total.putIfAbsent(identifier, global.signatures.get(identifier));
        }
        return Set.copyOf(total.values());
    }

    private Retrieval<Signature<?>> getSignature(@NotNull NamespaceIdentifier namespace, @NotNull FunctionIdentifier provided, boolean exact) {
        Preconditions.checkNotNull((Object)namespace, (Object)"namespace cannot be null");
        Preconditions.checkNotNull((Object)provided, (Object)"provided cannot be null");
        Namespace ns = this.namespaces.getOrDefault(namespace, new Namespace());
        if (!ns.identifiers.containsKey(provided.name)) {
            Skript.debug("No signatures named '%s' exist in the '%s' namespace", provided.name, namespace.name);
            return new Retrieval<Object>(RetrievalResult.NOT_REGISTERED, null, null);
        }
        Set<FunctionIdentifier> candidates = FunctionRegistry.candidates(provided, ns.identifiers.get(provided.name), exact);
        if (candidates.isEmpty()) {
            Skript.debug("Failed to find a signature for '%s'", provided.name);
            return new Retrieval<Object>(RetrievalResult.NOT_REGISTERED, null, null);
        }
        if (candidates.size() == 1) {
            if (Skript.debug()) {
                Skript.debug("Matched signature for '%s': %s", provided.name, ns.signatures.get(candidates.stream().findAny().orElse(null)));
            }
            return new Retrieval(RetrievalResult.EXACT, ns.signatures.get(candidates.stream().findAny().orElse(null)), null);
        }
        if (Skript.debug()) {
            String options = candidates.stream().map(Record::toString).collect(Collectors.joining(", "));
            Skript.debug("Failed to match an exact signature for '%s'", provided.name);
            Skript.debug("Identifier: %s", provided);
            Skript.debug("Options: %s", options);
        }
        return new Retrieval<Object>(RetrievalResult.AMBIGUOUS, null, (Class[][])candidates.stream().map(FunctionIdentifier::args).toArray(x$0 -> new Class[x$0][]));
    }

    private static @Unmodifiable @NotNull Set<FunctionIdentifier> candidates(@NotNull FunctionIdentifier provided, Set<FunctionIdentifier> existing, boolean exact) {
        HashSet<FunctionIdentifier> candidates = new HashSet<FunctionIdentifier>();
        block0: for (FunctionIdentifier candidate : existing) {
            if (Arrays.stream(candidate.args).filter(Class::isArray).count() == 1L && candidate.args.length == 1 && candidate.args[0].isArray()) {
                TypeDescriptor.OfField arrayType = candidate.args[0].componentType();
                for (Class<?> arrayArg : provided.args) {
                    if (!Converters.converterExists(arrayArg = Utils.getComponentType(arrayArg), arrayType)) continue block0;
                }
                candidates.add(candidate);
                continue;
            }
            if (provided.args.length > candidate.args.length || provided.args.length < candidate.minArgCount) continue;
            for (int i = 0; i < provided.args.length; ++i) {
                Class<?> providedType = Utils.getComponentType(provided.args[i]);
                Class<?> candidateType = Utils.getComponentType(candidate.args[i]);
                Class<?> providedArg = provided.args[i];
                if (exact ? providedArg != candidateType : !Converters.converterExists(providedType, candidateType)) continue block0;
            }
            candidates.add(candidate);
        }
        if (candidates.size() <= 1) {
            return Collections.unmodifiableSet(candidates);
        }
        Iterator iterator = candidates.iterator();
        block3: while (iterator.hasNext()) {
            FunctionIdentifier candidate;
            candidate = (FunctionIdentifier)iterator.next();
            int argIndex = 0;
            while (argIndex < provided.args.length) {
                if (provided.args[argIndex] == Object.class) {
                    ++argIndex;
                    continue;
                }
                if (provided.args[argIndex] != candidate.args[argIndex]) {
                    iterator.remove();
                    continue block3;
                }
                ++argIndex;
            }
        }
        return Collections.unmodifiableSet(candidates);
    }

    public void remove(@NotNull Signature<?> signature) {
        Preconditions.checkNotNull(signature, (Object)"signature cannot be null");
        String name = signature.getName();
        FunctionIdentifier identifier = FunctionIdentifier.of(signature);
        Namespace namespace = signature.isLocal() ? this.namespaces.get(new NamespaceIdentifier(signature.namespace())) : this.namespaces.get(this.GLOBAL_NAMESPACE);
        if (namespace == null) {
            return;
        }
        for (FunctionIdentifier other : namespace.identifiers.getOrDefault(name, Set.of())) {
            if (!identifier.equals(other)) continue;
            this.removeUpdateMaps(namespace, other, name);
            return;
        }
    }

    private void removeUpdateMaps(Namespace namespace, FunctionIdentifier toRemove, String name) {
        namespace.identifiers.computeIfPresent(name, (k, set) -> {
            if (set.remove(toRemove)) {
                Skript.debug("Removed identifier '%s' from %s", toRemove, namespace);
            }
            return set.isEmpty() ? null : set;
        });
        if (namespace.functions.remove(toRemove) != null) {
            Skript.debug("Removed function '%s' from %s", toRemove, namespace);
        }
        if (namespace.signatures.remove(toRemove) != null) {
            Skript.debug("Removed signature '%s' from %s", toRemove, namespace);
        }
    }

    static {
        FUNCTION_NAME_PATTERN = Pattern.compile("[\\p{IsAlphabetic}_][\\p{IsAlphabetic}\\d_]*");
    }

    private record NamespaceIdentifier(@Nullable String name) {
        public boolean local() {
            return this.name == null;
        }
    }

    private static final class Namespace {
        private final Map<String, Set<FunctionIdentifier>> identifiers = new HashMap<String, Set<FunctionIdentifier>>();
        private final Map<FunctionIdentifier, Function<?>> functions = new HashMap();
        private final Map<FunctionIdentifier, Signature<?>> signatures = new HashMap();

        private Namespace() {
        }
    }

    record FunctionIdentifier(@NotNull String name, boolean local, int minArgCount, @NotNull Class<?>[] args) {
        static FunctionIdentifier of(@NotNull String name, boolean local, Class<?> ... args) {
            Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
            Preconditions.checkNotNull(args, (Object)"args cannot be null");
            return new FunctionIdentifier(name, local, args.length, args);
        }

        static FunctionIdentifier of(@NotNull Signature<?> signature) {
            Preconditions.checkNotNull(signature, (Object)"signature cannot be null");
            Parameter<?>[] signatureParams = signature.parameters().all();
            Class[] parameters = new Class[signatureParams.length];
            int optionalArgs = 0;
            for (int i = 0; i < signatureParams.length; ++i) {
                Parameter<?> param = signatureParams[i];
                if (param.hasModifier(Parameter.Modifier.OPTIONAL)) {
                    ++optionalArgs;
                }
                parameters[i] = param.type();
            }
            return new FunctionIdentifier(signature.getName(), signature.isLocal(), parameters.length - optionalArgs, parameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, Arrays.hashCode(this.args));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FunctionIdentifier)) {
                return false;
            }
            FunctionIdentifier other = (FunctionIdentifier)obj;
            if (!this.name.equals(other.name)) {
                return false;
            }
            if (this.args.length != other.args.length) {
                return false;
            }
            for (int i = 0; i < this.args.length; ++i) {
                if (this.args[i] == other.args[i]) continue;
                return false;
            }
            return true;
        }

        @Override
        @NotNull
        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("name", (Object)this.name).add("local", this.local).add("minArgCount", this.minArgCount).add("args", (Object)Arrays.stream(this.args).map(Class::getSimpleName).collect(Collectors.joining(", "))).toString();
        }
    }

    public record Retrieval<T>(@NotNull RetrievalResult result, T retrieved, Class<?>[][] conflictingArgs) {
    }

    public static enum RetrievalResult {
        NOT_REGISTERED,
        AMBIGUOUS,
        EXACT;

    }
}

