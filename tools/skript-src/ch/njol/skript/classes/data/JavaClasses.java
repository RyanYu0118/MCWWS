/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes.data;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.RegexMessage;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.StringUtils;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.ContainsHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler;

public class JavaClasses {
    public static final int VARIABLENAME_NUMBERACCURACY = 8;
    public static final String SCIENTIFIC_PATTERN = "(?:[eE][+-]?\\d+)?";
    public static final String INTEGER_NUMBER_PATTERN = "-?\\d+(_\\d+)*";
    public static final Pattern INTEGER_PATTERN = Pattern.compile("(?<num>%s%s)(?: (?:in )?(?:(?<rad>rad(?:ian)?)|deg(?:ree)?)s?)?".formatted("-?\\d+(_\\d+)*", "(?:[eE][+-]?\\d+)?"));
    public static final String DECIMAL_NUMBER_PATTERN = "-?\\d+(_\\d+)*(?>\\.\\d+(_\\d+)*)?%?";
    public static final Pattern DECIMAL_PATTERN = Pattern.compile("(?<num>%s%s)(?: (?:in )?(?:(?<rad>rad(?:ian)?)|deg(?:ree)?)s?)?".formatted("-?\\d+(_\\d+)*(?>\\.\\d+(_\\d+)*)?%?", "(?:[eE][+-]?\\d+)?"));

    @Contract(pure=true)
    @Nullable
    private static <T extends Number> T convertIntegerFormatted(String string, Function<String, T> stringToNumber) {
        Matcher matcher = INTEGER_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        String number = matcher.group("num").replace("_", "");
        if (matcher.group("rad") != null) {
            try {
                return (T)Double.valueOf(Math.toDegrees(((Number)stringToNumber.apply(number)).doubleValue()));
            }
            catch (NumberFormatException numberFormatException) {
            }
        } else {
            try {
                return (T)((Number)stringToNumber.apply(number));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return null;
    }

    @Contract(pure=true)
    @Nullable
    private static <T extends Number> T convertDecimalFormatted(String string, Function<String, T> stringToNumber) {
        Matcher matcher = DECIMAL_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        String number = matcher.group("num").replace("_", "");
        try {
            Number result;
            if (number.endsWith("%")) {
                Number extracted = (Number)stringToNumber.apply(number.substring(0, number.length() - 1));
                result = extracted.doubleValue() / 100.0;
            } else {
                result = (Number)stringToNumber.apply(number);
            }
            if (matcher.group("rad") != null) {
                try {
                    return (T)Double.valueOf(Math.toDegrees(result.doubleValue()));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
            return (T)result;
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    static {
        Classes.registerClass(new ClassInfo<Object>(Object.class, "object").user("objects?").name("Object").description("The supertype of all types, meaning that if %object% is used in e.g. a condition it will accept all kinds of expressions.").usage("").examples("").since("1.0"));
        Classes.registerClass(new ClassInfo<Number>(Number.class, "number").user("num(ber)?s?").name("Number").description("A number, e.g. 2.5, 3, -9812454, 30 degrees or 3.14 radians.", "Please note that many expressions only need integers, i.e. will discard any fractional parts of any numbers without producing an error.", "Radians will be converted to degrees.").usage("[-]###[.###] [e[+|-]###] [[in ](rad[ian][s]|deg[ree][s])]").examples("set the player's health to 5.5", "set {_temp} to 2*{_temp} - 2.5", "set {_angle} to 3.14 in radians # will be converted to degrees").since("1.0").defaultExpression(new SimpleLiteral<Integer>(1, true)).parser(new NumberParser()).serializer(new NumberSerializer()));
        Classes.registerClass(new ClassInfo<Long>(Long.class, "long").user("int(eger)?s?").name(ClassInfo.NO_DOC).before("integer", "short", "byte").defaultExpression(new SimpleLiteral<Long>(1L, true)).parser(new LongParser()).serializer(new LongSerializer()));
        Classes.registerClass(new ClassInfo<Integer>(Integer.class, "integer").name(ClassInfo.NO_DOC).defaultExpression(new SimpleLiteral<Integer>(1, true)).parser(new IntegerParser()).serializer(new IntegerSerializer()));
        Classes.registerClass(new ClassInfo<Double>(Double.class, "double").name(ClassInfo.NO_DOC).defaultExpression(new SimpleLiteral<Double>(1.0, true)).after("long").before("float", "integer", "short", "byte").parser(new DoubleParser()).serializer(new DoubleSerializer()));
        Classes.registerClass(new ClassInfo<Float>(Float.class, "float").name(ClassInfo.NO_DOC).defaultExpression(new SimpleLiteral<Float>(Float.valueOf(1.0f), true)).parser(new FloatParser()).serializer(new FloatSerializer()));
        Classes.registerClass(new ClassInfo<Boolean>(Boolean.class, "boolean").user("booleans?").name("Boolean").description("A boolean is a value that is either true or false. Other accepted names are 'on' and 'yes' for true, and 'off' and 'no' for false.").usage("true/yes/on or false/no/off").examples("set {config.%player%.use mod} to false").since("1.0").parser(new Parser<Boolean>(){
            private final RegexMessage truePattern = new RegexMessage("boolean.true.pattern");
            private final RegexMessage falsePattern = new RegexMessage("boolean.false.pattern");
            private final Message trueName = new Message("boolean.true.name");
            private final Message falseName = new Message("boolean.false.name");

            @Override
            @Nullable
            public Boolean parse(String s, ParseContext context) {
                if (this.truePattern.matcher(s).matches()) {
                    return Boolean.TRUE;
                }
                if (this.falsePattern.matcher(s).matches()) {
                    return Boolean.FALSE;
                }
                return null;
            }

            @Override
            public String toString(Boolean b, int flags) {
                return b != false ? this.trueName.toString() : this.falseName.toString();
            }

            @Override
            public String toVariableNameString(Boolean b) {
                return "" + b;
            }
        }).serializer(new Serializer<Boolean>(){

            @Override
            public Fields serialize(Boolean n) {
                throw new IllegalStateException();
            }

            @Override
            public boolean canBeInstantiated() {
                return true;
            }

            @Override
            public void deserialize(Boolean o, Fields f) {
                assert (false);
            }

            @Override
            @Nullable
            public Boolean deserialize(String s) {
                if (s.equals("true")) {
                    return Boolean.TRUE;
                }
                if (s.equals("false")) {
                    return Boolean.FALSE;
                }
                return null;
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }
        }));
        Classes.registerClass(new ClassInfo<Short>(Short.class, "short").name(ClassInfo.NO_DOC).defaultExpression(new SimpleLiteral<Short>((short)1, true)).parser(new ShortParser()).serializer(new ShortSerializer()));
        Classes.registerClass(new ClassInfo<Byte>(Byte.class, "byte").name(ClassInfo.NO_DOC).defaultExpression(new SimpleLiteral<Byte>((byte)1, true)).parser(new ByteParser()).serializer(new ByteSerializer()));
        Classes.registerClass(new ClassInfo<String>(String.class, "string").user("(text|string)s?").name("Text").description("Text is simply text, i.e. a sequence of characters, which can optionally contain expressions which will be replaced with a meaningful representation (e.g. %player% will be replaced with the player's name).", "Because scripts are also text, you have to put text into double quotes to tell Skript which part of the line is an effect/expression and which part is the text.", "Please read the article on <a href='./text.html'>Texts and Variable Names</a> to learn more.").usage("simple: \"...\"", "quotes: \"...\"\"...\"", "expressions: \"...%expression%...\"", "percent signs: \"...%%...\"").examples("broadcast \"Hello World!\"", "message \"Hello %player%\"", "message \"The id of \"\"%type of tool%\"\" is %id of tool%.\"").since("1.0").parser(new Parser<String>(){

            @Override
            @Nullable
            public String parse(String s, ParseContext context) {
                switch (context) {
                    case DEFAULT: {
                        assert (false);
                        return null;
                    }
                    case CONFIG: {
                        return s;
                    }
                    case SCRIPT: 
                    case EVENT: {
                        if (VariableString.isQuotedCorrectly(s, true)) {
                            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
                        }
                        return null;
                    }
                    case COMMAND: 
                    case PARSE: {
                        return s;
                    }
                }
                assert (false);
                return null;
            }

            @Override
            public boolean canParse(ParseContext context) {
                return context != ParseContext.DEFAULT;
            }

            @Override
            public String toString(String s, int flags) {
                return s;
            }

            @Override
            public String getDebugMessage(String s) {
                return "\"" + s + "\"";
            }

            @Override
            public String toVariableNameString(String s) {
                return s;
            }
        }).serializer(new Serializer<String>(){

            @Override
            public Fields serialize(String n) {
                throw new IllegalStateException();
            }

            @Override
            public boolean canBeInstantiated() {
                return true;
            }

            @Override
            public void deserialize(String o, Fields f) {
                assert (false);
            }

            @Override
            public String deserialize(String s) {
                return s;
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }
        }).property(Property.CONTAINS, "Strings can contain other strings.", Skript.instance(), new ContainsHandler<String, String>(){

            @Override
            public boolean contains(String container, String element) {
                return StringUtils.contains(container, element, SkriptConfig.caseSensitive.value());
            }

            @Override
            public Class<? extends String>[] elementTypes() {
                return new Class[]{String.class};
            }
        }).property(Property.IS_EMPTY, "Whether the string is empty, i.e. has no characters.", Skript.instance(), ConditionPropertyHandler.of(String::isEmpty)));
        Classes.registerClass(new ClassInfo<UUID>(UUID.class, "uuid").user("uuids?").name("UUID").description("UUIDs are unique identifiers that ensure things can be reliably distinguished from each other. They are generated in a way that makes it practically impossible for duplicates to occur.", "Read more about UUIDs and how they are used in Minecraft in <a href='https://minecraft.wiki/w/UUID'>the wiki entry about UUIDs</a>.").since("2.11").parser(new UUIDParser()).serializer(new UUIDSerializer()));
    }

    private static class NumberParser
    extends Parser<Number> {
        private NumberParser() {
        }

        @Override
        @Nullable
        public Number parse(String string, ParseContext context) {
            Matcher numberMatcher = DECIMAL_PATTERN.matcher(string);
            if (!numberMatcher.matches()) {
                return null;
            }
            Integer integerAttempt = JavaClasses.convertIntegerFormatted(string, Integer::parseInt);
            if (integerAttempt != null) {
                return integerAttempt;
            }
            Double parsed = JavaClasses.convertDecimalFormatted(string, Double::parseDouble);
            return parsed == null || parsed.isInfinite() || parsed.isNaN() ? null : parsed;
        }

        @Override
        public String toString(Number number, int flags) {
            return StringUtils.toString(number.doubleValue(), SkriptConfig.numberAccuracy.value());
        }

        @Override
        public String toVariableNameString(Number number) {
            return StringUtils.toString(number.doubleValue(), 8);
        }
    }

    private static class NumberSerializer
    extends Serializer<Number> {
        private NumberSerializer() {
        }

        @Override
        public Fields serialize(Number number) {
            throw new IllegalStateException();
        }

        @Override
        public boolean canBeInstantiated() {
            return true;
        }

        @Override
        public void deserialize(Number number, Fields fields) {
            assert (false);
        }

        @Override
        @Nullable
        public Number deserialize(String string) {
            try {
                return Integer.valueOf(string);
            }
            catch (NumberFormatException numberFormatException) {
                try {
                    return Double.valueOf(string);
                }
                catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }
    }

    private static class LongParser
    extends Parser<Long> {
        private LongParser() {
        }

        @Override
        @Nullable
        public Long parse(String string, ParseContext context) {
            return JavaClasses.convertIntegerFormatted(string, Long::parseLong);
        }

        @Override
        public String toString(Long l, int flags) {
            return l.toString();
        }

        @Override
        public String toVariableNameString(Long l) {
            return l.toString();
        }
    }

    private static class LongSerializer
    extends Serializer<Long> {
        private LongSerializer() {
        }

        @Override
        public Fields serialize(Long l) {
            throw new IllegalStateException();
        }

        @Override
        public boolean canBeInstantiated() {
            return true;
        }

        @Override
        public void deserialize(Long l, Fields fields) {
            assert (false);
        }

        @Override
        @Nullable
        public Long deserialize(String string) {
            try {
                return Long.parseLong(string);
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }
    }

    private static class IntegerParser
    extends Parser<Integer> {
        private IntegerParser() {
        }

        @Override
        @Nullable
        public Integer parse(String string, ParseContext context) {
            return JavaClasses.convertIntegerFormatted(string, Integer::parseInt);
        }

        @Override
        public String toString(Integer i, int flags) {
            return i.toString();
        }

        @Override
        public String toVariableNameString(Integer i) {
            return i.toString();
        }
    }

    private static class IntegerSerializer
    extends Serializer<Integer> {
        private IntegerSerializer() {
        }

        @Override
        public Fields serialize(Integer i) {
            throw new IllegalStateException();
        }

        @Override
        public boolean canBeInstantiated() {
            return true;
        }

        @Override
        public void deserialize(Integer i, Fields fields) {
            assert (false);
        }

        @Override
        @Nullable
        public Integer deserialize(String string) {
            try {
                return Integer.parseInt(string);
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }
    }

    private static class DoubleParser
    extends Parser<Double> {
        private DoubleParser() {
        }

        @Override
        @Nullable
        public Double parse(String string, ParseContext context) {
            Double parsed = JavaClasses.convertDecimalFormatted(string, Double::parseDouble);
            return parsed == null || parsed.isInfinite() || parsed.isNaN() ? null : parsed;
        }

        @Override
        public String toString(Double d, int flags) {
            return StringUtils.toString(d, SkriptConfig.numberAccuracy.value());
        }

        @Override
        public String toVariableNameString(Double d) {
            return StringUtils.toString(d, 8);
        }
    }

    private static class DoubleSerializer
    extends Serializer<Double> {
        private DoubleSerializer() {
        }

        @Override
        public Fields serialize(Double d) {
            throw new IllegalStateException();
        }

        @Override
        public boolean canBeInstantiated() {
            return true;
        }

        @Override
        public void deserialize(Double d, Fields fields) {
            assert (false);
        }

        @Override
        @Nullable
        public Double deserialize(String string) {
            try {
                return Double.parseDouble(string);
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }
    }

    private static class FloatParser
    extends Parser<Float> {
        private FloatParser() {
        }

        @Override
        @Nullable
        public Float parse(String string, ParseContext context) {
            Float parsed = JavaClasses.convertDecimalFormatted(string, Float::parseFloat);
            return parsed == null || parsed.isInfinite() || parsed.isNaN() ? null : parsed;
        }

        @Override
        public String toString(Float f, int flags) {
            return StringUtils.toString(f.floatValue(), SkriptConfig.numberAccuracy.value());
        }

        @Override
        public String toVariableNameString(Float f) {
            return StringUtils.toString(f.doubleValue(), 8);
        }
    }

    private static class FloatSerializer
    extends Serializer<Float> {
        private FloatSerializer() {
        }

        @Override
        public Fields serialize(Float f) {
            throw new IllegalStateException();
        }

        @Override
        public boolean canBeInstantiated() {
            return true;
        }

        @Override
        public void deserialize(Float f, Fields fields) {
            assert (false);
        }

        @Override
        @Nullable
        public Float deserialize(String string) {
            try {
                return Float.valueOf(Float.parseFloat(string));
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }
    }

    private static class ShortParser
    extends Parser<Short> {
        private ShortParser() {
        }

        @Override
        @Nullable
        public Short parse(String string, ParseContext context) {
            return JavaClasses.convertIntegerFormatted(string, Short::parseShort);
        }

        @Override
        public String toString(Short s, int flags) {
            return s.toString();
        }

        @Override
        public String toVariableNameString(Short s) {
            return s.toString();
        }
    }

    private static class ShortSerializer
    extends Serializer<Short> {
        private ShortSerializer() {
        }

        @Override
        public Fields serialize(Short s) {
            throw new IllegalStateException();
        }

        @Override
        public boolean canBeInstantiated() {
            return true;
        }

        @Override
        public void deserialize(Short s, Fields fields) {
            assert (false);
        }

        @Override
        @Nullable
        public Short deserialize(String string) {
            try {
                return Short.parseShort(string);
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }
    }

    private static class ByteParser
    extends Parser<Byte> {
        private ByteParser() {
        }

        @Override
        @Nullable
        public Byte parse(String string, ParseContext context) {
            return JavaClasses.convertIntegerFormatted(string, Byte::parseByte);
        }

        @Override
        public String toString(Byte b, int flags) {
            return b.toString();
        }

        @Override
        public String toVariableNameString(Byte b) {
            return b.toString();
        }
    }

    private static class ByteSerializer
    extends Serializer<Byte> {
        private ByteSerializer() {
        }

        @Override
        public Fields serialize(Byte b) {
            throw new IllegalStateException();
        }

        @Override
        public boolean canBeInstantiated() {
            return true;
        }

        @Override
        public void deserialize(Byte b, Fields fields) {
            assert (false);
        }

        @Override
        @Nullable
        public Byte deserialize(String string) {
            try {
                return Byte.parseByte(string);
            }
            catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }
    }

    private static class UUIDParser
    extends Parser<UUID> {
        private UUIDParser() {
        }

        @Override
        @Nullable
        public UUID parse(String string, ParseContext context) {
            if (Utils.isValidUUID(string)) {
                return UUID.fromString(string);
            }
            return null;
        }

        @Override
        public String toString(UUID uuid, int flags) {
            return uuid.toString();
        }

        @Override
        public String toVariableNameString(UUID uuid) {
            return uuid.toString();
        }
    }

    private static class UUIDSerializer
    extends Serializer<UUID> {
        private UUIDSerializer() {
        }

        @Override
        public Fields serialize(UUID uuid) {
            Fields fields = new Fields();
            fields.putPrimitive("mostsignificantbits", uuid.getMostSignificantBits());
            fields.putPrimitive("leastsignificantbits", uuid.getLeastSignificantBits());
            return fields;
        }

        @Override
        public void deserialize(UUID o, Fields f) {
            assert (false);
        }

        @Override
        protected UUID deserialize(Fields fields) throws StreamCorruptedException {
            long mostSignificantBits = fields.getAndRemovePrimitive("mostsignificantbits", Long.TYPE);
            long leastSignificantBits = fields.getAndRemovePrimitive("leastsignificantbits", Long.TYPE);
            return new UUID(mostSignificantBits, leastSignificantBits);
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }

        @Override
        protected boolean canBeInstantiated() {
            return false;
        }
    }
}

