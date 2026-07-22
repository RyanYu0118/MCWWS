/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.FunctionRegistry;
import ch.njol.skript.lang.function.Signature;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.Sets;
import java.lang.invoke.CallSite;
import java.lang.invoke.TypeDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionArgumentParser;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.common.function.ScriptParameter;

public record FunctionReferenceParser(ParseContext context, int flags) {
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile("(?<name>[\\p{IsAlphabetic}_][\\p{IsAlphabetic}\\d_]*)\\((?<args>.*)\\)");
    private static final ArgsMessage UNEXPECTED_ARGUMENT = new ArgsMessage("functions.unexpected argument");
    private static final ArgsMessage INVALID_ARGUMENT = new ArgsMessage("functions.invalid argument");
    private static final ArgsMessage UNKNOWN_FUNCTION = new ArgsMessage("functions.unknown function");
    private static final ArgsMessage POTENTIAL_SIGNATURE = new ArgsMessage("functions.potential signature");

    public <T> FunctionReference<T> parseFunctionReference(String expr) {
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            if (!expr.endsWith(")")) {
                log.printLog();
                FunctionReference<T> functionReference = null;
                return functionReference;
            }
            Matcher matcher = FUNCTION_CALL_PATTERN.matcher(expr);
            if (!matcher.matches()) {
                log.printLog();
                FunctionReference<T> functionReference = null;
                return functionReference;
            }
            String functionName = matcher.group("name");
            String args = matcher.group("args");
            int i2 = 0;
            while (i2 < args.length()) {
                if (i2 == -1) {
                    log.printLog();
                    FunctionReference<T> functionReference = null;
                    return functionReference;
                }
                i2 = SkriptParser.next(args, i2, this.context);
            }
            if ((this.flags & 1) == 0) {
                Skript.error("Functions cannot be used here (or there is a problem with your arguments).");
                log.printError();
                FunctionReference<T> i2 = null;
                return i2;
            }
            FunctionReference.Argument<String>[] arguments = new FunctionArgumentParser(args).getArguments();
            FunctionReference<T> functionReference = this.parseFunctionReference(functionName, arguments, log);
            return functionReference;
        }
    }

    public <T> FunctionReference<T> parseFunctionReference(String name, FunctionReference.Argument<String>[] arguments, ParseLogHandler log) {
        HashSet<String> named = new HashSet<String>();
        for (FunctionReference.Argument<String> argument : arguments) {
            boolean added;
            if (argument.type() != FunctionReference.ArgumentType.NAMED || (added = named.add(argument.name()))) continue;
            Skript.error(Language.get("functions.already assigned value to parameter"), argument.name());
            log.printError();
            return null;
        }
        ParserInstance parser = ParserInstance.get();
        String namespace = parser.isActive() ? parser.getCurrentScript().getConfig().getFileName() : null;
        Set<Signature<?>> options = FunctionRegistry.getRegistry().getSignatures(namespace, name);
        if (options.isEmpty()) {
            this.doesNotExist(name, arguments, options);
            log.printError();
            return null;
        }
        HashSet exacts = new HashSet();
        HashSet lists = new HashSet();
        for (Signature<?> option : options) {
            if (option.parameters().size() == 1 && !option.parameters().getFirst().isSingle()) {
                lists.add(option);
                continue;
            }
            exacts.add(option);
        }
        Set<FunctionReference<T>> exactReferences = this.getExactReferences(namespace, name, exacts, arguments);
        if (exactReferences == null) {
            log.printError();
            return null;
        }
        if (exactReferences.size() == 1) {
            return exactReferences.stream().findAny().orElse(null);
        }
        Set<FunctionReference<T>> listReferences = this.getListReferences(namespace, name, lists, arguments);
        if (listReferences == null) {
            log.printError();
            return null;
        }
        exactReferences.addAll(listReferences);
        if (exactReferences.isEmpty()) {
            this.doesNotExist(name, arguments, (Set<Signature<?>>)Sets.union(exacts, lists));
            log.printError();
            return null;
        }
        if (exactReferences.size() == 1) {
            return exactReferences.stream().findAny().orElse(null);
        }
        this.ambiguousError(name, exactReferences);
        log.printError();
        return null;
    }

    private <T> Set<FunctionReference<T>> getExactReferences(String namespace, String name, Set<Signature<?>> signatures, FunctionReference.Argument<String>[] arguments) {
        HashSet<FunctionReference<T>> exactReferences = new HashSet<FunctionReference<T>>();
        boolean hasNames = Arrays.stream(arguments).anyMatch(argument -> argument.type() == FunctionReference.ArgumentType.NAMED);
        FunctionReference.Argument<String>[] originalArguments = arguments;
        for (Signature<?> signature : signatures) {
            if (arguments.length > signature.getMaxParameters() || arguments.length < signature.getMinParameters()) continue;
            arguments = originalArguments;
            SequencedMap<String, Parameter<?>> parameters = signature.parameters().sequencedMap();
            FunctionReference.Argument[] parseArguments = new FunctionReference.Argument[parameters.size()];
            ArgumentParseTarget[] parseTargets = new ArgumentParseTarget[parameters.size()];
            int index = 0;
            boolean hasPlaceholders = false;
            for (Map.Entry entry : parameters.entrySet()) {
                EmptyExpression fallback;
                Parameter parameter = (Parameter)entry.getValue();
                FunctionReference.Argument<Object> argument2 = null;
                if (hasNames) {
                    for (FunctionReference.Argument<String> candidate : arguments) {
                        if (!((String)entry.getKey()).equals(candidate.name())) continue;
                        argument2 = candidate;
                        break;
                    }
                } else if (index < arguments.length) {
                    argument2 = new FunctionReference.Argument<String>(FunctionReference.ArgumentType.UNNAMED, (String)entry.getKey(), arguments[index].value());
                }
                if (argument2 == null) {
                    argument2 = new FunctionReference.Argument<Object>(FunctionReference.ArgumentType.UNNAMED, (String)entry.getKey(), null);
                    hasPlaceholders = true;
                }
                parseArguments[index] = argument2;
                Class<?> targetType = Utils.getComponentType(parameter.type());
                if (parameter instanceof ScriptParameter) {
                    ScriptParameter sp = (ScriptParameter)parameter;
                    fallback = sp.defaultValue();
                } else {
                    fallback = parameter.hasModifier(Parameter.Modifier.OPTIONAL) ? new EmptyExpression() : null;
                }
                parseTargets[index] = new ArgumentParseTarget(targetType, fallback);
                ++index;
            }
            if (hasPlaceholders) {
                boolean copied = false;
                for (int i = 0; i < arguments.length; ++i) {
                    FunctionReference.Argument<String> argument3 = arguments[i];
                    if (argument3.type() != FunctionReference.ArgumentType.NAMED || parameters.containsKey(argument3.name())) continue;
                    if (!copied) {
                        arguments = Arrays.copyOf(arguments, arguments.length);
                        copied = true;
                    }
                    arguments[i] = new FunctionReference.Argument<String>(FunctionReference.ArgumentType.UNNAMED, argument3.name(), argument3.raw());
                }
                ArrayList<String> priorNames = new ArrayList<String>();
                int lastCheck = -1;
                int lastFilled = -1;
                block8: for (int i = 0; i < arguments.length; ++i) {
                    int j;
                    FunctionReference.Argument<String> argument4 = arguments[i];
                    if (argument4.type() == FunctionReference.ArgumentType.NAMED) {
                        priorNames.add(argument4.name());
                        if (i <= 0 || arguments[i - 1].type() != FunctionReference.ArgumentType.NAMED) continue;
                        lastCheck = -1;
                        continue;
                    }
                    String nextName = null;
                    for (j = i + 1; j < arguments.length; ++j) {
                        FunctionReference.Argument<String> nextArgument = arguments[j];
                        if (nextArgument.type() != FunctionReference.ArgumentType.NAMED) continue;
                        nextName = nextArgument.name();
                        break;
                    }
                    for (j = lastCheck + 1; j < parseArguments.length; ++j) {
                        FunctionReference.Argument parseArgument = parseArguments[j];
                        if (parseArgument.type() == FunctionReference.ArgumentType.NAMED) {
                            if (parseArgument.name().equals(nextName)) break;
                            priorNames.remove(parseArgument.name());
                            continue;
                        }
                        if (j <= lastFilled || !priorNames.isEmpty()) continue;
                        parseArguments[j] = new FunctionReference.Argument<String>(FunctionReference.ArgumentType.UNNAMED, parseArgument.name(), argument4.value());
                        lastCheck = j;
                        lastFilled = j;
                        continue block8;
                    }
                    if (argument4.name() != null) {
                        Skript.error(UNEXPECTED_ARGUMENT.toString(argument4.name()));
                    } else {
                        Skript.error(Language.get("functions.mixing named and unnamed arguments"));
                    }
                    return null;
                }
            }
            ArgumentParseResult result = this.parseFunctionArguments(parseArguments, parseTargets);
            switch (result.type().ordinal()) {
                case 2: {
                    return null;
                }
                case 0: {
                    FunctionReference reference = new FunctionReference(namespace, name, signature, result.parsed());
                    if (!reference.validate()) break;
                    exactReferences.add(reference);
                    break;
                }
            }
        }
        return exactReferences;
    }

    private <T> Set<FunctionReference<T>> getListReferences(String namespace, String name, Set<Signature<?>> signatures, FunctionReference.Argument<String>[] arguments) {
        if (arguments.length > 1) {
            for (FunctionReference.Argument<String> argument : arguments) {
                if (argument.type() != FunctionReference.ArgumentType.NAMED) continue;
                this.doesNotExist(name, arguments, signatures);
                return null;
            }
        }
        HashSet<FunctionReference<T>> references = new HashSet<FunctionReference<T>>();
        block1: for (Signature<?> signature : signatures) {
            FunctionReference reference;
            Expression<T>[] expressionArray;
            EmptyExpression fallback;
            Parameter<?> parameter = signature.parameters().getFirst();
            TypeDescriptor.OfField targetType = parameter.type().componentType();
            if (parameter instanceof ScriptParameter) {
                ScriptParameter sp = (ScriptParameter)parameter;
                fallback = sp.defaultValue();
            } else {
                fallback = parameter.hasModifier(Parameter.Modifier.OPTIONAL) ? new EmptyExpression() : null;
            }
            ArgumentParseTarget[] parseTargets = new ArgumentParseTarget[]{new ArgumentParseTarget((Class<?>)targetType, fallback)};
            if (arguments.length == 1 && arguments[0].type() == FunctionReference.ArgumentType.NAMED && !arguments[0].name().equals(parameter.name())) {
                this.doesNotExist(name, arguments, signatures);
                continue;
            }
            String joined = arguments.length == 0 ? null : Arrays.stream(arguments).map(FunctionReference.Argument::value).collect(Collectors.joining(", "));
            FunctionReference.Argument<String> argument = new FunctionReference.Argument<String>(FunctionReference.ArgumentType.NAMED, parameter.name(), joined);
            FunctionReference.Argument[] array = CollectionUtils.array(argument);
            ArgumentParseResult result = this.parseFunctionArguments(array, parseTargets);
            if (result.type() == ArgumentParseResultType.LIST_ERROR) {
                return null;
            }
            if (result.type() != ArgumentParseResultType.OK) continue;
            if (result.parsed().length == 1 && (expressionArray = result.parsed()[0].value()) instanceof ExpressionList) {
                ExpressionList list = (ExpressionList)expressionArray;
                expressionArray = list.getExpressions();
                int n = expressionArray.length;
                for (int i = 0; i < n; ++i) {
                    Expression expression = expressionArray[i];
                    if (!(expression instanceof ExpressionList)) continue;
                    this.doesNotExist(name, arguments, signatures);
                    continue block1;
                }
            }
            if (!(reference = new FunctionReference(namespace, name, signature, result.parsed())).validate()) continue;
            references.add(reference);
        }
        return references;
    }

    private <T> void ambiguousError(String name, Set<FunctionReference<T>> references) {
        ArrayList<CallSite> parts = new ArrayList<CallSite>();
        for (FunctionReference<T> reference : references) {
            String builder = reference.name() + "(" + Arrays.stream(reference.signature().parameters().all()).map(it -> {
                if (it.type().isArray()) {
                    return Classes.getSuperClassInfo(it.type().componentType()).getName().getPlural();
                }
                return Classes.getSuperClassInfo(it.type()).getName().getSingular();
            }).collect(Collectors.joining(", ")) + ")";
            parts.add((CallSite)((Object)builder));
        }
        Skript.error(Language.get("functions.ambiguous function call"), name, StringUtils.join(parts, ", ", " or "));
    }

    private void doesNotExist(String name, FunctionReference.Argument<String>[] arguments, Set<Signature<?>> possibleSignatures) {
        StringJoiner joiner = new StringJoiner(", ");
        ArrayList argumentTypes = new ArrayList();
        for (FunctionReference.Argument<String> argument : arguments) {
            SkriptParser parser = new SkriptParser(argument.value(), this.flags | 2, this.context);
            Expression expression = LiteralUtils.defendExpression(parser.parseExpression(Object.class));
            Object argumentName = argument.type() == FunctionReference.ArgumentType.NAMED ? argument.name() + ": " : "";
            if (!LiteralUtils.canInitSafely(expression)) {
                joiner.add((String)argumentName + "?");
                continue;
            }
            Class returnType = expression.getReturnType();
            argumentTypes.add(returnType);
            ClassInfo classInfo = Classes.getSuperClassInfo(returnType);
            if (expression.isSingle()) {
                joiner.add((String)argumentName + classInfo.getName().getSingular());
                continue;
            }
            joiner.add((String)argumentName + classInfo.getName().getPlural());
        }
        Object possibleMatch = "";
        Optional<Signature> intended = possibleSignatures.stream().filter(signature -> {
            ArrayList currentArgumentTypes = new ArrayList(argumentTypes);
            Arrays.stream(signature.parameters().all()).map(parameter -> Utils.getComponentType(parameter.type())).forEach(type -> {
                Iterator iterator = currentArgumentTypes.iterator();
                while (iterator.hasNext()) {
                    if (!type.isAssignableFrom((Class)iterator.next())) continue;
                    iterator.remove();
                    break;
                }
            });
            return currentArgumentTypes.isEmpty();
        }).min(Comparator.comparingInt(signature -> Math.abs(arguments.length - signature.getMaxParameters())));
        if (intended.isPresent()) {
            possibleMatch = " " + POTENTIAL_SIGNATURE.toString(intended.get().toString(false, false));
        }
        Skript.error(UNKNOWN_FUNCTION.toString(name, joiner) + (String)possibleMatch);
    }

    private ArgumentParseResult parseFunctionArguments(FunctionReference.Argument<String>[] arguments, ArgumentParseTarget[] targets) {
        assert (arguments.length == targets.length);
        assert (Arrays.stream(targets).map(ArgumentParseTarget::type).noneMatch(Class::isArray));
        FunctionReference.Argument[] parsed = new FunctionReference.Argument[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            ExpressionList list;
            FunctionReference.Argument<String> argument = arguments[i];
            ArgumentParseTarget targetData = targets[i];
            Expression<?> expression = this.parseExpression(argument, targetData);
            if (expression == null) {
                return new ArgumentParseResult(ArgumentParseResultType.PARSE_FAIL, null);
            }
            if (expression instanceof ExpressionList && !(list = (ExpressionList)expression).getAnd()) {
                Skript.error(Language.get("functions.or in arguments"));
                return new ArgumentParseResult(ArgumentParseResultType.LIST_ERROR, null);
            }
            parsed[i] = new FunctionReference.Argument(argument.type(), argument.name(), expression);
        }
        return new ArgumentParseResult(ArgumentParseResultType.OK, parsed);
    }

    private Expression<?> parseExpression(FunctionReference.Argument<String> argument, ArgumentParseTarget targetData) {
        if (argument.value() == null) {
            return targetData.fallback;
        }
        SkriptParser parser = new SkriptParser(argument.value(), this.flags | 2, this.context);
        try (ParseLogHandler logHandler = new ParseLogHandler().start();){
            Expression expression = parser.parseExpression(targetData.type());
            if (expression == null) {
                logHandler.printError(INVALID_ARGUMENT.toString(argument.name(), Classes.getSuperClassInfo(targetData.type()).getName().getSingular(), argument.value()));
            }
            Expression expression2 = expression;
            return expression2;
        }
    }

    private record ArgumentParseTarget(@NotNull Class<?> type, @Nullable Expression<?> fallback) {
    }

    public static class EmptyExpression
    extends SimpleLiteral<Integer> {
        public EmptyExpression() {
            super(1, true);
        }
    }

    private record ArgumentParseResult(@NotNull ArgumentParseResultType type, FunctionReference.Argument<Expression<?>>[] parsed) {
    }

    private static enum ArgumentParseResultType {
        OK,
        PARSE_FAIL,
        LIST_ERROR;

    }
}

