/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.primitives.Booleans
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.command.Argument;
import ch.njol.skript.command.Commands;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.DefaultExpressionUtils;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.LiteralString;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.function.ExprFunctionCall;
import ch.njol.skript.lang.function.FunctionReference;
import ch.njol.skript.lang.parser.DefaultValueData;
import ch.njol.skript.lang.parser.ExpressionParseCache;
import ch.njol.skript.lang.parser.ParseStackOverflowException;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.parser.ParsingStack;
import ch.njol.skript.lang.simplification.Simplifiable;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.patterns.MalformedPatternException;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.patterns.SkriptPattern;
import ch.njol.skript.patterns.TypePatternElement;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.CheckedIterator;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Booleans;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.common.function.FunctionReferenceParser;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.experiment.ExperimentSet;
import org.skriptlang.skript.lang.experiment.ExperimentalSyntax;
import org.skriptlang.skript.lang.script.ScriptWarning;
import org.skriptlang.skript.log.runtime.RuntimeErrorCatcher;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class SkriptParser {
    private final String expr;
    public static final int PARSE_EXPRESSIONS = 1;
    public static final int PARSE_LITERALS = 2;
    public static final int ALL_FLAGS = 3;
    private final int flags;
    public final boolean doSimplification = SkriptConfig.simplifySyntaxesOnParse.value();
    public final ParseContext context;
    public static final String WILDCARD = "[^\"]*?(?:\"[^\"]*?\"[^\"]*?)*?";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("((the )?var(iable)? )?\\{.+\\}", 2);
    private static final String INVALID_LSPEC_CHARS = "[^,():/\"\\[\\]}{]";
    private static final Pattern LITERAL_SPECIFICATION_PATTERN = Pattern.compile("(?<literal>[^,():/\"\\[\\]}{]+) \\((?<classinfo>[\\w\\p{L} ]+)\\)");
    public static final Pattern LIST_SPLIT_PATTERN = Pattern.compile("\\s*,?\\s+(and|n?or)\\s+|\\s*,\\s*", 2);
    public static final Pattern OR_PATTERN = Pattern.compile("\\sor\\s", 2);
    private static final String MULTIPLE_AND_OR = "List has multiple 'and' or 'or', will default to 'and'. Use brackets if you want to define multiple lists.";
    private static final String MISSING_AND_OR = "List is missing 'and' or 'or', defaulting to 'and'";
    private final boolean suppressMissingAndOrWarnings = SkriptConfig.disableMissingAndOrWarnings.value();
    private static final Map<String, SkriptPattern> patterns = new ConcurrentHashMap<String, SkriptPattern>();
    private static final Message M_QUOTES_ERROR = new Message("skript.quotes error");
    private static final Message M_BRACKETS_ERROR = new Message("skript.brackets error");

    public SkriptParser(String expr) {
        this(expr, 3);
    }

    public SkriptParser(String expr, int flags) {
        this(expr, flags, ParseContext.DEFAULT);
    }

    public SkriptParser(String expr, int flags, ParseContext context) {
        assert (expr != null);
        assert ((flags & 3) != 0);
        this.expr = expr.trim();
        this.flags = flags;
        this.context = context;
    }

    public SkriptParser(SkriptParser other, String expr) {
        this(expr, other.flags, other.context);
    }

    @Nullable
    public static <T> Literal<? extends T> parseLiteral(String expr, Class<T> expectedClass, ParseContext context) {
        if (((String)(expr = ((String)expr).trim())).isEmpty()) {
            return null;
        }
        return new UnparsedLiteral((String)expr).getConvertedExpression(context, expectedClass);
    }

    @Nullable
    public static <T extends SyntaxElement> T parse(String expr, Iterator<? extends SyntaxInfo<T>> source, @Nullable String defaultError) {
        if (((String)(expr = ((String)expr).trim())).isEmpty()) {
            Skript.error(defaultError);
            return null;
        }
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            T element = new SkriptParser((String)expr).parse(source);
            if (element != null) {
                log.printLog();
                T t = element;
                return t;
            }
            log.printError(defaultError);
            T t = null;
            return t;
        }
    }

    @Nullable
    public static <T extends SyntaxElement> T parseStatic(String expr, Iterator<? extends SyntaxInfo<? extends T>> source, @Nullable String defaultError) {
        return SkriptParser.parseStatic(expr, source, ParseContext.DEFAULT, defaultError);
    }

    @Nullable
    public static <T extends SyntaxElement> T parseStatic(String expr, Iterator<? extends SyntaxInfo<? extends T>> source, ParseContext parseContext, @Nullable String defaultError) {
        if ((expr = expr.trim()).isEmpty()) {
            Skript.error(defaultError);
            return null;
        }
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            T element = new SkriptParser(expr, 2, parseContext).parse(source);
            if (element != null) {
                log.printLog();
                T t = element;
                return t;
            }
            log.printError(defaultError);
            T t = null;
            return t;
        }
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private <T extends SyntaxElement> T parse(Iterator<? extends SyntaxInfo<? extends T>> source) {
        block25: {
            parsingStack = SkriptParser.getParser().getParsingStack();
            log = SkriptLogger.startParseLogHandler();
            while (true) {
                if (!source.hasNext()) {
                    log.printError();
                    var4_4 = null;
                    return var4_4;
                }
                break block25;
                break;
            }
            finally {
                if (log != null) {
                    log.close();
                }
            }
        }
        info = source.next();
        matchedPattern = -1;
        var6_8 = info.patterns().iterator();
        while (true) {
            if (!var6_8.hasNext()) ** continue;
            pattern = (String)var6_8.next();
            ++matchedPattern;
            log.clear();
            try {
                parsingStack.push(new ParsingStack.Element(info, matchedPattern));
                parseResult = this.parse_i(pattern);
            }
            catch (MalformedPatternException e) {
                message = "pattern compiling exception, element class: " + info.type().getName();
                try {
                    providingPlugin = JavaPlugin.getProvidingPlugin(info.type());
                    message = message + " (provided by " + providingPlugin.getName() + ")";
                    throw new RuntimeException(message, e);
                }
                catch (IllegalArgumentException | IllegalStateException providingPlugin) {
                    // empty catch block
                }
                throw new RuntimeException(message, e);
            }
            catch (StackOverflowError e) {
                throw new ParseStackOverflowException(e, new ParsingStack(parsingStack));
            }
            finally {
                stackElement = parsingStack.pop();
                if (!SkriptParser.$assertionsDisabled) {
                    if (stackElement.syntaxElementInfo() != info) throw new AssertionError();
                    if (stackElement.patternIndex() != matchedPattern) {
                        throw new AssertionError();
                    }
                }
            }
            if (parseResult == null) ** continue;
            if (!SkriptParser.$assertionsDisabled && parseResult.source == null) {
                throw new AssertionError();
            }
            types = null;
            for (i = 0; i < parseResult.exprs.length; ++i) {
                if (parseResult.exprs[i] != null) continue;
                if (types == null) {
                    types = parseResult.source.getElements(TypePatternElement.class);
                }
                exprInfo = ((TypePatternElement)types.get(i)).getExprInfo();
                if (exprInfo.isOptional) continue;
                exprs = SkriptParser.getDefaultExpressions(exprInfo, pattern);
                matchedExpr = null;
                for (DefaultExpression var15_29 : exprs) {
                    if (!var15_29.init()) continue;
                    matchedExpr = var15_29;
                    break;
                }
                if (matchedExpr == null) ** continue;
                parseResult.exprs[i] = matchedExpr;
            }
            element = info.instance();
            if (!SkriptParser.checkRestrictedEvents(element, parseResult) || !SkriptParser.checkExperimentalSyntax(element) || !(success = element.preInit() != false && element.init(parseResult.exprs, matchedPattern, SkriptParser.getParser().getHasDelayBefore(), parseResult) != false)) ** continue;
            exprs = parseResult.exprs;
            var13_25 = exprs.length;
            for (var14_27 = 0; !(var14_27 >= var13_25 || (var15_30 = exprs[var14_27]) instanceof UnparsedLiteral && (unparsedLiteral = (UnparsedLiteral)var15_30).multipleWarning()); ++var14_27) {
            }
            log.printLog();
            if (!this.doSimplification || !(element instanceof Simplifiable) || (element = this.simplify(simplifiable = (Simplifiable)element)) != null) break;
        }
        var12_21 = element;
        return var12_21;
    }

    @Nullable
    private <T extends SyntaxElement> T simplify(@NotNull Simplifiable<T> element) {
        try (RuntimeErrorCatcher catcher = new RuntimeErrorCatcher().start();){
            T simplified = element.simplify();
            AtomicBoolean error = new AtomicBoolean(false);
            catcher.getCachedErrors().stream().filter(err -> err.level() == Level.SEVERE).findFirst().ifPresent(err -> {
                Skript.error(err.error());
                error.set(true);
            });
            catcher.getCachedErrors().stream().filter(err -> err.level() == Level.WARNING).findFirst().ifPresent(warning -> Skript.warning(warning.error()));
            if (error.get()) {
                T t = null;
                return t;
            }
            T t = simplified;
            return t;
        }
    }

    private static boolean checkRestrictedEvents(SyntaxElement element, ParseResult parseResult) {
        if (element instanceof EventRestrictedSyntax) {
            EventRestrictedSyntax eventRestrictedSyntax = (EventRestrictedSyntax)((Object)element);
            Class<? extends Event>[] supportedEvents = eventRestrictedSyntax.supportedEvents();
            if (!SkriptParser.getParser().isCurrentEvent(supportedEvents)) {
                Skript.error("'" + parseResult.expr + "' can only be used in " + SkriptParser.supportedEventsNames(supportedEvents));
                return false;
            }
        }
        return true;
    }

    @NotNull
    private static String supportedEventsNames(Class<? extends Event>[] supportedEvents) {
        ArrayList<String> names = new ArrayList<String>();
        for (SkriptEventInfo<?> eventInfo : Skript.getEvents()) {
            for (Class<? extends Event> eventClass : supportedEvents) {
                for (Class<? extends Event> event : eventInfo.events) {
                    if (!event.isAssignableFrom(eventClass)) continue;
                    names.add("the %s event".formatted(eventInfo.getName().toLowerCase()));
                }
            }
        }
        return StringUtils.join(names, ", ", " or ");
    }

    private static <T extends SyntaxElement> boolean checkExperimentalSyntax(T element) {
        if (!(element instanceof ExperimentalSyntax)) {
            return true;
        }
        ExperimentalSyntax experimentalSyntax = (ExperimentalSyntax)element;
        ExperimentSet experiments = SkriptParser.getParser().getExperimentSet();
        return experimentalSyntax.isSatisfiedBy(experiments);
    }

    @NotNull
    private static DefaultExpression<?> getDefaultExpression(ExprInfo exprInfo, String pattern) {
        DefaultExpressionUtils.DefaultExpressionError errorType;
        ClassInfo<?> classInfo;
        DefaultValueData data = SkriptParser.getParser().getData(DefaultValueData.class);
        DefaultExpression<?> expr = data.getDefaultValue((classInfo = exprInfo.classes[0]).getC());
        if (expr == null) {
            expr = classInfo.getDefaultExpression();
        }
        if ((errorType = DefaultExpressionUtils.isValid(expr, exprInfo, 0)) == null) {
            assert (expr != null);
            return expr;
        }
        throw new SkriptAPIException(errorType.getError(List.of(classInfo.getCodeName()), pattern));
    }

    @NotNull
    static List<DefaultExpression<?>> getDefaultExpressions(ExprInfo exprInfo, String pattern) {
        if (exprInfo.classes.length == 1) {
            return new ArrayList(List.of(SkriptParser.getDefaultExpression(exprInfo, pattern)));
        }
        DefaultValueData data = SkriptParser.getParser().getData(DefaultValueData.class);
        EnumMap<DefaultExpressionUtils.DefaultExpressionError, List> failed = new EnumMap<DefaultExpressionUtils.DefaultExpressionError, List>(DefaultExpressionUtils.DefaultExpressionError.class);
        ArrayList passed = new ArrayList();
        for (int i = 0; i < exprInfo.classes.length; ++i) {
            ClassInfo<?> classInfo = exprInfo.classes[i];
            DefaultExpression<?> expr = data.getDefaultValue(classInfo.getC());
            if (expr == null) {
                expr = classInfo.getDefaultExpression();
            }
            String codeName = classInfo.getCodeName();
            DefaultExpressionUtils.DefaultExpressionError errorType = DefaultExpressionUtils.isValid(expr, exprInfo, i);
            if (errorType != null) {
                failed.computeIfAbsent(errorType, list -> new ArrayList()).add(codeName);
                continue;
            }
            passed.add(expr);
        }
        if (!passed.isEmpty()) {
            return passed;
        }
        ArrayList<String> errors = new ArrayList<String>();
        for (Map.Entry entry : failed.entrySet()) {
            String error = entry.getKey().getError((List)entry.getValue(), pattern);
            errors.add(error);
        }
        throw new SkriptAPIException(StringUtils.join(errors, "\n"));
    }

    @Nullable
    private static <T> Variable<T> parseVariable(String expr, Class<? extends T>[] returnTypes) {
        if (VARIABLE_PATTERN.matcher(expr).matches()) {
            String variableName = expr.substring(expr.indexOf(123) + 1, expr.lastIndexOf(125));
            boolean inExpression = false;
            int variableDepth = 0;
            for (char character : variableName.toCharArray()) {
                if (character == '%' && variableDepth == 0) {
                    boolean bl = inExpression = !inExpression;
                }
                if (inExpression) {
                    if (character == '{') {
                        ++variableDepth;
                    } else if (character == '}') {
                        --variableDepth;
                    }
                }
                if (inExpression || character != '{' && character != '}') continue;
                return null;
            }
            return Variable.newInstance(variableName, returnTypes);
        }
        return null;
    }

    @Nullable
    private static Expression<?> parseExpression(Class<?>[] types, String expr) {
        if (expr.startsWith("\u201c") || expr.startsWith("\u201d") || expr.endsWith("\u201d") || expr.endsWith("\u201c")) {
            Skript.error("Pretty quotes are not allowed, change to regular quotes (\")");
            return null;
        }
        if (expr.startsWith("\"") && expr.length() != 1 && SkriptParser.nextQuote(expr, 1) == expr.length() - 1) {
            return VariableString.newInstance(expr.substring(1, expr.length() - 1));
        }
        CheckedIterator<DefaultSyntaxInfos.Expression> iterator = new CheckedIterator<DefaultSyntaxInfos.Expression>(Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.EXPRESSION).iterator(), info -> {
            if (info == null || info.returnType() == Object.class) {
                return true;
            }
            for (Class returnType : types) {
                assert (returnType != null);
                if (!Converters.converterExists(info.returnType(), returnType)) continue;
                return true;
            }
            return false;
        });
        return (Expression)SkriptParser.parse(expr, iterator, null);
    }

    @Nullable
    private <T> Expression<? extends T> parseSingleExpr(boolean allowUnparsedLiteral, @Nullable LogEntry error, Class<? extends T> ... types) {
        assert (types.length > 0);
        assert (types.length == 1 || !CollectionUtils.contains(types, Object.class));
        if (this.expr.isEmpty()) {
            return null;
        }
        if (this.context != ParseContext.COMMAND && this.context != ParseContext.PARSE && this.expr.startsWith("(") && this.expr.endsWith(")") && SkriptParser.next(this.expr, 0, this.context) == this.expr.length()) {
            return new SkriptParser(this, this.expr.substring(1, this.expr.length() - 1)).parseSingleExpr(allowUnparsedLiteral, error, types);
        }
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            Expression<? extends T> expression;
            if (this.context == ParseContext.DEFAULT || this.context == ParseContext.EVENT) {
                Variable<? extends T> parsedVariable = SkriptParser.parseVariable(this.expr, types);
                if (parsedVariable != null) {
                    if ((this.flags & 1) == 0) {
                        Skript.error("Variables cannot be used here.");
                        log.printError();
                        Expression<? extends T> expression2 = null;
                        return expression2;
                    }
                    log.printLog();
                    Variable<? extends T> variable = parsedVariable;
                    return variable;
                }
                if (log.hasError()) {
                    log.printError();
                    Expression<? extends T> expression3 = null;
                    return expression3;
                }
                org.skriptlang.skript.common.function.FunctionReference<T> functionReference = this.parseFunctionReference();
                if (functionReference != null) {
                    log.printLog();
                    ExprFunctionCall<? extends T> exprFunctionCall = new ExprFunctionCall<T>(functionReference, types);
                    return exprFunctionCall;
                }
                if (log.hasError()) {
                    log.printError();
                    Expression<? extends T> expression4 = null;
                    return expression4;
                }
            }
            log.clear();
            if ((this.flags & 1) != 0) {
                Expression<?> parsedExpression = SkriptParser.parseExpression(types, this.expr);
                if (parsedExpression != null) {
                    Class<?> parsedReturnType = parsedExpression.getReturnType();
                    for (Class<T> clazz : types) {
                        if (!clazz.isAssignableFrom(parsedReturnType)) continue;
                        log.printLog();
                        Expression<?> expression5 = parsedExpression;
                        return expression5;
                    }
                    Class<? extends T>[] objTypes = types;
                    Expression<? extends T> convertedExpression = parsedExpression.getConvertedExpression(objTypes);
                    if (convertedExpression != null) {
                        log.printLog();
                        Expression<? extends T> expression6 = convertedExpression;
                        return expression6;
                    }
                    log.printError(parsedExpression.toString(null, false) + " " + Language.get("is") + " " + SkriptParser.notOfType(types), ErrorQuality.NOT_AN_EXPRESSION);
                    Expression<? extends T> expression7 = null;
                    return expression7;
                }
                log.clear();
            }
            if ((this.flags & 2) == 0) {
                log.printError();
                expression = null;
                return expression;
            }
            expression = this.parseAsLiteral(allowUnparsedLiteral, log, error, types);
            return expression;
        }
    }

    @Nullable
    private Expression<?> parseSingleExpr(boolean allowUnparsedLiteral, @Nullable LogEntry error, ExprInfo exprInfo) {
        if (this.expr.isEmpty()) {
            return null;
        }
        if (this.context != ParseContext.COMMAND && this.context != ParseContext.PARSE && this.expr.startsWith("(") && this.expr.endsWith(")") && SkriptParser.next(this.expr, 0, this.context) == this.expr.length()) {
            return new SkriptParser(this, this.expr.substring(1, this.expr.length() - 1)).parseSingleExpr(allowUnparsedLiteral, error, exprInfo);
        }
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            Expression expression;
            boolean onlySingular;
            Class[] types = new Class[exprInfo.classes.length];
            boolean hasSingular = false;
            boolean hasPlural = false;
            Class[] nonNullTypes = new Class[exprInfo.classes.length];
            int nonNullIndex = 0;
            for (int i = 0; i < types.length; ++i) {
                if ((this.flags & exprInfo.flagMask) == 0) continue;
                if (exprInfo.isPlural[i]) {
                    hasPlural = true;
                } else {
                    hasSingular = true;
                }
                types[i] = exprInfo.classes[i].getC();
                nonNullTypes[nonNullIndex] = types[i];
                ++nonNullIndex;
            }
            boolean onlyPlural = !hasSingular && hasPlural;
            boolean bl = onlySingular = hasSingular && !hasPlural;
            if (this.context == ParseContext.DEFAULT || this.context == ParseContext.EVENT) {
                if (onlySingular || onlyPlural) {
                    parsedVariable = SkriptParser.parseVariable(this.expr, nonNullTypes);
                    if (parsedVariable != null) {
                        if ((this.flags & 1) == 0) {
                            Skript.error("Variables cannot be used here.");
                            log.printError();
                            Expression<?> expression2 = null;
                            return expression2;
                        }
                        if (hasSingular && !parsedVariable.isSingle()) {
                            Skript.error("'" + this.expr + "' can only be a single " + Classes.toString(Stream.of(exprInfo.classes).map(classInfo -> classInfo.getName().toString()).toArray(), false) + ", not more.");
                            log.printError();
                            Expression<?> expression3 = null;
                            return expression3;
                        }
                        log.printLog();
                        Variable variable = parsedVariable;
                        return variable;
                    }
                    if (log.hasError()) {
                        log.printError();
                        Expression<?> expression4 = null;
                        return expression4;
                    }
                } else {
                    parsedVariable = SkriptParser.parseVariable(this.expr, types);
                    if (parsedVariable != null) {
                        if ((this.flags & 1) == 0) {
                            Skript.error("Variables cannot be used here.");
                            log.printError();
                            Expression<?> expression5 = null;
                            return expression5;
                        }
                        if ((exprInfo.classes.length == 1 && !exprInfo.isPlural[0] || Booleans.contains((boolean[])exprInfo.isPlural, (boolean)true)) && !parsedVariable.isSingle()) {
                            Skript.error("'" + this.expr + "' can only be a single " + Classes.toString(Stream.of(exprInfo.classes).map(classInfo -> classInfo.getName().toString()).toArray(), false) + ", not more.");
                            log.printError();
                            Expression<?> expression6 = null;
                            return expression6;
                        }
                        log.printLog();
                        Variable variable = parsedVariable;
                        return variable;
                    }
                    if (log.hasError()) {
                        log.printError();
                        Expression<?> expression7 = null;
                        return expression7;
                    }
                }
                org.skriptlang.skript.common.function.FunctionReference functionReference = this.parseFunctionReference();
                if (functionReference != null) {
                    if (onlySingular && !functionReference.isSingle()) {
                        Skript.error("'" + this.expr + "' can only be a single " + Classes.toString(Stream.of(exprInfo.classes).map(classInfo -> classInfo.getName().toString()).toArray(), false) + ", not more.");
                        log.printError();
                        Expression<?> expression8 = null;
                        return expression8;
                    }
                    log.printLog();
                    ExprFunctionCall exprFunctionCall = new ExprFunctionCall(functionReference, types);
                    return exprFunctionCall;
                }
                if (log.hasError()) {
                    log.printError();
                    Expression<?> expression9 = null;
                    return expression9;
                }
            }
            log.clear();
            if ((this.flags & 1) != 0) {
                Expression<?> parsedExpression = SkriptParser.parseExpression(types, this.expr);
                if (parsedExpression != null) {
                    Expression expression10;
                    Class<?> parsedReturnType = parsedExpression.getReturnType();
                    for (int i = 0; i < types.length; ++i) {
                        Class type = types[i];
                        if (type == null || !type.isAssignableFrom(parsedReturnType)) continue;
                        if (!exprInfo.isPlural[i] && !parsedExpression.isSingle()) {
                            if (this.context == ParseContext.COMMAND) {
                                Skript.error(Commands.m_too_many_arguments.toString(exprInfo.classes[i].getName().getIndefiniteArticle(), exprInfo.classes[i].getName().toString()), ErrorQuality.SEMANTIC_ERROR);
                            } else {
                                Skript.error("'" + this.expr + "' can only be a single " + Classes.toString(Stream.of(exprInfo.classes).map(classInfo -> classInfo.getName().toString()).toArray(), false) + ", not more.");
                            }
                            log.printError();
                            Expression<?> expression11 = null;
                            return expression11;
                        }
                        log.printLog();
                        Expression<?> expression12 = parsedExpression;
                        return expression12;
                    }
                    if (onlySingular && !parsedExpression.isSingle()) {
                        Skript.error("'" + this.expr + "' can only be a single " + Classes.toString(Stream.of(exprInfo.classes).map(classInfo -> classInfo.getName().toString()).toArray(), false) + ", not more.");
                        log.printError();
                        Expression<?> i = null;
                        return i;
                    }
                    Expression convertedExpression = parsedExpression.getConvertedExpression(types);
                    if (convertedExpression != null) {
                        log.printLog();
                        expression10 = convertedExpression;
                        return expression10;
                    }
                    log.printError(parsedExpression.toString(null, false) + " " + Language.get("is") + " " + SkriptParser.notOfType(types), ErrorQuality.NOT_AN_EXPRESSION);
                    expression10 = null;
                    return expression10;
                }
                log.clear();
            }
            if ((this.flags & 2) == 0) {
                log.printError();
                expression = null;
                return expression;
            }
            expression = this.parseAsLiteral(allowUnparsedLiteral, log, error, nonNullTypes);
            return expression;
        }
    }

    @SafeVarargs
    @Nullable
    private <T> Expression<? extends T> parseAsLiteral(boolean allowUnparsedLiteral, ParseLogHandler log, @Nullable LogEntry error, Class<? extends T> ... types) {
        String unparsedClassInfo;
        String literalString;
        Expression<? extends T> result;
        Matcher classInfoMatcher;
        if (this.expr.endsWith(")") && this.expr.contains("(") && (classInfoMatcher = LITERAL_SPECIFICATION_PATTERN.matcher(this.expr)).matches() && (result = this.parseSpecifiedLiteral(literalString = classInfoMatcher.group("literal"), unparsedClassInfo = Noun.stripDefiniteArticle(classInfoMatcher.group("classinfo")), types)) != null) {
            log.printLog();
            return result;
        }
        if (types.length == 1 && types[0] == Object.class) {
            if (!allowUnparsedLiteral) {
                log.printError();
                return null;
            }
            return this.getUnparsedLiteral(log, error);
        }
        boolean containsObjectClass = false;
        for (Class<? extends T> clazz : types) {
            log.clear();
            if (clazz == Object.class) {
                containsObjectClass = true;
                continue;
            }
            T parsedObject = Classes.parse(this.expr, clazz, this.context);
            if (parsedObject == null) continue;
            log.printLog();
            return new SimpleLiteral<T>(parsedObject, false, new UnparsedLiteral(this.expr));
        }
        if (allowUnparsedLiteral && containsObjectClass) {
            return this.getUnparsedLiteral(log, error);
        }
        if (this.expr.startsWith("\"") && this.expr.endsWith("\"") && this.expr.length() > 1) {
            for (Class<T> clazz : types) {
                if (!clazz.isAssignableFrom(String.class)) continue;
                VariableString string = VariableString.newInstance(this.expr.substring(1, this.expr.length() - 1));
                if (!(string instanceof LiteralString)) break;
                return string;
            }
        }
        log.printError();
        return null;
    }

    @Nullable
    private UnparsedLiteral getUnparsedLiteral(ParseLogHandler log, @Nullable LogEntry error) {
        if (Classes.parseSimple(this.expr, Object.class, this.context) == null) {
            log.printError();
            return null;
        }
        log.clear();
        LogEntry logError = log.getError();
        return new UnparsedLiteral(this.expr, logError != null && (error == null || logError.quality > error.quality) ? logError : error);
    }

    @SafeVarargs
    @Nullable
    private <T> Expression<? extends T> parseSpecifiedLiteral(String literalString, String unparsedClassInfo, Class<? extends T> ... types) {
        ClassInfo classInfo = Classes.parse(unparsedClassInfo, ClassInfo.class, this.context);
        if (classInfo == null) {
            Skript.error("A " + unparsedClassInfo + " is not a valid type.");
            return null;
        }
        Parser classInfoParser = classInfo.getParser();
        if (classInfoParser == null || !classInfoParser.canParse(this.context)) {
            Skript.error("A " + unparsedClassInfo + " cannot be parsed.");
            return null;
        }
        if (!this.checkAcceptedType(classInfo.getC(), types)) {
            Skript.error(this.expr + " " + Language.get("is") + " " + SkriptParser.notOfType(types));
            return null;
        }
        Object parsedObject = classInfoParser.parse(literalString, this.context);
        if (parsedObject != null) {
            return new SimpleLiteral(parsedObject, false, new UnparsedLiteral(literalString));
        }
        return null;
    }

    private boolean checkAcceptedType(Class<?> clazz, Class<?> ... types) {
        for (Class<?> targetType : types) {
            if (!targetType.isAssignableFrom(clazz)) continue;
            return true;
        }
        return false;
    }

    @SafeVarargs
    @Nullable
    public final <T> Expression<? extends T> parseExpression(Class<? extends T> ... types) {
        if (this.expr.isEmpty()) {
            return null;
        }
        assert (types.length > 0);
        assert (types.length == 1 || !CollectionUtils.contains(types, Object.class));
        ExpressionParseCache failedExprsCache = ParserInstance.get().getExpressionParseCache();
        failedExprsCache.push();
        try {
            Expression<? extends T> expression;
            block16: {
                ParseLogHandler log;
                block14: {
                    Expression<? extends T> expression2;
                    block15: {
                        log = SkriptLogger.startParseLogHandler();
                        try {
                            Expression<? extends T> parsedExpression = this.parseSingleExpr(true, null, types);
                            if (parsedExpression == null) break block14;
                            log.printLog();
                            expression2 = parsedExpression;
                            if (log == null) break block15;
                        }
                        catch (Throwable throwable) {
                            if (log != null) {
                                try {
                                    log.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        log.close();
                    }
                    return expression2;
                }
                log.clear();
                expression = this.parseExpressionList(log, types);
                if (log == null) break block16;
                log.close();
            }
            return expression;
        }
        finally {
            failedExprsCache.pop();
        }
    }

    @Nullable
    public Expression<?> parseExpression(ExprInfo exprInfo) {
        if (this.expr.isEmpty()) {
            return null;
        }
        ExpressionParseCache failedExprsCache = ParserInstance.get().getExpressionParseCache();
        failedExprsCache.push();
        try {
            Expression<?> expression;
            block14: {
                ParseLogHandler log;
                block12: {
                    Expression<?> expression2;
                    block13: {
                        log = SkriptLogger.startParseLogHandler();
                        try {
                            Expression<?> parsedExpression = this.parseSingleExpr(true, null, exprInfo);
                            if (parsedExpression == null) break block12;
                            log.printLog();
                            expression2 = parsedExpression;
                            if (log == null) break block13;
                        }
                        catch (Throwable throwable) {
                            if (log != null) {
                                try {
                                    log.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        log.close();
                    }
                    return expression2;
                }
                log.clear();
                expression = this.parseExpressionList(log, exprInfo);
                if (log == null) break block14;
                log.close();
            }
            return expression;
        }
        finally {
            failedExprsCache.pop();
        }
    }

    @SafeVarargs
    @Nullable
    private <T> Expression<? extends T> parseExpressionList(ParseLogHandler log, Class<? extends T> ... types) {
        return this.parseExpressionList_i(log, types);
    }

    @Nullable
    private Expression<?> parseExpressionList(ParseLogHandler log, ExprInfo info) {
        return this.parseExpressionList_i(log, info);
    }

    @Nullable
    private Expression<?> parseExpressionList(ParseLogHandler log, OrderedExprInfo info) {
        return this.parseExpressionList_i(log, info);
    }

    @Nullable
    private Expression<?> parseExpressionList_i(ParseLogHandler log, Object data) {
        ParserInstance parser;
        Class<Object> superReturnType;
        Class[] returnTypes;
        int currentPosition;
        Class[] types;
        ExprInfo info;
        OrderedExprInfo info2;
        OrderedExprInfo orderedExprInfo = data instanceof OrderedExprInfo ? (info2 = (OrderedExprInfo)data) : null;
        ExprInfo exprInfo = data instanceof ExprInfo ? (info = (ExprInfo)data) : null;
        Class[] classArray = types = orderedExprInfo == null && exprInfo == null ? (Class[])data : null;
        boolean isObject = orderedExprInfo != null ? orderedExprInfo.infos.length == 1 && orderedExprInfo.infos[0].classes[0].getC() == Object.class : (exprInfo != null ? exprInfo.classes.length == 1 && exprInfo.classes[0].getC() == Object.class : types.length == 1 && types[0] == Object.class);
        ArrayList<int[]> pieces = new ArrayList<int[]>();
        Matcher matcher = LIST_SPLIT_PATTERN.matcher(this.expr);
        int lastPosition = currentPosition = 0;
        while (currentPosition >= 0 && currentPosition <= this.expr.length()) {
            if (currentPosition == this.expr.length() || matcher.region(currentPosition, this.expr.length()).lookingAt()) {
                pieces.add(new int[]{lastPosition, currentPosition});
                if (currentPosition == this.expr.length()) break;
                lastPosition = currentPosition = matcher.end();
            }
            currentPosition = SkriptParser.next(this.expr, currentPosition, this.context);
        }
        if (currentPosition != this.expr.length()) {
            assert (currentPosition == -1 && this.context != ParseContext.COMMAND && this.context != ParseContext.PARSE) : currentPosition + "; " + this.expr;
            log.printError("Invalid brackets/variables/text in '" + this.expr + "'", ErrorQuality.NOT_AN_EXPRESSION);
            return null;
        }
        if (pieces.size() == 1) {
            if (this.expr.startsWith("(") && this.expr.endsWith(")") && SkriptParser.next(this.expr, 0, this.context) == this.expr.length()) {
                log.clear();
                SkriptParser parser2 = new SkriptParser(this, this.expr.substring(1, this.expr.length() - 1));
                if (exprInfo != null) {
                    return parser2.parseExpression(exprInfo);
                }
                return parser2.parseExpression(types);
            }
            if (isObject && (this.flags & 2) != 0) {
                log.clear();
                return new UnparsedLiteral(this.expr, log.getError());
            }
            log.printError();
            return null;
        }
        if (exprInfo != null && !Booleans.contains((boolean[])exprInfo.isPlural, (boolean)true) && !OR_PATTERN.matcher(this.expr).find()) {
            log.printError();
            return null;
        }
        ArrayList parsedExpressions = new ArrayList();
        boolean isLiteralList = true;
        Kleenean and = Kleenean.UNKNOWN;
        int first = 0;
        block1: while (first < pieces.size()) {
            for (int last = first; last < pieces.size(); ++last) {
                String delimiter;
                Expression<Object> parsedExpression;
                if (first == 0 && last == pieces.size() - 1) continue;
                int start = ((int[])pieces.get(first))[0];
                int end = ((int[])pieces.get(last))[1];
                String string = this.expr.substring(start, end);
                SkriptParser parser3 = new SkriptParser(this, string);
                if (string.startsWith("(") && string.endsWith(")") && SkriptParser.next(string, 0, this.context) == string.length()) {
                    if (orderedExprInfo != null) {
                        infoIndex = parsedExpressions.size();
                        if (infoIndex >= orderedExprInfo.infos.length) {
                            log.printError();
                            return null;
                        }
                        parsedExpression = parser3.parseExpression(orderedExprInfo.infos[infoIndex]);
                    } else {
                        parsedExpression = exprInfo != null ? parser3.parseExpression(exprInfo) : parser3.parseExpression(types);
                    }
                } else if (orderedExprInfo != null) {
                    infoIndex = parsedExpressions.size();
                    if (infoIndex >= orderedExprInfo.infos.length) {
                        log.printError();
                        return null;
                    }
                    parsedExpression = parser3.parseSingleExpr(last == first, log.getError(), orderedExprInfo.infos[infoIndex]);
                } else {
                    parsedExpression = exprInfo != null ? parser3.parseSingleExpr(last == first, log.getError(), exprInfo) : parser3.parseSingleExpr(last == first, log.getError(), types);
                }
                if (parsedExpression == null) continue;
                isLiteralList &= parsedExpression instanceof Literal;
                parsedExpressions.add(parsedExpression);
                if (first != 0 && !(delimiter = this.expr.substring(((int[])pieces.get(first - 1))[1], start).trim().toLowerCase(Locale.ENGLISH)).equals(",")) {
                    boolean or;
                    boolean bl = or = !delimiter.endsWith("nor") && delimiter.endsWith("or");
                    if (and.isUnknown()) {
                        and = Kleenean.get(!or);
                    } else if (and != Kleenean.get(!or)) {
                        Skript.warning("List has multiple 'and' or 'or', will default to 'and'. Use brackets if you want to define multiple lists. List: " + this.expr);
                        and = Kleenean.TRUE;
                    }
                }
                first = last + 1;
                continue block1;
            }
            log.printError();
            return null;
        }
        if (parsedExpressions.size() == 1) {
            returnTypes = null;
            superReturnType = ((Expression)parsedExpressions.get(0)).getReturnType();
        } else {
            returnTypes = new Class[parsedExpressions.size()];
            for (int i = 0; i < parsedExpressions.size(); ++i) {
                returnTypes[i] = ((Expression)parsedExpressions.get(i)).getReturnType();
            }
            superReturnType = Classes.getSuperClassInfo(returnTypes).getC();
        }
        if (exprInfo != null && !and.isFalse()) {
            boolean canBePlural = false;
            for (int typeIndex = 0; typeIndex < exprInfo.classes.length; ++typeIndex) {
                if (!exprInfo.isPlural[typeIndex] || !exprInfo.classes[typeIndex].getC().isAssignableFrom(superReturnType)) continue;
                canBePlural = true;
                break;
            }
            if (!canBePlural) {
                for (Expression expression : parsedExpressions) {
                    canBePlural = false;
                    for (int typeIndex = 0; typeIndex < exprInfo.classes.length; ++typeIndex) {
                        if (!exprInfo.isPlural[typeIndex] || !expression.canReturn(exprInfo.classes[typeIndex].getC())) continue;
                        canBePlural = true;
                        break;
                    }
                    if (canBePlural) continue;
                    break;
                }
            }
            if (!canBePlural) {
                log.printError();
                return null;
            }
        }
        if (returnTypes == null) {
            return (Expression)parsedExpressions.get(0);
        }
        log.printLog(false);
        if (and.isUnknown() && !this.suppressMissingAndOrWarnings && (parser = SkriptParser.getParser()).isActive() && !parser.getCurrentScript().suppressesWarning(ScriptWarning.MISSING_CONJUNCTION)) {
            Skript.warning("List is missing 'and' or 'or', defaulting to 'and': " + this.expr);
        }
        if (isLiteralList) {
            Literal[] literals = parsedExpressions.toArray(new Literal[0]);
            return new LiteralList(literals, superReturnType, returnTypes, !and.isFalse());
        }
        Expression[] expressions = parsedExpressions.toArray(new Expression[0]);
        return new ExpressionList(expressions, superReturnType, returnTypes, !and.isFalse());
    }

    public <T> org.skriptlang.skript.common.function.FunctionReference<T> parseFunctionReference() {
        if (this.context == ParseContext.DEFAULT || this.context == ParseContext.EVENT) {
            return new FunctionReferenceParser(this.context, this.flags).parseFunctionReference(this.expr);
        }
        return null;
    }

    @Deprecated(forRemoval=true, since="2.14")
    @Nullable
    public <T> FunctionReference<T> parseFunction(Class<? extends T> ... types) {
        if (this.context != ParseContext.DEFAULT && this.context != ParseContext.EVENT) {
            return null;
        }
        org.skriptlang.skript.common.function.FunctionReference newReference = new FunctionReferenceParser(this.context, this.flags).parseFunctionReference(this.expr);
        if (newReference == null) {
            return null;
        }
        Expression[] expressions = (Expression[])Arrays.stream(newReference.arguments()).map(FunctionReference.Argument::value).toArray(Expression[]::new);
        return new FunctionReference<T>(newReference.name(), null, newReference.namespace(), types, expressions);
    }

    public static boolean parseArguments(String args, ScriptCommand command, ScriptCommandEvent event) {
        SkriptParser parser = new SkriptParser(args, 2, ParseContext.COMMAND);
        ParseResult parseResult = parser.parse_i(command.getPattern());
        if (parseResult == null) {
            return false;
        }
        List<Argument<?>> arguments = command.getArguments();
        assert (arguments.size() == parseResult.exprs.length);
        for (int i = 0; i < parseResult.exprs.length; ++i) {
            if (parseResult.exprs[i] == null) {
                arguments.get(i).setToDefault(event);
                continue;
            }
            arguments.get(i).set(event, parseResult.exprs[i].getArray(event));
        }
        return true;
    }

    @Nullable
    public static ParseResult parse(String text, String pattern) {
        return new SkriptParser(text, 2, ParseContext.COMMAND).parse_i(pattern);
    }

    @Nullable
    public static ParseResult parse(String text, String pattern, int parseFlags, ParseContext parseContext) {
        return new SkriptParser(text, parseFlags, parseContext).parse_i(pattern);
    }

    @Nullable
    public static ParseResult parse(String text, SkriptPattern pattern, int parseFlags, ParseContext parseContext) {
        return SkriptParser.parse(text, pattern.toString(), parseFlags, parseContext);
    }

    public static int nextBracket(String pattern, char closingBracket, char openingBracket, int start, boolean isGroup) throws MalformedPatternException {
        int index = 0;
        for (int i = start; i < pattern.length(); ++i) {
            if (pattern.charAt(i) == '\\') {
                ++i;
                continue;
            }
            if (pattern.charAt(i) == closingBracket) {
                if (index == 0) {
                    if (!isGroup) {
                        throw new MalformedPatternException(pattern, "Unexpected closing bracket '" + closingBracket + "'");
                    }
                    return i;
                }
                --index;
                continue;
            }
            if (pattern.charAt(i) != openingBracket) continue;
            ++index;
        }
        if (isGroup) {
            throw new MalformedPatternException(pattern, "Missing closing bracket '" + closingBracket + "'");
        }
        return -1;
    }

    private static int nextUnescaped(String pattern, char character, int from) {
        for (int i = from; i < pattern.length(); ++i) {
            if (pattern.charAt(i) == '\\') {
                ++i;
                continue;
            }
            if (pattern.charAt(i) != character) continue;
            return i;
        }
        return -1;
    }

    static int countUnescaped(String haystack, char needle) {
        return SkriptParser.countUnescaped(haystack, needle, 0, haystack.length());
    }

    static int countUnescaped(String haystack, char needle, int start, int end) {
        assert (start >= 0 && start <= end && end <= haystack.length()) : start + ", " + end + "; " + haystack.length();
        int count = 0;
        for (int i = start; i < end; ++i) {
            char character = haystack.charAt(i);
            if (character == '\\') {
                ++i;
                continue;
            }
            if (character != needle) continue;
            ++count;
        }
        return count;
    }

    private static int nextQuote(String string, int start) {
        boolean inExpression = false;
        int length = string.length();
        for (int i = start; i < length; ++i) {
            char character = string.charAt(i);
            if (character == '\"' && !inExpression) {
                if (i == length - 1 || string.charAt(i + 1) != '\"') {
                    return i;
                }
                ++i;
                continue;
            }
            if (character != '%') continue;
            inExpression = !inExpression;
        }
        return -1;
    }

    public static String notOfType(Class<?> ... types) {
        if (types.length == 1) {
            Class<?> type = types[0];
            assert (type != null);
            return Language.get("not") + " " + Classes.getSuperClassInfo(type).getName().withIndefiniteArticle();
        }
        StringBuilder message = new StringBuilder(Language.get("neither") + " ");
        for (int i = 0; i < types.length; ++i) {
            if (i != 0) {
                if (i != types.length - 1) {
                    message.append(", ");
                } else {
                    message.append(" ").append(Language.get("nor")).append(" ");
                }
            }
            Class<?> c = types[i];
            assert (c != null);
            ClassInfo<?> classInfo = Classes.getSuperClassInfo(c);
            if (classInfo != null) {
                message.append(classInfo.getName().withIndefiniteArticle());
                continue;
            }
            message.append(c.getName());
        }
        return message.toString();
    }

    public static String notOfType(ClassInfo<?> ... types) {
        if (types.length == 1) {
            return Language.get("not") + " " + types[0].getName().withIndefiniteArticle();
        }
        StringBuilder message = new StringBuilder(Language.get("neither") + " ");
        for (int i = 0; i < types.length; ++i) {
            if (i != 0) {
                if (i != types.length - 1) {
                    message.append(", ");
                } else {
                    message.append(" ").append(Language.get("nor")).append(" ");
                }
            }
            message.append(types[i].getName().withIndefiniteArticle());
        }
        return message.toString();
    }

    public static int next(String expr, int startIndex, ParseContext context) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        int exprLength = expr.length();
        if (startIndex >= exprLength) {
            return -1;
        }
        if (context == ParseContext.COMMAND || context == ParseContext.PARSE) {
            return startIndex + 1;
        }
        return switch (expr.charAt(startIndex)) {
            case '\"' -> {
                int index = SkriptParser.nextQuote(expr, startIndex + 1);
                if (index < 0) {
                    yield -1;
                }
                yield index + 1;
            }
            case '{' -> {
                int index = VariableString.nextVariableBracket(expr, startIndex + 1);
                if (index < 0) {
                    yield -1;
                }
                yield index + 1;
            }
            case '(' -> {
                int index = startIndex + 1;
                while (index >= 0 && index < exprLength) {
                    if (expr.charAt(index) == ')') {
                        yield index + 1;
                    }
                    index = SkriptParser.next(expr, index, context);
                }
                yield -1;
            }
            default -> startIndex + 1;
        };
    }

    public static int nextOccurrence(String haystack, String needle, int startIndex, ParseContext parseContext, boolean caseSensitive) {
        boolean startsWithSpecialChar;
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (parseContext == ParseContext.COMMAND || parseContext == ParseContext.PARSE) {
            return haystack.indexOf(needle, startIndex);
        }
        int haystackLength = haystack.length();
        if (startIndex >= haystackLength) {
            return -1;
        }
        int needleLength = needle.length();
        char firstChar = needle.charAt(0);
        boolean bl = startsWithSpecialChar = firstChar == '\"' || firstChar == '{' || firstChar == '(';
        while (startIndex < haystackLength) {
            char character = haystack.charAt(startIndex);
            if (startsWithSpecialChar && haystack.regionMatches(!caseSensitive, startIndex, needle, 0, needleLength)) {
                return startIndex;
            }
            switch (character) {
                case '\"': {
                    startIndex = SkriptParser.nextQuote(haystack, startIndex + 1);
                    if (startIndex >= 0) break;
                    return -1;
                }
                case '{': {
                    startIndex = VariableString.nextVariableBracket(haystack, startIndex + 1);
                    if (startIndex >= 0) break;
                    return -1;
                }
                case '(': {
                    startIndex = SkriptParser.next(haystack, startIndex, parseContext);
                    if (startIndex >= 0) break;
                    return -1;
                }
            }
            if (haystack.regionMatches(!caseSensitive, startIndex, needle, 0, needleLength)) {
                return startIndex;
            }
            ++startIndex;
        }
        return -1;
    }

    @Nullable
    private ParseResult parse_i(String pattern) {
        SkriptPattern skriptPattern = patterns.computeIfAbsent(pattern, PatternCompiler::compile);
        MatchResult matchResult = skriptPattern.match(this.expr, this.flags, this.context);
        if (matchResult == null) {
            return null;
        }
        return matchResult.toParseResult();
    }

    @Nullable
    public static NonNullPair<String, NonNullPair<ClassInfo<?>, Boolean>[]> validatePattern(String pattern) {
        ArrayList pairs = new ArrayList();
        int groupLevel = 0;
        int optionalLevel = 0;
        LinkedList<Character> groups = new LinkedList<Character>();
        StringBuilder stringBuilder = new StringBuilder(pattern.length());
        int last = 0;
        for (int i = 0; i < pattern.length(); ++i) {
            int j;
            char character = pattern.charAt(i);
            if (character == '(') {
                ++groupLevel;
                groups.addLast(Character.valueOf(character));
                continue;
            }
            if (character == '|') {
                if (groupLevel == 0 || ((Character)groups.peekLast()).charValue() != '(' && ((Character)groups.peekLast()).charValue() != '|') {
                    return SkriptParser.error("Cannot use the pipe character '|' outside of groups. Escape it if you want to match a literal pipe: '\\|'");
                }
                groups.removeLast();
                groups.addLast(Character.valueOf(character));
                continue;
            }
            if (character == ')') {
                if (groupLevel == 0 || ((Character)groups.peekLast()).charValue() != '(' && ((Character)groups.peekLast()).charValue() != '|') {
                    return SkriptParser.error("Unexpected closing group bracket ')'. Escape it if you want to match a literal bracket: '\\)'");
                }
                if (((Character)groups.peekLast()).charValue() == '(') {
                    return SkriptParser.error("(...|...) groups have to contain at least one pipe character '|' to separate it into parts. Escape the brackets if you want to match literal brackets: \"\\(not a group\\)\"");
                }
                --groupLevel;
                groups.removeLast();
                continue;
            }
            if (character == '[') {
                ++optionalLevel;
                groups.addLast(Character.valueOf(character));
                continue;
            }
            if (character == ']') {
                if (optionalLevel == 0 || ((Character)groups.peekLast()).charValue() != '[') {
                    return SkriptParser.error("Unexpected closing optional bracket ']'. Escape it if you want to match a literal bracket: '\\]'");
                }
                --optionalLevel;
                groups.removeLast();
                continue;
            }
            if (character == '<') {
                j = pattern.indexOf(62, i + 1);
                if (j == -1) {
                    return SkriptParser.error("Missing closing regex bracket '>'. Escape the '<' if you want to match a literal bracket: '\\<'");
                }
                try {
                    Pattern.compile(pattern.substring(i + 1, j));
                }
                catch (PatternSyntaxException e) {
                    return SkriptParser.error("Invalid Regular Expression '" + pattern.substring(i + 1, j) + "': " + e.getLocalizedMessage());
                }
                i = j;
                continue;
            }
            if (character == '>') {
                return SkriptParser.error("Unexpected closing regex bracket '>'. Escape it if you want to match a literal bracket: '\\>'");
            }
            if (character == '%') {
                j = pattern.indexOf(37, i + 1);
                if (j == -1) {
                    return SkriptParser.error("Missing end sign '%' of expression. Escape the percent sign to match a literal '%': '\\%'");
                }
                NonNullPair<String, Boolean> pair = Utils.getEnglishPlural(pattern.substring(i + 1, j));
                ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(pair.getFirst());
                if (classInfo == null) {
                    return SkriptParser.error("The type '" + pair.getFirst() + "' could not be found. Please check your spelling or escape the percent signs if you want to match literal %s: \"\\%not an expression\\%\"");
                }
                pairs.add(new NonNullPair(classInfo, pair.getSecond()));
                stringBuilder.append(pattern, last, i + 1);
                stringBuilder.append(Utils.toEnglishPlural(classInfo.getCodeName(), pair.getSecond()));
                last = j;
                i = j;
                continue;
            }
            if (character != '\\') continue;
            if (i == pattern.length() - 1) {
                return SkriptParser.error("Pattern must not end in an unescaped backslash. Add another backslash to escape it, or remove it altogether.");
            }
            ++i;
        }
        stringBuilder.append(pattern.substring(last));
        return new NonNullPair<String, NonNullPair<ClassInfo<?>, Boolean>[]>(stringBuilder.toString(), pairs.toArray(new NonNullPair[0]));
    }

    @Nullable
    private static NonNullPair<String, NonNullPair<ClassInfo<?>, Boolean>[]> error(String error) {
        Skript.error("Invalid pattern: " + error);
        return null;
    }

    public static boolean validateLine(String line) {
        if (StringUtils.count(line, '\"') % 2 != 0) {
            Skript.error(M_QUOTES_ERROR.toString());
            return false;
        }
        int i = 0;
        while (i < line.length()) {
            if (i == -1) {
                Skript.error(M_BRACKETS_ERROR.toString());
                return false;
            }
            i = SkriptParser.next(line, i, ParseContext.DEFAULT);
        }
        return true;
    }

    private static ParserInstance getParser() {
        return ParserInstance.get();
    }

    static {
        ParserInstance.registerData(DefaultValueData.class, DefaultValueData::new);
    }

    public static class ParseResult {
        @Nullable
        public SkriptPattern source;
        public Expression<?>[] exprs;
        public List<java.util.regex.MatchResult> regexes = new ArrayList<java.util.regex.MatchResult>(1);
        public String expr;
        public int mark = 0;
        public List<String> tags = new ArrayList<String>();

        public ParseResult(SkriptParser parser, String pattern) {
            this.expr = parser.expr;
            this.exprs = new Expression[SkriptParser.countUnescaped(pattern, '%') / 2];
        }

        public ParseResult(String expr, Expression<?>[] expressions) {
            this.expr = expr;
            this.exprs = expressions;
        }

        public boolean hasTag(String tag) {
            return this.tags.contains(tag);
        }
    }

    public static class ExprInfo {
        public final ClassInfo<?>[] classes;
        public final boolean[] isPlural;
        public boolean isOptional;
        public int flagMask = -1;
        public int time = 0;

        public ExprInfo(int length) {
            this(new ClassInfo[length], new boolean[length]);
        }

        public ExprInfo(ClassInfo<?>[] classes, boolean[] isPlural) {
            Preconditions.checkState((classes.length == isPlural.length ? 1 : 0) != 0, (Object)"classes and isPlural must be the same length");
            this.classes = classes;
            this.isPlural = isPlural;
        }
    }

    private record OrderedExprInfo(ExprInfo[] infos) {
    }
}

