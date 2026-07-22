/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Lists
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Contract;
import ch.njol.skript.util.Utils;
import ch.njol.util.StringUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.common.function.Parameters;

public class Signature<T>
implements org.skriptlang.skript.common.function.Signature<T> {
    @Nullable
    final String script;
    final String name;
    private final Parameters parameters;
    final boolean local;
    @Nullable
    final ClassInfo<T> returnType;
    final Class<?> returns;
    final boolean single;
    final Collection<FunctionReference<?>> calls;
    @Nullable
    final Contract contract;
    @Nullable
    String originClassPath;

    static Class<?> getReturns(boolean single, Class<?> cls) {
        if (single) {
            return cls;
        }
        return cls.arrayType();
    }

    public Signature(@Nullable String script, String name, Parameter<?>[] parameters, boolean local, @Nullable ClassInfo<T> returnType, boolean single, @Nullable Contract contract) {
        this.script = script;
        this.name = name;
        this.parameters = Signature.initParameters(parameters);
        this.local = local;
        this.returnType = returnType;
        this.single = single;
        this.returns = returnType == null ? null : Signature.getReturns(single, returnType.getC());
        this.contract = contract;
        this.calls = Collections.newSetFromMap(new WeakHashMap());
        this.originClassPath = "";
    }

    public Signature(@Nullable String script, String name, Parameter<?>[] parameters, boolean local, @Nullable ClassInfo<T> returnType, boolean single, String stacktrace) {
        this(script, name, parameters, local, returnType, single, (Contract)null);
        this.originClassPath = stacktrace;
    }

    public Signature(String script, String name, Parameter<?>[] parameters, boolean local, ClassInfo<T> returnType, boolean single, String stacktrace, @Nullable Contract contract) {
        this(script, name, parameters, local, returnType, single, contract);
        this.originClassPath = stacktrace;
    }

    public Signature(@Nullable String script, String name, Parameters parameters, Class<T> returnType, boolean local, @Nullable Contract contract) {
        this.script = script;
        this.name = name;
        this.parameters = parameters;
        this.local = local;
        this.returns = returnType;
        if (returnType != null) {
            this.returnType = Classes.getExactClassInfo(Utils.getComponentType(returnType));
            this.single = !returnType.isArray();
        } else {
            this.returnType = null;
            this.single = true;
        }
        this.contract = contract;
        this.calls = Collections.newSetFromMap(new WeakHashMap());
    }

    public Signature(@Nullable String script, String name, Parameters parameters, Class<T> returnType, boolean local) {
        this(script, name, parameters, returnType, local, null);
    }

    public Signature(String namespace, String name, org.skriptlang.skript.common.function.Parameter<?>[] parameters, Class<T> returnType, boolean single, @Nullable Contract contract) {
        this(namespace, name, Signature.initParameters(parameters), returnType, false, contract);
    }

    private static Parameters initParameters(org.skriptlang.skript.common.function.Parameter<?>[] params) {
        LinkedHashMap map = new LinkedHashMap();
        for (org.skriptlang.skript.common.function.Parameter<?> parameter : params) {
            map.put(parameter.name(), parameter);
        }
        return new Parameters(map);
    }

    static Parameter<?> toOldParameter(org.skriptlang.skript.common.function.Parameter<?> parameter) {
        if (parameter == null) {
            return null;
        }
        ClassInfo<?> classInfo = Classes.getExactClassInfo(Utils.getComponentType(parameter.type()));
        return new Parameter(parameter.name(), classInfo, !parameter.type().isArray(), null, parameter.modifiers().toArray(new Parameter.Modifier[0]));
    }

    @Deprecated(forRemoval=true, since="2.14")
    public Parameter<?> getParameter(int index) {
        return this.getParameters()[index];
    }

    @Deprecated(forRemoval=true, since="2.14")
    public Parameter<?>[] getParameters() {
        return (Parameter[])Arrays.stream(this.parameters.all()).map(Signature::toOldParameter).toArray(Parameter[]::new);
    }

    @Override
    @Nullable
    public Class<T> returnType() {
        return this.returns;
    }

    @Override
    @NotNull
    public Parameters parameters() {
        return this.parameters;
    }

    @Override
    public Contract contract() {
        return this.contract;
    }

    @Override
    public void addCall(FunctionReference<?> reference) {
        this.calls.add(reference);
    }

    public org.skriptlang.skript.common.function.Parameter<?> getParameter(@NotNull String name) {
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        return this.parameters.get(name);
    }

    public String getName() {
        return this.name;
    }

    public boolean isLocal() {
        return this.local;
    }

    public String namespace() {
        return this.script;
    }

    @Nullable
    public ClassInfo<T> getReturnType() {
        return this.returnType;
    }

    @Override
    public boolean isSingle() {
        return this.single;
    }

    @Deprecated(forRemoval=true, since="2.13")
    public String getOriginClassPath() {
        return this.originClassPath;
    }

    @Nullable
    public Contract getContract() {
        return this.contract;
    }

    public Collection<FunctionReference<?>> calls() {
        return this.calls;
    }

    public int getMaxParameters() {
        return this.parameters.size();
    }

    public int getMinParameters() {
        LinkedList params = new LinkedList(List.of(this.parameters.all()));
        int i = this.parameters.size() - 1;
        for (org.skriptlang.skript.common.function.Parameter parameter : Lists.reverse(params)) {
            if (!parameter.hasModifier(Parameter.Modifier.OPTIONAL)) {
                return i + 1;
            }
            --i;
        }
        return 0;
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public String toString() {
        return this.toString(true, Skript.debug());
    }

    public String toString(boolean includeReturnType, boolean debug) {
        StringBuilder signatureBuilder = new StringBuilder();
        if (this.local) {
            signatureBuilder.append("local ");
        }
        signatureBuilder.append(this.name);
        signatureBuilder.append('(').append(StringUtils.join(this.parameters.all(), ", ")).append(')');
        if (includeReturnType && this.returns != null) {
            signatureBuilder.append(" :: ");
            signatureBuilder.append(Utils.toEnglishPlural(this.returnType.getCodeName(), this.returns.isArray()));
        }
        return signatureBuilder.toString();
    }
}

