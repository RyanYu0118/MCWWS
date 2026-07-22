/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionRegistry;
import ch.njol.skript.lang.function.Signature;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Contract;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.util.Executable;

@Deprecated(forRemoval=true, since="2.14")
public class FunctionReference<T>
implements Contract,
Executable<Event, T[]> {
    private static final String AMBIGUOUS_ERROR = "Skript cannot determine which function named '%s' to call. The following functions were matched: %s. Try clarifying the type of the arguments using the 'value within' expression.";
    final String functionName;
    @Nullable
    private Signature<? extends T> signature;
    @Nullable
    private Function<? extends T> function;
    private boolean singleListParam;
    private final Expression<?>[] parameters;
    private boolean single;
    @Nullable
    final Class<? extends T>[] returnTypes;
    @Nullable
    private final Node node;
    @Nullable
    public final String script;
    private Contract contract;
    private Class<?>[] parameterTypes;

    public FunctionReference(String functionName, @Nullable Node node, @Nullable String script, @Nullable Class<? extends T>[] returnTypes, Expression<?>[] params) {
        this.functionName = functionName;
        this.node = node;
        this.script = script;
        this.returnTypes = returnTypes;
        this.parameters = params;
        this.contract = this;
    }

    public boolean validateParameterArity(boolean first) {
        if (!first && this.script == null) {
            return false;
        }
        Signature<?> sign = this.getRegisteredSignature();
        if (sign == null) {
            return false;
        }
        return this.parameters.length >= sign.getMinParameters();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean validateFunction(boolean first) {
        if (!first && this.script == null) {
            return false;
        }
        Function<? extends T> previousFunction = this.function;
        this.function = null;
        SkriptLogger.setNode(this.node);
        Skript.debug("Validating function " + this.functionName);
        Signature<?> sign = this.getRegisteredSignature();
        StringJoiner args = new StringJoiner(", ");
        for (Class<?> parameterType : this.parameterTypes) {
            args.add(Classes.getSuperClassInfo(Utils.getComponentType(parameterType)).getCodeName());
        }
        String stringified = "%s(%s)".formatted(this.functionName, args);
        if (sign == null) {
            if (first) {
                Skript.error("The function '" + stringified + "' does not exist.");
            } else {
                Skript.error("The function '" + stringified + "' was deleted or renamed, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                this.function = previousFunction;
            }
            return false;
        }
        Class<? extends T>[] expectedReturnTypes = this.returnTypes;
        if (expectedReturnTypes != null) {
            Class<?> candidateReturnType = sign.returnType();
            if (candidateReturnType == null) {
                if (first) {
                    Skript.error("The function '" + stringified + "' doesn't return any value.");
                } else {
                    Skript.error("The function '" + stringified + "' was redefined with no return value, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                    this.function = previousFunction;
                }
                return false;
            }
            if (!Converters.converterExists(candidateReturnType, expectedReturnTypes)) {
                if (first) {
                    Skript.error("The returned value of the function '" + stringified + "', " + String.valueOf(candidateReturnType) + ", is " + SkriptParser.notOfType(expectedReturnTypes) + ".");
                } else {
                    Skript.error("The function '" + stringified + "' was redefined with a different, incompatible return type, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                    this.function = previousFunction;
                }
                return false;
            }
            if (first) {
                this.single = sign.isSingle();
            } else if (this.single && !sign.isSingle()) {
                Skript.error("The function '" + this.functionName + "' was redefined with a different, incompatible return type, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                this.function = previousFunction;
                return false;
            }
        }
        boolean bl = this.singleListParam = sign.getMaxParameters() == 1 && !sign.parameters().getFirst().isSingle();
        if (!this.singleListParam && this.parameters.length > sign.getMaxParameters()) {
            if (first) {
                if (sign.getMaxParameters() == 0) {
                    Skript.error("The function '" + stringified + "' has no arguments, but " + this.parameters.length + " are given. To call a function without parameters, just write the function name followed by '()', e.g. 'func()'.");
                } else {
                    Skript.error("The function '" + stringified + "' has only " + sign.getMaxParameters() + " argument" + (sign.getMaxParameters() == 1 ? "" : "s") + ", but " + this.parameters.length + " are given. If you want to use lists in function calls, you have to use additional parentheses, e.g. 'give(player, (iron ore and gold ore))'");
                }
            } else {
                Skript.error("The function '" + stringified + "' was redefined with a different, incompatible amount of arguments, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                this.function = previousFunction;
            }
            return false;
        }
        if (this.parameters.length < sign.getMinParameters()) {
            if (first) {
                Skript.error("The function '" + stringified + "' requires at least " + sign.getMinParameters() + " argument" + (sign.getMinParameters() == 1 ? "" : "s") + ", but only " + this.parameters.length + " " + (this.parameters.length == 1 ? "is" : "are") + " given.");
            } else {
                Skript.error("The function '" + stringified + "' was redefined with a different, incompatible amount of arguments, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                this.function = previousFunction;
            }
            return false;
        }
        for (int i = 0; i < this.parameters.length; ++i) {
            Parameter<?> signatureParam = sign.parameters().get(this.singleListParam ? 0 : i);
            RetainingLogHandler log = SkriptLogger.startRetainingLog();
            try {
                Class<?> target = Utils.getComponentType(signatureParam.type());
                Expression exprParam = this.parameters[i].getConvertedExpression(target);
                if (exprParam == null) {
                    if (first) {
                        if (LiteralUtils.hasUnparsedLiteral(this.parameters[i])) {
                            Skript.error("Can't understand this expression: " + this.parameters[i].toString());
                        } else {
                            String type = Classes.toString(Classes.getSuperClassInfo(target));
                            Skript.error("The " + StringUtils.fancyOrderNumber(i + 1) + " argument given to the function '" + stringified + "' is not of the required type " + type + ". Check the correct order of the arguments and put lists into parentheses if appropriate (e.g. 'give(player, (iron ore and gold ore))'). Please note that storing the value in a variable and then using that variable as parameter may suppress this error, but it still won't work.");
                        }
                    } else {
                        Skript.error("The function '" + stringified + "' was redefined with different, incompatible arguments, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                        this.function = previousFunction;
                    }
                    boolean type = false;
                    return type;
                }
                if (signatureParam.isSingle() && !exprParam.isSingle()) {
                    if (first) {
                        Skript.error("The " + StringUtils.fancyOrderNumber(i + 1) + " argument given to the function '" + this.functionName + "' is plural, but a single argument was expected");
                    } else {
                        Skript.error("The function '" + stringified + "' was redefined with different, incompatible arguments, but is still used in other script(s). These will continue to use the old version of the function until Skript restarts.");
                        this.function = previousFunction;
                    }
                    boolean type = false;
                    return type;
                }
                if (signatureParam.hasModifier(Parameter.Modifier.RANGED) && exprParam instanceof Literal) {
                    Literal literalParam = (Literal)exprParam;
                    Parameter.Modifier.RangedModifier range = signatureParam.getModifier(Parameter.Modifier.RangedModifier.class);
                    if (!range.inRange(literalParam.getArray())) {
                        Skript.error("The argument '" + signatureParam.name() + "' only accepts values between " + Classes.toString(range.getMin()) + " and " + Classes.toString(range.getMax()) + ". Provided: " + literalParam.toString(null, Skript.debug()));
                        boolean bl2 = false;
                        return bl2;
                    }
                }
                this.parameters[i] = exprParam;
                continue;
            }
            finally {
                log.printLog();
            }
        }
        this.signature = sign;
        FunctionReference.Argument[] arguments = (FunctionReference.Argument[])Arrays.stream(this.parameters).map(it -> new FunctionReference.Argument<Expression>(FunctionReference.ArgumentType.UNNAMED, null, (Expression)it)).toArray(FunctionReference.Argument[]::new);
        sign.calls().add(new org.skriptlang.skript.common.function.FunctionReference<T>(this.script, this.functionName, this.signature, arguments));
        Contract contract = sign.getContract();
        if (contract != null) {
            this.contract = contract;
        }
        return true;
    }

    private void parseParameters() {
        if (this.parameterTypes != null) {
            return;
        }
        this.parameterTypes = new Class[this.parameters.length];
        for (int i = 0; i < this.parameters.length; ++i) {
            Expression parsed = LiteralUtils.defendExpression(this.parameters[i]);
            this.parameterTypes[i] = parsed.getReturnType();
        }
    }

    private Signature<?> getRegisteredSignature() {
        FunctionRegistry.Retrieval<Signature<?>> attempt;
        this.parseParameters();
        if (Skript.debug()) {
            Skript.debug("Getting signature for '%s' with types %s", this.functionName, Arrays.toString(Arrays.stream(this.parameterTypes).map(Class::getSimpleName).toArray()));
        }
        if ((attempt = FunctionRegistry.getRegistry().getSignature(this.script, this.functionName, this.parameterTypes)).result() == FunctionRegistry.RetrievalResult.EXACT) {
            return attempt.retrieved();
        }
        if (attempt.result() == FunctionRegistry.RetrievalResult.AMBIGUOUS) {
            this.ambiguousError(attempt.conflictingArgs());
        }
        return null;
    }

    private Function<?> getRegisteredFunction() {
        FunctionRegistry.Retrieval<Function<?>> attempt;
        this.parseParameters();
        if (Skript.debug()) {
            Skript.debug("Getting function '%s' with types %s", this.functionName, Arrays.toString(Arrays.stream(this.parameterTypes).map(Class::getSimpleName).toArray()));
        }
        if ((attempt = FunctionRegistry.getRegistry().getFunction(this.script, this.functionName, this.parameterTypes)).result() == FunctionRegistry.RetrievalResult.EXACT) {
            return attempt.retrieved();
        }
        if (attempt.result() == FunctionRegistry.RetrievalResult.AMBIGUOUS) {
            this.ambiguousError(attempt.conflictingArgs());
        }
        return null;
    }

    @Nullable
    public Function<? extends T> getFunction() {
        return this.function;
    }

    public String @Nullable [] returnedKeys() {
        if (this.function != null) {
            return this.function.returnedKeys();
        }
        return null;
    }

    public boolean resetReturnValue() {
        if (this.function != null) {
            return this.function.resetReturnValue();
        }
        return false;
    }

    protected T @Nullable [] execute(Event event) {
        if (this.function == null) {
            this.function = this.getRegisteredFunction();
        }
        if (this.function == null) {
            Skript.error("Couldn't resolve call for '" + this.functionName + "'.");
            return null;
        }
        Object[][] params = new Object[this.singleListParam ? 1 : this.parameters.length][];
        if (this.singleListParam) {
            params[0] = this.evaluateSingleListParameter(this.function.signature().parameters().getFirst(), this.parameters, event);
        } else {
            for (int i = 0; i < this.parameters.length; ++i) {
                params[i] = this.function.signature().parameters().get(i).evaluate(this.parameters[i], event);
            }
        }
        return this.function.execute(params);
    }

    private Object[] evaluateSingleListParameter(Parameter<?> parameter, Expression<?>[] arguments, Event event) {
        if (arguments.length == 0) {
            return null;
        }
        if (arguments.length == 1) {
            return parameter.evaluate(arguments[0], event);
        }
        if (!parameter.hasModifier(Parameter.Modifier.KEYED)) {
            ArrayList<Object> list = new ArrayList<Object>();
            for (Expression<?> argument : arguments) {
                list.addAll(Arrays.asList(parameter.evaluate(argument, event)));
            }
            return list.toArray();
        }
        ArrayList<Object> values = new ArrayList<Object>();
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        int keyIndex = 1;
        for (Expression<?> argument : arguments) {
            ?[] valuesArray = argument.getArray(event);
            String[] keysArray = KeyProviderExpression.areKeysRecommended(argument) ? ((KeyProviderExpression)argument).getArrayKeys(event) : null;
            for (int i = 0; i < valuesArray.length; ++i) {
                if (keysArray == null) {
                    while (keys.contains(String.valueOf(keyIndex))) {
                        ++keyIndex;
                    }
                    keys.add(String.valueOf(keyIndex++));
                } else if (!keys.add(keysArray[i])) continue;
                values.add(Classes.clone(valuesArray[i]));
            }
        }
        return KeyedValue.zip(values.toArray(), keys.toArray(new String[0]));
    }

    public boolean isSingle() {
        return this.contract.isSingle(this.parameters);
    }

    @Override
    public boolean isSingle(Expression<?> ... arguments) {
        return this.single;
    }

    @Nullable
    public Class<? extends T> getReturnType() {
        return this.contract.getReturnType(this.parameters);
    }

    @Override
    @Nullable
    public Class<?> getReturnType(Expression<?> ... arguments) {
        if (this.signature == null) {
            throw new SkriptAPIException("Signature of function is null when return type is asked!");
        }
        return this.signature.returnType();
    }

    public Contract getContract() {
        return this.contract;
    }

    public String toString(@Nullable Event event, boolean debug) {
        StringBuilder b = new StringBuilder(this.functionName + "(");
        for (int i = 0; i < this.parameters.length; ++i) {
            if (i != 0) {
                b.append(", ");
            }
            b.append(this.parameters[i].toString(event, debug));
        }
        b.append(")");
        return b.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public T[] execute(Event event, Object ... arguments) {
        if (this.function == null) {
            this.function = this.getRegisteredFunction();
        }
        if (this.function == null) {
            Skript.error("Couldn't resolve call for '" + this.functionName + "'.");
            return null;
        }
        Object[][] consigned = FunctionReference.consign(arguments);
        try {
            T[] TArray = this.function.execute(consigned);
            return TArray;
        }
        finally {
            this.resetReturnValue();
        }
    }

    static Object[][] consign(Object ... arguments) {
        Object[][] consigned = new Object[arguments.length][];
        for (int i = 0; i < consigned.length; ++i) {
            consigned[i] = arguments[i] instanceof Object[] || arguments[i] == null ? (Object[])arguments[i] : new Object[]{arguments[i]};
        }
        return consigned;
    }

    private void ambiguousError(Class<?>[][] conflictingArgs) {
        ArrayList<String> parts = new ArrayList<String>();
        for (Class<?>[] args : conflictingArgs) {
            String argNames = Arrays.stream(args).map(arg -> {
                String name = Classes.getExactClassName(arg);
                if (name == null) {
                    return arg.getSimpleName();
                }
                return name.toLowerCase();
            }).collect(Collectors.joining(", "));
            parts.add("%s(%s)".formatted(this.functionName, argNames));
        }
        Skript.error(AMBIGUOUS_ERROR, this.functionName, StringUtils.join(parts, ", ", " and "));
    }
}

