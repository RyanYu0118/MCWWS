/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Iterables
 *  com.google.common.io.ByteArrayDataInput
 *  com.google.common.io.ByteArrayDataOutput
 *  com.google.common.io.ByteStreams
 *  net.md_5.bungee.api.ChatColor
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.plugin.messaging.Messenger
 *  org.bukkit.plugin.messaging.PluginMessageListener
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.SkriptColor;
import ch.njol.util.NonNullPair;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.util.ClassLoader;

public abstract class Utils {
    public static final Random random = new Random();
    protected static final Deque<WordEnding> plurals = new LinkedList<WordEnding>();
    static final ChatColor[] styles;
    static final Map<String, String> chat;
    static final Map<String, String> englishChat;
    private static final Pattern STYLE_PATTERN;
    private static final Pattern UNICODE_PATTERN;
    private static final Pattern HEX_PATTERN;

    private Utils() {
    }

    public static String join(Object[] objects) {
        assert (objects != null);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < objects.length; ++i) {
            if (i != 0) {
                b.append(", ");
            }
            b.append(Classes.toString(objects[i]));
        }
        return b.toString();
    }

    public static String join(Iterable<?> objects) {
        assert (objects != null);
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Object o : objects) {
            if (!first) {
                b.append(", ");
            } else {
                first = false;
            }
            b.append(Classes.toString(o));
        }
        return b.toString();
    }

    public static <T> boolean isEither(@Nullable T compared, T ... types) {
        return CollectionUtils.contains(types, compared);
    }

    public static Pair<String, Integer> getAmount(String s) {
        if (s.matches("\\d+ of .+")) {
            return new Pair<String, Integer>(s.split(" ", 3)[2], Utils.parseInt(s.split(" ", 2)[0]));
        }
        if (s.matches("\\d+ .+")) {
            return new Pair<String, Integer>(s.split(" ", 2)[1], Utils.parseInt(s.split(" ", 2)[0]));
        }
        if (s.matches("an? .+")) {
            return new Pair<String, Integer>(s.split(" ", 2)[1], 1);
        }
        return new Pair<String, Integer>(s, -1);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static Class<?>[] getClasses(Plugin plugin, String basePackage, String ... subPackages) throws IOException {
        ArrayList classes = new ArrayList();
        ClassLoader loader = ClassLoader.builder().basePackage(basePackage).addSubPackages(subPackages).deep(true).initialize(true).forEachClass(classes::add).build();
        File jarFile = Utils.getFile(plugin);
        if (jarFile != null) {
            loader.loadClasses(plugin.getClass(), jarFile);
        } else {
            loader.loadClasses(plugin.getClass());
        }
        return classes.toArray(new Class[0]);
    }

    @Nullable
    public static File getFile(Plugin plugin) {
        try {
            Method getFile = JavaPlugin.class.getDeclaredMethod("getFile", new Class[0]);
            getFile.setAccessible(true);
            return (File)getFile.invoke((Object)plugin, new Object[0]);
        }
        catch (NoSuchMethodException e) {
            Skript.outdatedError(e);
        }
        catch (IllegalArgumentException e) {
            Skript.outdatedError(e);
        }
        catch (IllegalAccessException e) {
            assert (false);
        }
        catch (SecurityException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
        return null;
    }

    @Deprecated(forRemoval=true, since="2.14")
    public static NonNullPair<String, Boolean> getEnglishPlural(String word) {
        PluralResult result = Utils.isPlural(word);
        return new NonNullPair<String, Boolean>(result.updated, result.plural);
    }

    public static PluralResult isPlural(String word) {
        Preconditions.checkNotNull((Object)word, (Object)"word cannot be null");
        if (word.isEmpty()) {
            return new PluralResult("", false);
        }
        if (Utils.couldBeSingular(word)) {
            return new PluralResult(word, false);
        }
        for (WordEnding ending : plurals) {
            if (ending.isCompleteWord() && word.length() != ending.plural().length()) continue;
            if (word.endsWith(ending.plural())) {
                return new PluralResult(word.substring(0, word.length() - ending.plural().length()) + ending.singular(), true);
            }
            if (!word.endsWith(ending.plural().toUpperCase(Locale.ENGLISH))) continue;
            return new PluralResult(word.substring(0, word.length() - ending.plural().length()) + ending.singular().toUpperCase(Locale.ENGLISH), true);
        }
        return new PluralResult(word, false);
    }

    private static boolean couldBeSingular(String word) {
        for (WordEnding ending : plurals) {
            if (ending.singular().isBlank() || ending.isCompleteWord() && ending.singular().length() != word.length() || !word.endsWith(ending.singular()) && !word.toLowerCase().endsWith(ending.singular())) continue;
            return true;
        }
        return false;
    }

    public static void addPluralOverride(String singular, String plural) {
        plurals.addFirst(new WordEnding(singular, plural, true));
    }

    public static String toEnglishPlural(String word) {
        assert (word != null && word.length() != 0);
        for (WordEnding ending : plurals) {
            if (ending.isCompleteWord() && word.length() != ending.singular().length() || !word.endsWith(ending.singular())) continue;
            return word.substring(0, word.length() - ending.singular().length()) + ending.plural();
        }
        assert (false);
        return word + "s";
    }

    public static String toEnglishPlural(String s, boolean p) {
        if (p) {
            return Utils.toEnglishPlural(s);
        }
        return s;
    }

    public static String a(String s) {
        return Utils.a(s, false);
    }

    public static String A(String s) {
        return Utils.a(s, true);
    }

    public static String a(String s, boolean capA) {
        assert (s != null && s.length() != 0);
        if ("aeiouAEIOU".indexOf(s.charAt(0)) != -1) {
            if (capA) {
                return "An " + s;
            }
            return "an " + s;
        }
        if (capA) {
            return "A " + s;
        }
        return "a " + s;
    }

    public static double getBlockHeight(int type, byte data) {
        switch (type) {
            case 26: {
                return 0.5625;
            }
            case 44: 
            case 126: {
                return (data & 8) == 0 ? 0.5 : 1.0;
            }
            case 78: {
                return data == 0 ? 1.0 : (double)(data % 8) * 2.0 / 16.0;
            }
            case 85: 
            case 107: 
            case 113: 
            case 139: {
                return 1.5;
            }
            case 88: {
                return 0.875;
            }
            case 92: {
                return 0.4375;
            }
            case 93: 
            case 94: 
            case 149: 
            case 150: {
                return 0.125;
            }
            case 96: {
                return (data & 4) == 0 ? ((data & 8) == 0 ? 0.1875 : 1.0) : 0.0;
            }
            case 116: {
                return 0.75;
            }
            case 117: {
                return 0.875;
            }
            case 118: {
                return 0.3125;
            }
            case 120: {
                return (data & 4) == 0 ? 0.8125 : 1.0;
            }
            case 127: {
                return 0.75;
            }
            case 140: {
                return 0.375;
            }
            case 144: {
                return 0.5;
            }
            case 151: {
                return 0.375;
            }
            case 154: {
                return 0.625;
            }
        }
        return 1.0;
    }

    public static CompletableFuture<ByteArrayDataInput> sendPluginMessage(String channel, String ... data) {
        return Utils.sendPluginMessage(channel, (ByteArrayDataInput r) -> true, data);
    }

    public static CompletableFuture<ByteArrayDataInput> sendPluginMessage(Player player, String channel, String ... data) {
        return Utils.sendPluginMessage(player, channel, (ByteArrayDataInput r) -> true, data);
    }

    public static CompletableFuture<ByteArrayDataInput> sendPluginMessage(String channel, Predicate<ByteArrayDataInput> messageVerifier, String ... data) throws IllegalStateException {
        Player firstPlayer = (Player)Iterables.getFirst((Iterable)Bukkit.getOnlinePlayers(), null);
        if (firstPlayer == null) {
            throw new IllegalStateException("There are no players online");
        }
        return Utils.sendPluginMessage(firstPlayer, channel, messageVerifier, data);
    }

    public static CompletableFuture<ByteArrayDataInput> sendPluginMessage(Player player, String channel, Predicate<ByteArrayDataInput> messageVerifier, String ... data) {
        CompletableFuture<ByteArrayDataInput> completableFuture = new CompletableFuture<ByteArrayDataInput>();
        Skript skript = Skript.getInstance();
        Messenger messenger = Bukkit.getMessenger();
        messenger.registerOutgoingPluginChannel((Plugin)skript, channel);
        PluginMessageListener listener = (sendingChannel, sendingPlayer, message) -> {
            ByteArrayDataInput input = ByteStreams.newDataInput((byte[])message);
            if (channel.equals(sendingChannel) && sendingPlayer == player && !completableFuture.isDone() && !completableFuture.isCancelled() && messageVerifier.test(input)) {
                completableFuture.complete(input);
            }
        };
        messenger.registerIncomingPluginChannel((Plugin)skript, channel, listener);
        completableFuture.whenComplete((r, ex) -> messenger.unregisterIncomingPluginChannel((Plugin)skript, channel, listener));
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)skript, () -> {
            if (!completableFuture.isDone()) {
                completableFuture.cancel(true);
            }
        }, 1200L);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        Stream.of(data).forEach(arg_0 -> ((ByteArrayDataOutput)out).writeUTF(arg_0));
        player.sendPluginMessage((Plugin)Skript.getInstance(), channel, out.toByteArray());
        return completableFuture;
    }

    @Deprecated(since="2.15", forRemoval=true)
    @Nullable
    public static String getChatStyle(String s) {
        SkriptColor color = SkriptColor.fromName(s);
        if (color != null) {
            return color.getFormattedChat();
        }
        return chat.get(s);
    }

    @Deprecated(since="2.15", forRemoval=true)
    @NotNull
    public static String replaceChatStyles(String message) {
        if (message.isEmpty()) {
            return message;
        }
        return Utils.replaceChatStyle(message.replace("<<none>>", ""));
    }

    @Deprecated(since="2.15", forRemoval=true)
    @NotNull
    public static String replaceEnglishChatStyles(String message) {
        if (message.isEmpty()) {
            return message;
        }
        return Utils.replaceChatStyle(message);
    }

    @Deprecated(since="2.15", forRemoval=true)
    @NotNull
    private static String replaceChatStyle(String message) {
        String m = StringUtils.replaceAll((CharSequence)Matcher.quoteReplacement(message), STYLE_PATTERN, matcher -> {
            String character;
            SkriptColor color = SkriptColor.fromName(matcher.group(1));
            if (color != null) {
                return color.getFormattedChat();
            }
            String tag = matcher.group(1).toLowerCase(Locale.ENGLISH);
            String f = englishChat.get(tag);
            if (f != null) {
                return f;
            }
            if (tag.startsWith("#")) {
                ChatColor chatColor = Utils.parseHexColor(tag);
                if (chatColor != null) {
                    return chatColor.toString();
                }
            } else if ((tag.startsWith("u:") || tag.startsWith("unicode:")) && (character = Utils.parseUnicode(tag)) != null) {
                return character;
            }
            return matcher.group();
        });
        if (!message.equals(m)) {
            m = m.replace("\\$", "$").replace("\\\\", "\\");
        }
        return ChatColor.translateAlternateColorCodes((char)'&', (String)m);
    }

    @Deprecated(since="2.15", forRemoval=true)
    @Nullable
    public static String parseUnicode(String string) {
        Matcher matcher = UNICODE_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Character.toString(Integer.parseInt(matcher.group("code"), 16));
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Deprecated(since="2.15", forRemoval=true)
    @Nullable
    public static ChatColor parseHexColor(String string) {
        Matcher matcher = HEX_PATTERN.matcher(string);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return ChatColor.of((String)("#" + matcher.group("code")));
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static int random(int start, int end) {
        if (end <= start) {
            throw new IllegalArgumentException("end (" + end + ") must be > start (" + start + ")");
        }
        return start + random.nextInt(end - start);
    }

    public static Class<?> getSuperType(Class<?> ... classes) {
        return Utils.highestDenominator(Object.class, classes);
    }

    @SafeVarargs
    public static <Found, Type extends Found> Class<Found> highestDenominator(Class<? super Found> bestGuess, Class<? extends Type> ... classes) {
        assert (classes.length > 0);
        Class<Object> chosen = classes[0];
        block0: for (Class<Type> clazz : classes) {
            assert (!clazz.isArray() && !clazz.isPrimitive()) : "%s has no super".formatted(clazz.getSimpleName());
            if (chosen.isAssignableFrom(clazz)) continue;
            Class<Object> superType = clazz;
            do {
                if (superType == Object.class || !superType.isAssignableFrom(chosen)) continue;
                chosen = superType;
                continue block0;
            } while ((superType = superType.getSuperclass()) != null);
            for (Class<?> anInterface : clazz.getInterfaces()) {
                superType = Utils.highestDenominator(Object.class, anInterface, chosen);
                if (superType == Object.class) continue;
                chosen = superType;
                continue block0;
            }
            return bestGuess;
        }
        if (!bestGuess.isAssignableFrom(chosen)) {
            return bestGuess;
        }
        return chosen == Cloneable.class ? bestGuess : (chosen == Object.class ? bestGuess : chosen);
    }

    public static int parseInt(String s) {
        assert (s.matches("-?\\d+"));
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return s.startsWith("-") ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
    }

    public static long parseLong(String s) {
        assert (s.matches("-?\\d+"));
        try {
            return Long.parseLong(s);
        }
        catch (NumberFormatException e) {
            return s.startsWith("-") ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
    }

    public static Class<?> classForName(String name) {
        try {
            Class<?> c = Class.forName(name);
            return c;
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found!");
        }
    }

    public static <T> int findLastIndex(List<T> list, Predicate<T> checker) {
        int lastIndex = -1;
        for (int i = 0; i < list.size(); ++i) {
            if (!checker.test(list.get(i))) continue;
            lastIndex = i;
        }
        return lastIndex;
    }

    public static boolean isInteger(Number ... numbers) {
        for (Number number : numbers) {
            if (!Double.class.isAssignableFrom(number.getClass()) && !Float.class.isAssignableFrom(number.getClass())) continue;
            return false;
        }
        return true;
    }

    @ApiStatus.Internal
    public static int loadedRemovedClassWarning(Class<?> source) {
        String authors;
        String name;
        Logger logger = Skript.getInstance().getLogger();
        Exception exception = new Exception();
        exception.fillInStackTrace();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StackTraceElement caller = stackTrace[2];
        try {
            Class<?> callingClass = Class.forName(caller.getClassName());
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(callingClass);
            name = plugin.getDescription().getFullName();
            authors = String.valueOf(plugin.getDescription().getAuthors());
        }
        catch (ClassCastException | ClassNotFoundException | IllegalArgumentException error) {
            name = caller.getClassLoaderName();
            authors = "(unknown)";
        }
        logger.log(Level.SEVERE, String.format("\n\nWARNING!\n\nAn addon attempted to load a deprecated/outdated/removed '%s' class.\n\nThe plugin '%s' tried to use a class that has been deprecated/removed in this version of Skript.\nPlease make sure you are using the latest supported version of the addon.\n\nIf there are no supported versions, you should contact the author(s): %s, and ask them to update it.\n\n(This addon may not work correctly on this version of Skript.)\n\n", source.getSimpleName(), name, authors));
        return 0;
    }

    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.length() != 36) {
            return false;
        }
        if (uuid.charAt(8) != '-' || uuid.charAt(13) != '-' || uuid.charAt(18) != '-' || uuid.charAt(23) != '-') {
            return false;
        }
        for (int i = 0; i < 36; ++i) {
            char c;
            if (i == 8 || i == 13 || i == 18 || i == 23 || (c = uuid.charAt(i)) >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') continue;
            return false;
        }
        return true;
    }

    public static Class<?> getComponentType(Class<?> cls) {
        if (cls != null && cls.isArray()) {
            return cls.componentType();
        }
        return cls;
    }

    static {
        plurals.add(new WordEnding("axe", "axes"));
        plurals.add(new WordEnding("x", "xes"));
        plurals.add(new WordEnding("ay", "ays"));
        plurals.add(new WordEnding("ey", "eys"));
        plurals.add(new WordEnding("iy", "iys"));
        plurals.add(new WordEnding("oy", "oys"));
        plurals.add(new WordEnding("uy", "uys"));
        plurals.add(new WordEnding("kie", "kies"));
        plurals.add(new WordEnding("zombie", "zombies", true));
        plurals.add(new WordEnding("y", "ies"));
        plurals.add(new WordEnding("wife", "wives", true));
        plurals.add(new WordEnding("life", "lives"));
        plurals.add(new WordEnding("knife", "knives", true));
        plurals.add(new WordEnding("ive", "ives"));
        plurals.add(new WordEnding("lf", "lves"));
        plurals.add(new WordEnding("thief", "thieves", true));
        plurals.add(new WordEnding("ief", "iefs"));
        plurals.add(new WordEnding("hoof", "hooves"));
        plurals.add(new WordEnding("fe", "ves"));
        plurals.add(new WordEnding("h", "hes"));
        plurals.add(new WordEnding("man", "men"));
        plurals.add(new WordEnding("ui", "uis"));
        plurals.add(new WordEnding("api", "apis"));
        plurals.add(new WordEnding("us", "i"));
        plurals.add(new WordEnding("hoe", "hoes", true));
        plurals.add(new WordEnding("toe", "toes", true));
        plurals.add(new WordEnding("foe", "foes", true));
        plurals.add(new WordEnding("woe", "woes", true));
        plurals.add(new WordEnding("o", "oes"));
        plurals.add(new WordEnding("alias", "aliases", true));
        plurals.add(new WordEnding("gas", "gases", true));
        plurals.add(new WordEnding("child", "children"));
        plurals.add(new WordEnding("sheep", "sheep", true));
        plurals.add(new WordEnding("", "s"));
        styles = new ChatColor[]{ChatColor.BOLD, ChatColor.ITALIC, ChatColor.STRIKETHROUGH, ChatColor.UNDERLINE, ChatColor.MAGIC, ChatColor.RESET};
        chat = new HashMap<String, String>();
        englishChat = new HashMap<String, String>();
        Language.addListener(() -> {
            boolean english = englishChat.isEmpty();
            chat.clear();
            for (ChatColor style : styles) {
                for (String s : Language.getList("chat styles." + style.name())) {
                    chat.put(s.toLowerCase(Locale.ENGLISH), style.toString());
                    if (!english) continue;
                    englishChat.put(s.toLowerCase(Locale.ENGLISH), style.toString());
                }
            }
        });
        STYLE_PATTERN = Pattern.compile("<([^<>]+)>");
        UNICODE_PATTERN = Pattern.compile("(?i)u(?:nicode)?:(?<code>[0-9a-f]{4,})");
        HEX_PATTERN = Pattern.compile("(?i)#{0,2}(?<code>[0-9a-f]{6})");
    }

    public record PluralResult(String updated, boolean plural) {
    }

    protected record WordEnding(String singular, String plural, boolean isCompleteWord) {
        public WordEnding(String singular, String plural) {
            this(singular, plural, false);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof WordEnding)) {
                return false;
            }
            WordEnding ending = (WordEnding)object;
            return Objects.equals(this.singular, ending.singular) && Objects.equals(this.plural, ending.plural);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.singular, this.plural);
        }
    }
}

