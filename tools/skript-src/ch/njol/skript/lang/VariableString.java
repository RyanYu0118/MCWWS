/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralString;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.structures.StructVariables;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.skript.util.chat.MessageComponent;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.SingleItemIterator;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.elements.expressions.ExprColored;
import org.skriptlang.skript.lang.script.Script;

public class VariableString
implements Expression<String> {
    @Nullable
    private final Script script;
    protected final String original;
    private final Object[] strings;
    private final boolean isSimple;
    @Nullable
    private final String simple;
    private final StringMode mode;

    protected VariableString(String input) {
        input = input.replace("%%", "%");
        ParserInstance parser = this.getParser();
        this.script = parser.isActive() ? parser.getCurrentScript() : null;
        this.original = input;
        this.strings = null;
        this.isSimple = true;
        this.simple = input;
        this.mode = StringMode.MESSAGE;
    }

    private VariableString(String original, Object[] strings, StringMode mode) {
        ParserInstance parser = this.getParser();
        this.script = parser.isActive() ? parser.getCurrentScript() : null;
        this.original = original;
        this.strings = strings;
        this.isSimple = false;
        this.simple = null;
        this.mode = mode;
    }

    @Nullable
    public static VariableString newInstance(String input) {
        return VariableString.newInstance(input, StringMode.MESSAGE);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static VariableString newInstance(String original, StringMode mode) {
        if (mode != StringMode.VARIABLE_NAME && !VariableString.isQuotedCorrectly(original, false)) {
            return null;
        }
        int percentCount = StringUtils.count(original, '%');
        if (percentCount % 2 != 0) {
            Skript.error("The percent sign is used for expressions (e.g. %player%). To insert a '%' type it twice: %%.");
            return null;
        }
        if (mode != StringMode.VARIABLE_NAME) {
            StringBuilder stringBuilder = new StringBuilder();
            boolean expression = false;
            for (int i = 0; i < original.length(); ++i) {
                char c = original.charAt(i);
                stringBuilder.append(c);
                if (c == '%') {
                    boolean bl = expression = !expression;
                }
                if (expression || c != '\"') continue;
                ++i;
            }
            original = stringBuilder.toString();
        }
        ArrayList<Object> strings = new ArrayList<Object>(percentCount / 2 + 2);
        int exprStart = original.indexOf(37);
        if (exprStart != -1) {
            if (exprStart != 0) {
                strings.add(original.substring(0, exprStart));
            }
            while (exprStart != original.length()) {
                String literalString;
                int variableStart;
                int exprEnd = original.indexOf(37, exprStart + 1);
                int variableEnd = exprStart;
                while (exprEnd != -1 && (variableStart = original.indexOf(123, variableEnd + 1)) != -1 && variableStart < exprEnd) {
                    variableEnd = VariableString.nextVariableBracket(original, variableStart + 1);
                    if (variableEnd == -1) {
                        Skript.error("Missing closing bracket '}' to end variable");
                        return null;
                    }
                    exprEnd = original.indexOf(37, variableEnd + 1);
                }
                if (exprEnd == -1) {
                    assert (false);
                    return null;
                }
                if (exprStart + 1 == exprEnd) {
                    if (strings.size() > 0 && strings.get(strings.size() - 1) instanceof String) {
                        strings.set(strings.size() - 1, String.valueOf(strings.get(strings.size() - 1)) + "%");
                    } else {
                        strings.add("%");
                    }
                } else {
                    RetainingLogHandler log = SkriptLogger.startRetainingLog();
                    try {
                        Expression expr = new SkriptParser(original.substring(exprStart + 1, exprEnd), 1, ParseContext.DEFAULT).parseExpression(Object.class);
                        if (expr == null) {
                            log.printErrors("Can't understand this expression: " + original.substring(exprStart + 1, exprEnd));
                            VariableString variableString = null;
                            return variableString;
                        }
                        strings.add(expr);
                        log.printLog();
                    }
                    finally {
                        log.stop();
                    }
                }
                exprStart = original.indexOf(37, exprEnd + 1);
                if (exprStart == -1) {
                    exprStart = original.length();
                }
                if ((literalString = original.substring(exprEnd + 1, exprStart)).isEmpty()) continue;
                if (strings.size() > 0 && strings.get(strings.size() - 1) instanceof String) {
                    strings.set(strings.size() - 1, String.valueOf(strings.get(strings.size() - 1)) + literalString);
                    continue;
                }
                strings.add(literalString);
            }
        } else {
            strings.add(original);
        }
        if (strings.size() == 1 && strings.get(0) instanceof String) {
            return new LiteralString(original);
        }
        if (strings.size() == 1 && strings.get(0) instanceof Expression && ((Expression)strings.get(0)).getReturnType() == String.class && ((Expression)strings.get(0)).isSingle() && mode == StringMode.MESSAGE) {
            String expr = ((Expression)strings.get(0)).toString(null, false);
            Skript.warning(expr + " is already a text, so you should not put it in one (e.g. " + expr + " instead of \"%" + expr.replace("\"", "\"\"") + "%\")");
        }
        return new VariableString(original, strings.toArray(), mode);
    }

    public static String quote(String string) {
        StringBuilder fixed = new StringBuilder();
        boolean inExpression = false;
        for (char character : string.toCharArray()) {
            if (character == '%') {
                boolean bl = inExpression = !inExpression;
            }
            if (!inExpression && character == '\"') {
                fixed.append('\"');
            }
            fixed.append(character);
        }
        return fixed.toString();
    }

    public static boolean isQuotedCorrectly(String string, boolean withQuotes) {
        if (!(!withQuotes || string.startsWith("\"") && string.endsWith("\"") && string.length() >= 2)) {
            return false;
        }
        boolean quote = false;
        boolean percentage = false;
        if (withQuotes) {
            string = string.substring(1, string.length() - 1);
        }
        for (char character : string.toCharArray()) {
            if (percentage) {
                if (character != '%') continue;
                percentage = false;
                continue;
            }
            if (quote && character != '\"') {
                return false;
            }
            if (character == '\"') {
                quote = !quote;
                continue;
            }
            if (character != '%') continue;
            percentage = true;
        }
        return !quote;
    }

    public static String unquote(String string, boolean surroundingQuotes) {
        assert (VariableString.isQuotedCorrectly(string, surroundingQuotes));
        if (surroundingQuotes) {
            return string.substring(1, string.length() - 1).replace("\"\"", "\"");
        }
        return string.replace("\"\"", "\"");
    }

    public static int nextVariableBracket(String string, int start) {
        int variableDepth = 0;
        for (int index = start; index < string.length(); ++index) {
            if (string.charAt(index) == '}') {
                if (variableDepth == 0) {
                    return index;
                }
                --variableDepth;
                continue;
            }
            if (string.charAt(index) != '{') continue;
            ++variableDepth;
        }
        return -1;
    }

    public static VariableString[] makeStrings(String[] args) {
        VariableString[] strings = new VariableString[args.length];
        int j = 0;
        for (String arg : args) {
            VariableString variableString = VariableString.newInstance(arg);
            if (variableString == null) continue;
            strings[j++] = variableString;
        }
        if (j != args.length) {
            strings = Arrays.copyOf(strings, j);
        }
        return strings;
    }

    public static VariableString @Nullable [] makeStringsFromQuoted(List<String> args) {
        VariableString[] strings = new VariableString[args.size()];
        for (int i = 0; i < args.size(); ++i) {
            assert (args.get(i).startsWith("\"") && args.get(i).endsWith("\""));
            VariableString variableString = VariableString.newInstance(args.get(i).substring(1, args.get(i).length() - 1));
            if (variableString == null) {
                return null;
            }
            strings[i] = variableString;
        }
        return strings;
    }

    @Deprecated(since="2.15", forRemoval=true)
    public String toUnformattedString(Event event) {
        if (this.isSimple) {
            assert (this.simple != null);
            return this.simple;
        }
        Object[] strings = this.strings;
        assert (strings != null);
        StringBuilder builder = new StringBuilder();
        for (Object string : strings) {
            if (string instanceof Expression) {
                builder.append(Classes.toString((Object[])((Expression)string).getArray(event), true, this.mode));
                continue;
            }
            builder.append(string);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return this.toString(null, false);
    }

    public String toString(@Nullable Event event) {
        StructVariables.DefaultVariables data;
        if (this.isSimple) {
            assert (this.simple != null);
            return this.simple;
        }
        if (event == null) {
            throw new IllegalArgumentException("Event may not be null in non-simple VariableStrings!");
        }
        Object[] string = this.strings;
        assert (string != null);
        StringBuilder builder = new StringBuilder();
        ArrayList types = new ArrayList();
        for (Object object : string) {
            if (object instanceof Expression) {
                Object[] objects = ((Expression)object).getArray(event);
                if (objects != null && objects.length > 0) {
                    types.add(objects[0].getClass());
                }
                builder.append(Classes.toString(objects, true, this.mode));
                continue;
            }
            builder.append(object);
        }
        String complete = builder.toString();
        if (this.script != null && this.mode == StringMode.VARIABLE_NAME && !types.isEmpty() && (data = this.script.getData(StructVariables.DefaultVariables.class)) != null) {
            data.add(complete, types.toArray(new Class[0]));
        }
        return complete;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.isSimple) {
            assert (this.simple != null);
            return "\"" + this.simple + "\"";
        }
        Object[] string = this.strings;
        assert (string != null);
        StringBuilder builder = new StringBuilder("\"");
        for (Object object : string) {
            if (object instanceof Expression) {
                builder.append("%").append(((Expression)object).toString(event, debug)).append("%");
                continue;
            }
            builder.append(object);
        }
        builder.append('\"');
        return builder.toString();
    }

    @NotNull
    public List<String> getDefaultVariableNames(String variableName, Event event) {
        if (this.script == null || this.mode != StringMode.VARIABLE_NAME) {
            return Lists.newArrayList();
        }
        if (this.isSimple) {
            assert (this.simple != null);
            return Lists.newArrayList((Object[])new String[]{this.simple, "object"});
        }
        StructVariables.DefaultVariables data = this.script.getData(StructVariables.DefaultVariables.class);
        assert (data != null) : "default variables not present in current script";
        Class<?>[] savedHints = data.get(variableName);
        if (savedHints == null || savedHints.length == 0) {
            return Lists.newArrayList();
        }
        ArrayList typeHints = Lists.newArrayList((Object[])new StringBuilder[]{new StringBuilder()});
        int hintIndex = 0;
        assert (this.strings != null);
        for (Object object : this.strings) {
            if (!(object instanceof Expression)) {
                typeHints.forEach(builder -> builder.append(object));
                continue;
            }
            if (hintIndex >= savedHints.length) break;
            StringBuilder[] current = typeHints.toArray(new StringBuilder[0]);
            for (ClassInfo<?> classInfo : Classes.getAllSuperClassInfos(savedHints[hintIndex])) {
                for (StringBuilder builder2 : current) {
                    String hint = builder2.toString() + "<" + classInfo.getCodeName() + ">";
                    typeHints.add(new StringBuilder(hint));
                    typeHints.remove(builder2);
                }
            }
            ++hintIndex;
        }
        return typeHints.stream().map(StringBuilder::toString).collect(Collectors.toList());
    }

    public boolean isSimple() {
        return this.isSimple;
    }

    public StringMode getMode() {
        return this.mode;
    }

    public VariableString setMode(StringMode mode) {
        if (this.mode == mode || this.isSimple) {
            return this;
        }
        try (BlockingLogHandler ignored = new BlockingLogHandler().start();){
            VariableString variableString = VariableString.newInstance(this.original, mode);
            if (variableString == null) {
                assert (false) : String.valueOf(this) + "; " + String.valueOf((Object)mode);
                VariableString variableString2 = this;
                return variableString2;
            }
            VariableString variableString3 = variableString;
            return variableString3;
        }
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSingle(Event event) {
        return this.toString(event);
    }

    public String[] getArray(Event event) {
        return new String[]{this.toString(event)};
    }

    public String[] getAll(Event event) {
        return new String[]{this.toString(event)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public boolean check(Event event, Predicate<? super String> checker, boolean negated) {
        return SimpleExpression.check(this.getAll(event), checker, negated, false);
    }

    @Override
    public boolean check(Event event, Predicate<? super String> checker) {
        return SimpleExpression.check(this.getAll(event), checker, false, false);
    }

    @Override
    @Nullable
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        if (CollectionUtils.containsSuperclass(to, String.class)) {
            return this;
        }
        return ConvertedExpression.newInstance(this, to);
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getAnd() {
        return true;
    }

    @Override
    public boolean setTime(int time) {
        return false;
    }

    @Override
    public int getTime() {
        return 0;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public Iterator<? extends String> iterator(Event event) {
        return new SingleItemIterator<String>(this.toString(event));
    }

    @Override
    public boolean isLoopOf(String input) {
        return false;
    }

    @Override
    public Expression<?> getSource() {
        return this;
    }

    public static <T> Expression<T> setStringMode(Expression<T> expression, StringMode mode) {
        if (expression instanceof ExpressionList) {
            Expression<T>[] expressions = ((ExpressionList)expression).getExpressions();
            for (int i = 0; i < expressions.length; ++i) {
                Expression expr = expressions[i];
                assert (expr != null);
                expressions[i] = VariableString.setStringMode(expr, mode);
            }
        } else if (expression instanceof VariableString) {
            return ((VariableString)expression).setMode(mode);
        }
        return expression;
    }

    @Override
    public Expression<String> simplify() {
        if (this.isSimple) {
            return SimplifiedLiteral.fromExpression(this);
        }
        if (this.strings == null || Arrays.stream(this.strings).allMatch(o -> o instanceof Literal)) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Deprecated(since="2.15", forRemoval=true)
    public List<MessageComponent> getMessageComponents(Event event) {
        return this.getMessageComponents(event, null);
    }

    @Deprecated(since="2.15", forRemoval=true)
    public List<MessageComponent> getMessageComponents(Event event, @Nullable StringBuilder unformattedBuilder) {
        if (this.isSimple) {
            assert (this.simple != null);
            return ChatMessages.parse(this.simple);
        }
        Object[] strings = this.strings;
        assert (strings != null);
        ArrayList<MessageComponent> message = new ArrayList<MessageComponent>();
        for (Object string : strings) {
            String text = null;
            if (string instanceof Expression) {
                ExprColored exprColored;
                Expression expression = (Expression)string;
                text = Classes.toString((Object[])expression.getArray(event), true, this.mode);
                if (unformattedBuilder != null) {
                    unformattedBuilder.append(text);
                }
                if (string instanceof ExprColored && (exprColored = (ExprColored)string).isUnsafeFormat()) {
                    message.addAll(ChatMessages.parse(text));
                    continue;
                }
            }
            assert (text != null);
            List<MessageComponent> components = ChatMessages.fromParsedString(text);
            if (!message.isEmpty()) {
                int startSize = message.size();
                for (int i = 0; i < components.size(); ++i) {
                    MessageComponent plain = components.get(i);
                    ChatMessages.copyStyles((MessageComponent)message.get(startSize + i - 1), plain);
                    message.add(plain);
                }
                continue;
            }
            message.addAll(components);
        }
        return message;
    }

    @Deprecated(since="2.15", forRemoval=true)
    public List<MessageComponent> getMessageComponentsUnsafe(Event event) {
        if (this.isSimple) {
            assert (this.simple != null);
            return ChatMessages.parse(this.simple);
        }
        return ChatMessages.parse(this.toUnformattedString(event));
    }

    @Deprecated(since="2.15", forRemoval=true)
    public String toChatString(Event event) {
        return ChatMessages.toJson(this.getMessageComponents(event));
    }
}

