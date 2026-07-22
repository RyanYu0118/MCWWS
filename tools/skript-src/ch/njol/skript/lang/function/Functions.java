/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionRegistry;
import ch.njol.skript.lang.function.JavaFunction;
import ch.njol.skript.lang.function.Namespace;
import ch.njol.skript.lang.function.ScriptFunction;
import ch.njol.skript.lang.function.Signature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.DefaultFunction;
import org.skriptlang.skript.common.function.FunctionParser;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.lang.script.Script;

public abstract class Functions {
    private static final String INVALID_FUNCTION_DEFINITION = "Invalid function definition. Please check for typos and make sure that the function's name only contains letters and underscores. Refer to the documentation for more information.";
    @Nullable
    public static ScriptFunction<?> currentFunction = null;
    private static final Map<Namespace.Key, Namespace> namespaces = new HashMap<Namespace.Key, Namespace>();
    private static final Namespace javaNamespace = new Namespace();
    private static final Map<String, Namespace> globalFunctions;
    public static boolean callFunctionEvents;
    public static final String functionNamePattern = "[\\p{IsAlphabetic}_][\\p{IsAlphabetic}\\d_]*";
    private static final Collection<FunctionReference<?>> toValidate;

    private Functions() {
    }

    public static DefaultFunction<?> register(DefaultFunction<?> function) {
        Skript.checkAcceptRegistrations();
        String name = function.name();
        if (!name.matches(functionNamePattern)) {
            throw new SkriptAPIException("Invalid function name '%s'".formatted(name));
        }
        if (javaNamespace.getSignature(name) == null) {
            javaNamespace.addSignature((Signature)function.signature());
            javaNamespace.addFunction((Function)((Object)function));
        }
        globalFunctions.put(function.name(), javaNamespace);
        FunctionRegistry.getRegistry().register(null, (Function)((Object)function));
        return function;
    }

    @Deprecated(forRemoval=true, since="2.13")
    public static JavaFunction<?> registerFunction(JavaFunction<?> function) {
        Skript.checkAcceptRegistrations();
        String name = function.getName();
        if (!name.matches(functionNamePattern)) {
            throw new SkriptAPIException("Invalid function name '" + name + "'");
        }
        if (javaNamespace.getSignature(name) == null) {
            javaNamespace.addSignature(function.getSignature());
            javaNamespace.addFunction(function);
        }
        globalFunctions.put(function.getName(), javaNamespace);
        FunctionRegistry.getRegistry().register(null, function);
        return function;
    }

    @Nullable
    public static Function<?> loadFunction(Script script, SectionNode node, Signature<?> signature) {
        ScriptFunction function;
        String name = signature.getName();
        Namespace namespace = Functions.getScriptNamespace(script.getConfig().getFileName());
        if (namespace == null && (namespace = globalFunctions.get(name)) == null) {
            return null;
        }
        if (Skript.debug() || node.debug()) {
            Skript.debug(signature.toString());
        }
        try {
            function = new ScriptFunction(signature, node);
        }
        catch (SkriptAPIException ex) {
            Skript.exception((Throwable)ex, "Error while trying to load a function");
            Functions.unregisterFunction(signature);
            return null;
        }
        if (namespace.getFunction(signature.getName()) == null) {
            namespace.addFunction(function);
        }
        if (function.getSignature().isLocal()) {
            FunctionRegistry.getRegistry().register(script.getConfig().getFileName(), function);
        } else {
            FunctionRegistry.getRegistry().register(null, function);
        }
        return function;
    }

    @Deprecated(forRemoval=true, since="2.14")
    @Nullable
    public static Signature<?> parseSignature(String script, String name, String args, @Nullable String returnType, boolean local) {
        return FunctionParser.parse(script, name, args, returnType, local);
    }

    @Nullable
    public static Signature<?> registerSignature(Signature<?> signature) {
        FunctionRegistry.Retrieval<Signature<?>> existing;
        Parameter<?>[] parameters = signature.parameters().all();
        if (parameters.length == 1 && !parameters[0].isSingle()) {
            existing = FunctionRegistry.getRegistry().getExactSignature(signature.namespace(), signature.getName(), new Class[]{parameters[0].type().arrayType()});
        } else {
            Class[] types = new Class[parameters.length];
            for (int i = 0; i < parameters.length; ++i) {
                types[i] = parameters[i].type();
            }
            existing = FunctionRegistry.getRegistry().getExactSignature(signature.namespace(), signature.getName(), types);
        }
        if (existing.result() == FunctionRegistry.RetrievalResult.EXACT && existing.retrieved().isLocal() == signature.isLocal()) {
            StringBuilder error = new StringBuilder();
            if (existing.retrieved().isLocal()) {
                error.append("Local function ");
            } else {
                error.append("Function ");
            }
            error.append("'%s' with the same argument types already exists".formatted(signature.getName()));
            if (existing.retrieved().namespace() != null) {
                error.append(" in script '%s'.".formatted(existing.retrieved().namespace()));
            } else {
                error.append(".");
            }
            Skript.error(error.toString());
            return null;
        }
        Namespace.Key namespaceKey = new Namespace.Key(Namespace.Origin.SCRIPT, signature.namespace());
        Namespace namespace = namespaces.computeIfAbsent(namespaceKey, k -> new Namespace());
        if (namespace.getSignature(signature.getName()) == null) {
            namespace.addSignature(signature);
        }
        if (!signature.isLocal()) {
            globalFunctions.put(signature.getName(), namespace);
        }
        if (signature.isLocal()) {
            FunctionRegistry.getRegistry().register(signature.namespace(), signature);
        } else {
            FunctionRegistry.getRegistry().register(null, signature);
        }
        Skript.debug("Registered function signature: " + signature.getName());
        return signature;
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    @Nullable
    public static Function<?> getFunction(String name) {
        return Functions.getGlobalFunction(name);
    }

    @Nullable
    public static Function<?> getGlobalFunction(String name) {
        Namespace namespace = globalFunctions.get(name);
        if (namespace == null) {
            return null;
        }
        return namespace.getFunction(name, false);
    }

    @Nullable
    public static Function<?> getLocalFunction(String name, String script) {
        Namespace namespace = null;
        Function<?> function = null;
        namespace = Functions.getScriptNamespace(script);
        if (namespace != null) {
            function = namespace.getFunction(name);
        }
        return function;
    }

    @Nullable
    public static Function<?> getFunction(String name, @Nullable String script) {
        if (script == null) {
            return Functions.getGlobalFunction(name);
        }
        Function<?> function = Functions.getLocalFunction(name, script);
        if (function == null) {
            return Functions.getGlobalFunction(name);
        }
        return function;
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    @Nullable
    public static Signature<?> getSignature(String name) {
        return Functions.getGlobalSignature(name);
    }

    @Nullable
    public static Signature<?> getGlobalSignature(String name) {
        Namespace namespace = globalFunctions.get(name);
        if (namespace == null) {
            return null;
        }
        return namespace.getSignature(name, false);
    }

    @Nullable
    public static Signature<?> getLocalSignature(String name, String script) {
        Namespace namespace = null;
        Signature<?> signature = null;
        namespace = Functions.getScriptNamespace(script);
        if (namespace != null) {
            signature = namespace.getSignature(name);
        }
        return signature;
    }

    @Nullable
    public static Signature<?> getSignature(String name, @Nullable String script) {
        if (script == null) {
            return Functions.getGlobalSignature(name);
        }
        Signature<?> signature = Functions.getLocalSignature(name, script);
        if (signature == null) {
            return Functions.getGlobalSignature(name);
        }
        return signature;
    }

    @Nullable
    public static Namespace getScriptNamespace(String script) {
        return namespaces.get(new Namespace.Key(Namespace.Origin.SCRIPT, script));
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public static int clearFunctions(String script) {
        Namespace namespace = namespaces.remove(new Namespace.Key(Namespace.Origin.SCRIPT, script));
        if (namespace == null) {
            return 0;
        }
        globalFunctions.values().removeIf(loopedNamespaced -> loopedNamespaced == namespace);
        for (Signature<?> sign : namespace.getSignatures()) {
            for (FunctionReference<?> ref : sign.calls()) {
                if (script.equals(ref.namespace())) continue;
                toValidate.add(ref);
            }
        }
        return namespace.getSignatures().size();
    }

    public static void unregisterFunction(Signature<?> signature) {
        FunctionRegistry.getRegistry().remove(signature);
        Iterator<Namespace> namespaceIterator = namespaces.values().iterator();
        while (namespaceIterator.hasNext()) {
            Namespace namespace = namespaceIterator.next();
            if (!namespace.removeSignature(signature)) continue;
            if (!signature.isLocal()) {
                globalFunctions.remove(signature.getName());
            }
            if (!namespace.getSignatures().isEmpty()) break;
            namespaceIterator.remove();
            break;
        }
        for (FunctionReference<?> ref : signature.calls()) {
            if (signature.namespace() == null || signature.namespace().equals(ref.namespace())) continue;
            toValidate.add(ref);
        }
    }

    public static void validateFunctions() {
        for (FunctionReference<?> c : toValidate) {
            c.validate();
        }
        toValidate.clear();
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public static void clearFunctions() {
        globalFunctions.values().removeIf(namespace -> namespace != javaNamespace);
        namespaces.clear();
        assert (toValidate.isEmpty()) : toValidate;
        toValidate.clear();
    }

    @Deprecated(forRemoval=true, since="2.13")
    public static Collection<JavaFunction<?>> getJavaFunctions() {
        return javaNamespace.getFunctions().stream().filter(it -> it instanceof JavaFunction).map(it -> (JavaFunction)it).collect(Collectors.toSet());
    }

    public static Collection<Function<?>> getFunctions() {
        return javaNamespace.getFunctions();
    }

    public static void enableFunctionEvents(SkriptAddon addon) {
        if (addon == null) {
            throw new SkriptAPIException("enabling function events requires addon instance");
        }
        callFunctionEvents = true;
    }

    static {
        namespaces.put(new Namespace.Key(Namespace.Origin.JAVA, "unknown"), javaNamespace);
        globalFunctions = new HashMap<String, Namespace>();
        callFunctionEvents = false;
        toValidate = new ArrayList();
    }
}

