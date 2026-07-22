/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes.data;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.AnyInfo;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumSerializer;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.classes.YggdrasilSerializer;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.function.DynamicFunctionReference;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.lang.util.common.AnyAmount;
import ch.njol.skript.lang.util.common.AnyContains;
import ch.njol.skript.lang.util.common.AnyNamed;
import ch.njol.skript.lang.util.common.AnyValued;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.localization.RegexMessage;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.skript.util.Experience;
import ch.njol.skript.util.GameruleValue;
import ch.njol.skript.util.SkriptColor;
import ch.njol.skript.util.StructureType;
import ch.njol.skript.util.Time;
import ch.njol.skript.util.Timeperiod;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.WeatherType;
import ch.njol.yggdrasil.Fields;
import java.io.File;
import java.io.StreamCorruptedException;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.ContainsHandler;
import org.skriptlang.skript.lang.properties.handlers.TypedValueHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.util.Executable;

public class SkriptClasses {
    static {
        Classes.registerClass(new ClassInfo<ClassInfo>(ClassInfo.class, "classinfo").user("types?").name("Type").description("Represents a type, e.g. number, object, item type, location, block, world, entity type, etc.", "This is mostly used for expressions like 'event-<type>', '<type>-argument', 'loop-<type>', etc., e.g. event-world, number-argument and loop-player.").usage("See the type name patterns of all types - including this one").examples("{variable} is a number # check whether the variable contains a number, e.g. -1 or 5.5", "{variable} is a type # check whether the variable contains a type, e.g. number or player", "{variable} is an object # will always succeed if the variable is set as everything is an object, even types.", "disable PvP in the event-world", "kill the loop-entity").since("2.0").after("entitydata", "entitytype", "itemtype").supplier(() -> Classes.getClassInfos().iterator()).parser(new Parser<ClassInfo>(){

            @Override
            @Nullable
            public ClassInfo parse(String s, ParseContext context) {
                return Classes.getClassInfoFromUserInput(Noun.stripIndefiniteArticle(s));
            }

            @Override
            public String toString(ClassInfo c, int flags) {
                return c.toString(flags);
            }

            @Override
            public String toVariableNameString(ClassInfo c) {
                return c.getCodeName();
            }

            @Override
            public String getDebugMessage(ClassInfo c) {
                return c.getCodeName();
            }
        }).serializer(new Serializer<ClassInfo>(){

            @Override
            public Fields serialize(ClassInfo c) {
                Fields f = new Fields();
                f.putObject("codeName", c.getCodeName());
                return f;
            }

            @Override
            public boolean canBeInstantiated() {
                return false;
            }

            @Override
            public void deserialize(ClassInfo o, Fields f) {
                assert (false);
            }

            @Override
            protected ClassInfo deserialize(Fields fields) throws StreamCorruptedException {
                String codeName = fields.getObject("codeName", String.class);
                if (codeName == null) {
                    throw new StreamCorruptedException();
                }
                ClassInfo<?> ci = Classes.getClassInfoNoError(codeName);
                if (ci == null) {
                    throw new StreamCorruptedException("Invalid ClassInfo " + codeName);
                }
                return ci;
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }
        }));
        Classes.registerClass(new ClassInfo<WeatherType>(WeatherType.class, "weathertype").user("weather ?types?", "weather conditions?", "weathers?").name("Weather Type").description("The weather types sunny, rainy, and thundering.").usage("clear/sun/sunny, rain/rainy/raining, and thunder/thundering/thunderstorm").examples("is raining", "is sunny in the player's world", "message \"It is %weather in the argument's world% in %world of the argument%\"").since("1.0").defaultExpression(new SimpleLiteral<WeatherType>(WeatherType.CLEAR, true)).parser(new Parser<WeatherType>(){

            @Override
            @Nullable
            public WeatherType parse(String s, ParseContext context) {
                return WeatherType.parse(s);
            }

            @Override
            public String toString(WeatherType o, int flags) {
                return o.toString(flags);
            }

            @Override
            public String toVariableNameString(WeatherType o) {
                return o.name().toLowerCase(Locale.ENGLISH);
            }
        }).serializer(new EnumSerializer<WeatherType>(WeatherType.class)));
        Classes.registerClass(new ClassInfo<Time>(Time.class, "time").user("times?").name("Time").description("A time is a point in a minecraft day's time (i.e. ranges from 0:00 to 23:59), which can vary per world.", "See <a href='#date'>date</a> and <a href='#timespan'>timespan</a> for the other time types of Skript.").usage("##:##", "##[:##][ ]am/pm").examples("at 20:00:", "\ttime is 8 pm", "\tbroadcast \"It's %time%\"").since("1.0").defaultExpression(new EventValueExpression<Time>(Time.class)).parser(new Parser<Time>(){

            @Override
            @Nullable
            public Time parse(String s, ParseContext context) {
                return Time.parse(s);
            }

            @Override
            public String toString(Time t, int flags) {
                return t.toString();
            }

            @Override
            public String toVariableNameString(Time o) {
                return "time:" + o.getTicks();
            }
        }).serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<Timespan>(Timespan.class, "timespan").user("time ?spans?").name("Timespan").description("A timespan is a difference of two different dates or times, e.g '10 minutes'. Timespans are always displayed as real life time, but can be defined as minecraft time, e.g. '5 minecraft days and 12 hours'.", "NOTE: Months always have the value of 30 days, and years of 365 days.", "See <a href='#date'>date</a> and <a href='#time'>time</a> for the other time types of Skript.").usage("<number> [minecraft/mc/real/rl/irl] ticks/seconds/minutes/hours/days/weeks/months/years [[,/and] <more...>]", "[###:]##:##[.####] ([hours:]minutes:seconds[.milliseconds])").examples("every 5 minecraft days:", "\twait a minecraft second and 5 ticks", "every 10 mc days and 12 hours:", "\thalt for 12.7 irl minutes, 12 hours and 120.5 seconds").since("1.0, 2.6.1 (weeks, months, years)").parser(new Parser<Timespan>(){

            @Override
            @Nullable
            public Timespan parse(String s, ParseContext context) {
                try {
                    return Timespan.parse(s, context);
                }
                catch (IllegalArgumentException e) {
                    Skript.error("'" + s + "' is not a valid timespan");
                    return null;
                }
            }

            @Override
            public String toString(Timespan t, int flags) {
                return t.toString(flags);
            }

            @Override
            public String toVariableNameString(Timespan o) {
                return "timespan:" + o.getAs(Timespan.TimePeriod.MILLISECOND);
            }
        }).serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<Timeperiod>(Timeperiod.class, "timeperiod").user("time ?periods?", "durations?").name("Timeperiod").description("A period of time between two <a href='#time'>times</a>. Mostly useful since you can use this to test for whether it's day, night, dusk or dawn in a specific world.", "This type might be removed in the future as you can use 'time of world is between x and y' as a replacement.").usage("##:## - ##:##", "dusk/day/dawn/night").examples("time in world is night").since("1.0").before("timespan").defaultExpression(new SimpleLiteral<Timeperiod>(new Timeperiod(0, 23999), true)).parser(new Parser<Timeperiod>(){

            @Override
            @Nullable
            public Timeperiod parse(String s, ParseContext context) {
                if (s.equalsIgnoreCase("day")) {
                    return new Timeperiod(0, 11999);
                }
                if (s.equalsIgnoreCase("dusk")) {
                    return new Timeperiod(12000, 13799);
                }
                if (s.equalsIgnoreCase("night")) {
                    return new Timeperiod(13800, 22199);
                }
                if (s.equalsIgnoreCase("dawn")) {
                    return new Timeperiod(22200, 23999);
                }
                int c = s.indexOf(45);
                if (c == -1) {
                    Time t = Time.parse(s);
                    if (t == null) {
                        return null;
                    }
                    return new Timeperiod(t.getTicks());
                }
                Time t1 = Time.parse(s.substring(0, c).trim());
                Time t2 = Time.parse(s.substring(c + 1).trim());
                if (t1 == null || t2 == null) {
                    return null;
                }
                return new Timeperiod(t1.getTicks(), t2.getTicks());
            }

            @Override
            public String toString(Timeperiod o, int flags) {
                return o.toString();
            }

            @Override
            public String toVariableNameString(Timeperiod o) {
                return "timeperiod:" + o.start + "-" + o.end;
            }
        }).serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<Date>(Date.class, "date").user("dates?").name("Date").description("A date is a certain point in the real world's time which can be obtained with <a href='#ExprNow'>now expression</a>, <a href='#ExprUnixDate'>unix date expression</a> and <a href='#date'>date function</a>.", "See <a href='#time'>time</a> and <a href='#timespan'>timespan</a> for the other time types of Skript.").usage("").examples("set {_yesterday} to now", "subtract a day from {_yesterday}", "# now {_yesterday} represents the date 24 hours before now").since("1.4").serializer(new Serializer<Date>(){

            @Override
            public Fields serialize(Date date) {
                Fields fields = new Fields();
                fields.putPrimitive("timestamp", date.getTime());
                return fields;
            }

            @Override
            protected Date deserialize(Fields fields) throws StreamCorruptedException {
                long time = fields.hasField("time") ? fields.getPrimitive("time", Long.TYPE).longValue() : fields.getPrimitive("timestamp", Long.TYPE).longValue();
                return new Date(time);
            }

            @Override
            public void deserialize(Date date, Fields fields) {
                throw new IllegalStateException("Cannot deserialize to an existing date object.");
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }

            @Override
            protected boolean canBeInstantiated() {
                return false;
            }
        }));
        Classes.registerClass(new ClassInfo<Direction>(Direction.class, "direction").user("directions?").name("Direction").description("A direction, e.g. north, east, behind, 5 south east, 1.3 meters to the right, etc.", "<a href='#location'>Locations</a> and some <a href='#block'>blocks</a> also have a direction, but without a length.", "Please note that directions have changed extensively in the betas and might not work perfectly. They can also not be used as command arguments.").usage("see <a href='#ExprDirection'>direction (expression)</a>").examples("set the block below the victim to a chest", "loop blocks from the block infront of the player to the block 10 below the player:", "\tset the block behind the loop-block to water").since("2.0").defaultExpression(new SimpleLiteral<Direction>(new Direction(new double[]{0.0, 0.0, 0.0}), true)).parser(new Parser<Direction>(){

            @Override
            @Nullable
            public Direction parse(String s, ParseContext context) {
                return null;
            }

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(Direction o, int flags) {
                return o.toString();
            }

            @Override
            public String toVariableNameString(Direction o) {
                return o.toString();
            }
        }).serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<Color>(Color.class, "color").user("colou?rs?").name("Color").description("Wool, dye and chat colors.").usage("black, dark grey/dark gray, grey/light grey/gray/light gray/silver, white, blue/dark blue, cyan/aqua/dark cyan/dark aqua, light blue/light cyan/light aqua, green/dark green, light green/lime/lime green, yellow/light yellow, orange/gold/dark yellow, red/dark red, pink/light red, purple/dark purple, magenta/light purple, brown/indigo").examples("color of the sheep is red or black", "set the color of the block to green", "message \"You're holding a <%color of tool%>%color of tool%<reset> wool block\"").since("").supplier((Color[])SkriptColor.values()).parser(new Parser<Color>(){

            @Override
            @Nullable
            public Color parse(String input, ParseContext context) {
                ColorRGB rgbColor = ColorRGB.fromString(input);
                if (rgbColor != null) {
                    return rgbColor;
                }
                return SkriptColor.fromName(input);
            }

            @Override
            public String toString(Color c, int flags) {
                return c.getName();
            }

            @Override
            public String toVariableNameString(Color color) {
                return color.getName().toLowerCase(Locale.ENGLISH).replace('_', ' ');
            }
        }));
        Classes.registerClass(new ClassInfo<StructureType>(StructureType.class, "structuretype").user("tree ?types?", "trees?").name("Tree Type").description("A tree type represents a tree species or a huge mushroom species. These can be generated in a world with the <a href='#EffTree'>generate tree</a> effect.").usage("[any] <general tree/mushroom type>, e.g. tree/any jungle tree/etc.", "<specific tree/mushroom species>, e.g. red mushroom/small jungle tree/big regular tree/etc.").examples("grow any regular tree at the block", "grow a huge red mushroom above the block").since("").defaultExpression(new SimpleLiteral<StructureType>(StructureType.TREE, true)).parser(new Parser<StructureType>(){

            @Override
            @Nullable
            public StructureType parse(String s, ParseContext context) {
                return StructureType.fromName(s);
            }

            @Override
            public String toString(StructureType o, int flags) {
                return o.toString(flags);
            }

            @Override
            public String toVariableNameString(StructureType o) {
                return o.name().toLowerCase(Locale.ENGLISH);
            }
        }).serializer(new EnumSerializer<StructureType>(StructureType.class)));
        Classes.registerClass(new ClassInfo<EnchantmentType>(EnchantmentType.class, "enchantmenttype").user("enchant(ing|ment) types?").name("Enchantment Type").description("An enchantment with an optional level, e.g. 'sharpness 2' or 'fortune'.").usage("<enchantment> [<level>]").examples("enchant the player's tool with sharpness 5", "helmet is enchanted with waterbreathing").since("1.4.6").parser(new Parser<EnchantmentType>(){

            @Override
            @Nullable
            public EnchantmentType parse(String s, ParseContext context) {
                return EnchantmentType.parse(s);
            }

            @Override
            public String toString(EnchantmentType t, int flags) {
                return t.toString();
            }

            @Override
            public String toVariableNameString(EnchantmentType o) {
                return o.toString();
            }
        }).serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<Experience>(Experience.class, "experience").user("experience ?(points?)?").name("Experience").description("Experience points. Please note that Bukkit only allows to give XP, but not remove XP from players. You can however change a player's <a href='#ExprLevel'>level</a> and <a href='./expressions/#ExprLevelProgress'>level progress</a> freely.").usage("[<number>] ([e]xp|experience [point[s]])").examples("give 10 xp to the player").since("2.0").parser(new Parser<Experience>(){
            private final RegexMessage pattern = new RegexMessage("types.experience.pattern", 2);

            @Override
            @Nullable
            public Experience parse(String s, ParseContext context) {
                int xp = -1;
                if (((String)s).matches("\\d+ .+")) {
                    xp = Utils.parseInt(((String)s).substring(0, ((String)s).indexOf(32)));
                    s = ((String)s).substring(((String)s).indexOf(32) + 1);
                }
                if (this.pattern.matcher((String)s).matches()) {
                    return new Experience(xp);
                }
                return null;
            }

            @Override
            public String toString(Experience xp, int flags) {
                return xp.toString();
            }

            @Override
            public String toVariableNameString(Experience xp) {
                return "" + xp.getXP();
            }
        }).serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<GameruleValue>(GameruleValue.class, "gamerulevalue").user("gamerule values?").name("Gamerule Value").description("A wrapper for the value of a gamerule for a world.").usage("").examples("").since("2.5").serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<Config>(Config.class, "config").user("configs?").name("Config").description("A configuration (or code) loaded by Skript, such as the config.sk or aliases.", "Configs can be reloaded or navigated to find options.").usage("").examples("the skript config").since("2.10").parser(new Parser<Config>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(Config config, int flags) {
                @Nullable File file = config.getFile();
                if (file == null) {
                    return config.getFileName();
                }
                return Skript.getInstance().getDataFolder().getAbsoluteFile().toPath().relativize(file.toPath().toAbsolutePath()).toString();
            }

            @Override
            public String toVariableNameString(Config config) {
                return this.toString(config, 0);
            }
        }).property(Property.NAME, "The filename of the Config, as text.", Skript.instance(), ExpressionPropertyHandler.of(Config::name, String.class)));
        Classes.registerClass(new ClassInfo<Node>(Node.class, "node").user("nodes?").name("Node").description("A node (entry) from a script config file.", "This may have navigable children.").usage("").examples("the current script").since("2.10").parser(new Parser<Node>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(Node node, int flags) {
                return node.getPath();
            }

            @Override
            public String toVariableNameString(Node node) {
                return this.toString(node, 0);
            }
        }).property(Property.NAME, "The key of the node, as text.", Skript.instance(), ExpressionPropertyHandler.of(Node::getKey, String.class)).property(Property.TYPED_VALUE, "The value of the node, if it is an entry node, as text.", Skript.instance(), new TypedValueHandler<Node, String>(){

            @Override
            @Nullable
            public String convert(Node propertyHolder) {
                if (propertyHolder instanceof EntryNode) {
                    EntryNode entryNode = (EntryNode)propertyHolder;
                    return entryNode.getValue();
                }
                return null;
            }

            @Override
            @NotNull
            public Class<String> returnType() {
                return String.class;
            }
        }));
        Classes.registerClass(new ClassInfo<Executable>(Executable.class, "executable").user("executables?").name("Executable").description("Something that can be executed (run) and may accept arguments, e.g. a function.", "This may also return a result.").examples("run {_function} with arguments 1 and true").since("2.10"));
        Classes.registerClass(new ClassInfo<DynamicFunctionReference>(DynamicFunctionReference.class, "function").user("functions?").name("Function").description("A function loaded by Skript.", "This can be executed (with arguments) and may return a result.").examples("run {_function} with arguments 1 and true", "set {_result} to the result of {_function}").since("2.10").parser(new Parser<DynamicFunctionReference<?>>(){

            @Override
            public boolean canParse(ParseContext context) {
                return switch (context) {
                    case ParseContext.PARSE, ParseContext.COMMAND -> true;
                    default -> false;
                };
            }

            @Override
            @Nullable
            public DynamicFunctionReference<?> parse(String name, ParseContext context) {
                return switch (context) {
                    case ParseContext.PARSE, ParseContext.COMMAND -> DynamicFunctionReference.parseFunction(name);
                    default -> null;
                };
            }

            @Override
            public String toString(DynamicFunctionReference<?> function, int flags) {
                return function.toString();
            }

            @Override
            public String toVariableNameString(DynamicFunctionReference<?> function) {
                return this.toString(function, 0);
            }
        }).property(Property.NAME, "The function's name, as text.", Skript.instance(), ExpressionPropertyHandler.of(DynamicFunctionReference::name, String.class)));
        Classes.registerClass(new AnyInfo<AnyNamed>(AnyNamed.class, "named").name("Any Named Thing").description("Something that has a name (e.g. an item).").usage("").examples("{thing}'s name").since("2.10").property(Property.NAME, "The name of the thing, as text. Can be set if supported.", Skript.instance(), new ExpressionPropertyHandler<AnyNamed, String>(){

            @Override
            @NotNull
            public Class<String> returnType() {
                return String.class;
            }

            @Override
            public String convert(AnyNamed anyNamed) {
                return anyNamed.name();
            }

            @Override
            public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
                if (mode == Changer.ChangeMode.SET) {
                    return new Class[]{String.class};
                }
                return null;
            }

            @Override
            public void change(AnyNamed named, Object @Nullable [] delta, Changer.ChangeMode mode) {
                if (mode == Changer.ChangeMode.SET && named.supportsNameChange()) {
                    assert (delta != null);
                    named.setName((String)delta[0]);
                }
            }
        }));
        ExpressionPropertyHandler<AnyAmount, Number> amountHandler = new ExpressionPropertyHandler<AnyAmount, Number>(){

            @Override
            public Number convert(AnyAmount anyNamed) {
                return anyNamed.amount();
            }

            @Override
            public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
                if (mode == Changer.ChangeMode.SET) {
                    return new Class[]{String.class};
                }
                return null;
            }

            @Override
            public void change(AnyAmount named, Object @Nullable [] delta, Changer.ChangeMode mode) {
                if (mode == Changer.ChangeMode.SET && named.supportsAmountChange()) {
                    assert (delta != null);
                    named.setAmount((Number)delta[0]);
                }
            }

            @Override
            @NotNull
            public Class<Number> returnType() {
                return Number.class;
            }
        };
        Classes.registerClass(new AnyInfo<AnyAmount>(AnyAmount.class, "numbered").name("Any Numbered/Sized Thing").description("Something that has an amount or size.").usage("").examples("the size of {thing}", "the amount of {thing}").since("2.10").property(Property.AMOUNT, "The amount of a thing", Skript.instance(), amountHandler).property(Property.SIZE, "The size of a thing", Skript.instance(), amountHandler).property(Property.NUMBER, "The number of a thing", Skript.instance(), amountHandler).property(Property.IS_EMPTY, "Whether this thing is empty, i.e. has an amount of 0.", Skript.instance(), ConditionPropertyHandler.of(AnyAmount::isEmpty)));
        Classes.registerClass(new AnyInfo<AnyValued>(AnyValued.class, "valued").name("Any Valued Thing").description("Something that has a value.").usage("").examples("the text of {node}").since("2.10").property(Property.TYPED_VALUE, "The value of something. Can be set.", Skript.instance(), new TypedValueHandler<AnyValued, Object>(){

            @Override
            public Object convert(AnyValued propertyHolder) {
                return propertyHolder.value();
            }

            @Override
            public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
                Class[] classArray;
                switch (mode) {
                    case SET: 
                    case RESET: 
                    case DELETE: {
                        Class[] classArray2 = new Class[1];
                        classArray = classArray2;
                        classArray2[0] = Object.class;
                        break;
                    }
                    default: {
                        classArray = null;
                    }
                }
                return classArray;
            }

            @Override
            public void change(AnyValued propertyHolder, Object @Nullable [] delta, Changer.ChangeMode mode) {
                if (propertyHolder.supportsValueChange()) {
                    if (delta != null) {
                        propertyHolder.changeValue(delta[0]);
                    } else {
                        propertyHolder.resetValue();
                    }
                }
            }

            @Override
            @NotNull
            public Class<Object> returnType() {
                return Object.class;
            }
        }));
        Classes.registerClass(new AnyInfo<AnyContains>(AnyContains.class, "containing").user("any container").name("Anything with Contents").description("Something that contains other things.").usage("").examples("{a} contains {b}").since("2.10").property(Property.CONTAINS, "AnyContains can contain other things depending on its type.", Skript.instance(), new ContainsHandler<AnyContains, Object>(){

            @Override
            public boolean contains(AnyContains anyContains, Object object) {
                return anyContains.checkSafely(object);
            }

            @Override
            public Class<?>[] elementTypes() {
                return new Class[]{Object.class};
            }
        }));
    }
}

