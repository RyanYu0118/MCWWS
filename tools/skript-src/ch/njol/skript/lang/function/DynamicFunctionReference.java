/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionReference;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.Signature;
import ch.njol.skript.lang.util.common.AnyNamed;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Contract;
import ch.njol.skript.util.Utils;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.util.Executable;
import org.skriptlang.skript.util.Validated;

public class DynamicFunctionReference<Result>
implements Contract,
Executable<Event, Result[]>,
Validated,
AnyNamed {
    @NotNull
    private final String name;
    @Nullable
    private final Script source;
    private final Reference<Function<? extends Result>> function;
    private final @UnknownNullability Signature<? extends Result> signature;
    private final Validated validator = Validated.validator();
    private final Map<Input, Expression<?>> checkedInputs = new HashMap();
    private final boolean resolved;

    public DynamicFunctionReference(Function<? extends Result> function) {
        this.resolved = true;
        this.function = new WeakReference<Function<Result>>(function);
        this.name = function.getName();
        this.signature = function.getSignature();
        @Nullable File file = ScriptLoader.getScriptFromName(this.signature.namespace());
        this.source = file != null ? ScriptLoader.getScript(file) : null;
    }

    public DynamicFunctionReference(@NotNull String name) {
        this(name, null);
    }

    public DynamicFunctionReference(@NotNull String name, @Nullable Script source) {
        this.name = name;
        Function<?> function = source != null ? Functions.getFunction(name, source.getConfig().getFileName()) : Functions.getFunction(name, null);
        this.resolved = function != null;
        this.function = new WeakReference(function);
        if (this.resolved) {
            this.signature = function.getSignature();
            @Nullable File file = ScriptLoader.getScriptFromName(this.signature.namespace());
            this.source = file != null ? ScriptLoader.getScript(file) : null;
        } else {
            this.signature = null;
            this.source = null;
        }
    }

    @Nullable
    public Script source() {
        return this.source;
    }

    @Override
    @NotNull
    public String name() {
        return this.name;
    }

    @Override
    public boolean isSingle(Expression<?> ... arguments) {
        if (!this.resolved) {
            return true;
        }
        return this.signature.getContract() != null ? this.signature.getContract().isSingle(arguments) : this.signature.isSingle();
    }

    @Override
    @Nullable
    public Class<?> getReturnType(Expression<?> ... arguments) {
        if (!this.resolved) {
            return Object.class;
        }
        if (this.signature.getContract() != null) {
            return this.signature.getContract().getReturnType(arguments);
        }
        Function<Result> function = this.function.get();
        if (function != null && function.getReturnType() != null) {
            return function.getReturnType().getC();
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Result @Nullable [] execute(Event event, Object ... arguments) {
        if (!this.valid()) {
            return null;
        }
        Function<Result> function = this.function.get();
        if (function == null) {
            return null;
        }
        Object[][] consigned = FunctionReference.consign(arguments);
        try {
            Result[] ResultArray = function.execute(consigned);
            return ResultArray;
        }
        finally {
            function.resetReturnValue();
        }
    }

    @Override
    public void invalidate() {
        this.validator.invalidate();
    }

    @Override
    public boolean valid() {
        return this.resolved && this.validator.valid() && this.function.get() != null && (this.source == null || this.source.valid());
    }

    public String toString() {
        if (this.source != null) {
            return this.name + "() from " + Classes.toString(this.source);
        }
        return this.name + "()";
    }

    @Nullable
    public Expression<?> validate(Expression<?>[] parameters) {
        Input input = new Input(parameters);
        return this.validate(input);
    }

    @Nullable
    public Expression<?> validate(Input input) {
        if (this.checkedInputs.containsKey(input)) {
            return this.checkedInputs.get(input);
        }
        this.checkedInputs.put(input, null);
        if (this.signature == null) {
            return null;
        }
        boolean varArgs = this.signature.getMaxParameters() == 1 && !this.signature.parameters().getFirst().isSingle();
        Expression<?>[] inputParameters = input.parameters();
        if (inputParameters.length > this.signature.getMaxParameters() && !varArgs) {
            return null;
        }
        if (inputParameters.length < this.signature.getMinParameters()) {
            return null;
        }
        Expression[] checkedInputParameters = new Expression[inputParameters.length];
        for (int i = 0; i < inputParameters.length; ++i) {
            Parameter<?> parameter = this.signature.parameters().all()[varArgs ? 0 : i];
            Class<?> target = Utils.getComponentType(parameter.type());
            Expression expression = inputParameters[i].getConvertedExpression(target);
            if (expression == null) {
                return null;
            }
            if (parameter.isSingle() && !expression.isSingle()) {
                return null;
            }
            checkedInputParameters[i] = expression;
        }
        ExpressionList<Object> result = new ExpressionList<Object>(checkedInputParameters, Object.class, true);
        this.checkedInputs.put(input, result);
        return result;
    }

    @Nullable
    public static DynamicFunctionReference<?> parseFunction(String name) {
        if (name.contains(") from ")) {
            String source = name.substring(name.lastIndexOf(" from ") + 6).trim();
            Script script = DynamicFunctionReference.getScript(source);
            return DynamicFunctionReference.resolveFunction(name.substring(0, name.lastIndexOf(" from ")).trim(), script);
        }
        return DynamicFunctionReference.resolveFunction(name, null);
    }

    @Nullable
    public static DynamicFunctionReference<?> resolveFunction(String name, @Nullable Script script) {
        DynamicFunctionReference reference;
        if (name.contains("(") && name.contains(")")) {
            name = name.replaceAll("\\(.*\\).*", "").trim();
        }
        if (!(reference = new DynamicFunctionReference(name, script)).valid()) {
            return null;
        }
        return reference;
    }

    @Nullable
    private static Script getScript(@Nullable String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        @Nullable File file = ScriptLoader.getScriptFromName(source);
        if (file == null || file.isDirectory()) {
            return null;
        }
        return ScriptLoader.getScript(file);
    }

    public static class Input {
        private final Class<?>[] types;
        private final transient Expression<?>[] parameters;

        public Input(Expression<?> ... types) {
            Class[] classes = new Class[types.length];
            for (int i = 0; i < types.length; ++i) {
                classes[i] = types[i].getReturnType();
            }
            this.parameters = types;
            this.types = classes;
        }

        private Expression<?>[] parameters() {
            return this.parameters;
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Input)) {
                return false;
            }
            Input input = (Input)object;
            return Arrays.equals(this.parameters, input.parameters) && Objects.deepEquals(this.types, input.types);
        }

        public int hashCode() {
            return Arrays.hashCode(this.types) ^ Arrays.hashCode(this.parameters);
        }
    }
}

