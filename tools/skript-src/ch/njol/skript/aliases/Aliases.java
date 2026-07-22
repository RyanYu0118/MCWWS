/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.aliases;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.aliases.AliasesParser;
import ch.njol.skript.aliases.AliasesProvider;
import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.aliases.MaterialName;
import ch.njol.skript.aliases.ScriptAliases;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.localization.RegexMessage;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.Version;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

public abstract class Aliases {
    private static final AliasesProvider provider = Aliases.createProvider(10000, null);
    private static final AliasesParser parser = Aliases.createParser(provider);
    private static final ItemType everything = new ItemType();
    private static final Message m_empty_string = new Message("aliases.empty string");
    private static final ArgsMessage m_invalid_item_type = new ArgsMessage("aliases.invalid item type");
    private static final Message m_outside_section = new Message("aliases.outside section");
    private static final RegexMessage p_any = new RegexMessage("aliases.any", "", " (.+)", 2);
    private static final RegexMessage p_every = new RegexMessage("aliases.every", "", " (.+)", 2);
    private static final RegexMessage p_of_every = new RegexMessage("aliases.of every", "(\\d+) ", " (.+)", 2);
    private static final RegexMessage p_of = new RegexMessage("aliases.of", "(\\d+) (?:", " )?(.+)", 2);
    private static final Map<String, ItemType> trackedTypes = new HashMap<String, ItemType>();
    private static final boolean noHardExceptions = SkriptConfig.apiSoftExceptions.value();
    static String itemSingular = "item";
    static String itemPlural = "items";
    static String blockSingular = "block";
    static String blockPlural = "blocks";

    @Nullable
    private static ItemType getAlias_i(String s) {
        ScriptAliases aliases = Aliases.getScriptAliases();
        if (aliases != null) {
            return aliases.provider.getAlias(s);
        }
        return provider.getAlias(s);
    }

    private static AliasesProvider createProvider(int expectedCount, @Nullable AliasesProvider parent) {
        return new AliasesProvider(expectedCount, parent);
    }

    private static AliasesParser createParser(AliasesProvider provider) {
        AliasesParser parser = new AliasesParser(provider);
        parser.registerCondition("minecraft version", str -> {
            int orNewer = str.indexOf("or newer");
            if (orNewer != -1) {
                Version ver = new Version(str.substring(0, orNewer - 1));
                return Skript.getMinecraftVersion().compareTo(ver) >= 0;
            }
            int orOlder = str.indexOf("or older");
            if (orOlder != -1) {
                Version ver = new Version(str.substring(0, orOlder - 1));
                return Skript.getMinecraftVersion().compareTo(ver) <= 0;
            }
            int to = str.indexOf("to");
            if (to != -1) {
                Version first = new Version(str.substring(0, to - 1));
                Version second = new Version(str.substring(to + 3));
                Version current = Skript.getMinecraftVersion();
                return current.compareTo(first) >= 0 && current.compareTo(second) <= 0;
            }
            return Skript.getMinecraftVersion().equals(new Version((String)str));
        });
        return parser;
    }

    static String concatenate(String ... parts) {
        assert (parts.length >= 2);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; ++i) {
            if (parts[i].isEmpty()) continue;
            if (b.length() == 0) {
                b.append(parts[i]);
                continue;
            }
            char c = parts[i].charAt(0);
            if (Character.isUpperCase(c) && b.charAt(b.length() - 1) != ' ') {
                b.append(Character.toLowerCase(c) + parts[i].substring(1));
                continue;
            }
            b.append(parts[i]);
        }
        return b.toString().replace("  ", " ").trim();
    }

    @Nullable
    private static MaterialName getMaterialNameData(ItemData type) {
        ScriptAliases aliases = Aliases.getScriptAliases();
        if (aliases != null) {
            return aliases.provider.getMaterialName(type);
        }
        return provider.getMaterialName(type);
    }

    public static String getMaterialName(ItemData type, boolean plural) {
        MaterialName name = Aliases.getMaterialNameData(type);
        if (name == null) {
            return String.valueOf(type.type);
        }
        return name.toString(plural);
    }

    public static int getGender(ItemData item) {
        MaterialName n = Aliases.getMaterialNameData(item);
        if (n != null) {
            return n.gender;
        }
        return -1;
    }

    @Nullable
    public static ItemType parseAlias(String s) {
        String[] types;
        if (s.isEmpty()) {
            Skript.error(m_empty_string.toString());
            return null;
        }
        if (s.equals("*")) {
            return everything;
        }
        ItemType t = new ItemType();
        for (String type : types = s.split("\\s*,\\s*")) {
            if (type != null && Aliases.parseType(type, t, true) != null) continue;
            return null;
        }
        return t;
    }

    @Nullable
    public static ItemType parseItemType(String s) {
        if (((String)s).isEmpty()) {
            return null;
        }
        s = ((String)s).trim();
        ItemType t = new ItemType();
        Matcher m = p_of_every.matcher((String)s);
        if (m.matches()) {
            t.setAmount(Utils.parseInt(m.group(1)));
            t.setAll(true);
            s = m.group(m.groupCount());
        } else {
            m = p_of.matcher((String)s);
            if (m.matches()) {
                t.setAmount(Utils.parseInt(m.group(1)));
                s = m.group(m.groupCount());
            } else {
                m = p_every.matcher((String)s);
                if (m.matches()) {
                    t.setAll(true);
                    s = m.group(m.groupCount());
                } else {
                    int l = ((String)s).length();
                    if (((String)(s = Noun.stripIndefiniteArticle((String)s))).length() != l) {
                        t.setAmount(1);
                    }
                }
            }
        }
        String lc = ((String)s).toLowerCase(Locale.ENGLISH);
        String of = Language.getSpaced("of").toLowerCase();
        int c = -1;
        block5: while ((c = lc.indexOf(of, c + 1)) != -1) {
            String[] enchs;
            ItemType t2 = t.clone();
            BlockingLogHandler ignored = new BlockingLogHandler().start();
            try {
                if (Aliases.parseType(((String)s).substring(0, c), t2, false) == null) {
                    continue;
                }
            }
            finally {
                if (ignored == null) continue;
                ignored.close();
                continue;
            }
            if (t2.numTypes() == 0) continue;
            for (String ench : enchs = lc.substring(c + of.length()).split("\\s*(,|" + Pattern.quote(Language.get("and")) + ")\\s*")) {
                EnchantmentType e = EnchantmentType.parse(ench);
                if (e == null) continue block5;
                t2.addEnchantments(e);
            }
            return t2;
        }
        if (Aliases.parseType((String)s, t, false) == null) {
            return null;
        }
        if (t.numTypes() == 0) {
            return null;
        }
        return t;
    }

    @Nullable
    private static ItemType parseType(String s, ItemType t, boolean isAlias) {
        if (s.isEmpty()) {
            t.add(new ItemData(Material.AIR));
            return t;
        }
        if (s.matches("\\d+")) {
            return null;
        }
        ItemType i = Aliases.getAlias(s);
        if (i != null) {
            for (ItemData d : i) {
                t.add(d.clone());
            }
            return t;
        }
        if (isAlias) {
            Skript.error(m_invalid_item_type.toString(s));
        }
        return null;
    }

    @Nullable
    private static ItemType getAlias(String rawInput) {
        String stripped;
        Material material;
        NamespacedKey namespacedKey;
        String input = rawInput.toLowerCase(Locale.ENGLISH).trim();
        ItemType itemType = Aliases.getAlias_i(input);
        if (itemType != null) {
            return itemType.clone();
        }
        if ((input.contains(":") || input.contains("_")) && !input.contains(" ") && (namespacedKey = NamespacedKey.fromString((String)input)) != null && (material = (Material)Registry.MATERIAL.get(namespacedKey)) != null) {
            return new ItemType(material);
        }
        if (input.endsWith(" " + blockSingular) || input.endsWith(" " + blockPlural)) {
            stripped = input.substring(0, input.lastIndexOf(" "));
            itemType = Aliases.getAlias_i(stripped);
            if (itemType != null) {
                itemType = itemType.clone();
                for (int j = 0; j < itemType.numTypes(); ++j) {
                    ItemData d = itemType.getTypes().get(j);
                    if (d.getType().isBlock() && !d.getType().getKey().getKey().endsWith(blockSingular)) continue;
                    itemType.remove(d);
                    --j;
                }
                if (itemType.getTypes().isEmpty()) {
                    return null;
                }
                return itemType;
            }
        } else if ((input.endsWith(" " + itemSingular) || input.endsWith(" " + itemPlural)) && (itemType = Aliases.getAlias_i(stripped = input.substring(0, input.lastIndexOf(" ")))) != null) {
            itemType = itemType.clone();
            for (int j = 0; j < itemType.numTypes(); ++j) {
                ItemData data = itemType.getTypes().get(j);
                if (data.isAnything || data.getType().isItem()) continue;
                itemType.remove(data);
                --j;
            }
            if (itemType.getTypes().isEmpty()) {
                return null;
            }
            return itemType;
        }
        return null;
    }

    public static void clear() {
        provider.clearAliases();
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static void load() {
        try {
            long start = System.currentTimeMillis();
            Aliases.loadInternal();
            Skript.info("Loaded " + provider.getAliasCount() + " aliases in " + (System.currentTimeMillis() - start) + "ms");
        }
        catch (IOException e) {
            Skript.exception((Throwable)e, new String[0]);
        }
    }

    public static CompletableFuture<Boolean> loadAsync() {
        ItemUtils.applyComponentData(ItemStack.empty(), "[]");
        return CompletableFuture.supplyAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                Aliases.loadInternal();
                Skript.info("Loaded " + provider.getAliasCount() + " aliases in " + (System.currentTimeMillis() - start) + "ms");
                return true;
            }
            catch (StackOverflowError e) {
                if (System.getProperty("java.vm.name").contains("32")) {
                    Skript.error("");
                    Skript.error("There was a StackOverflowError that occurred while loading aliases.");
                    Skript.error("As you are currently using 32-bit Java, please update to 64-bit Java to resolve the error.");
                    Skript.error("Please report this issue to our GitHub only if updating to 64-bit Java does not fix the issue.");
                    Skript.error("");
                } else {
                    Skript.exception((Throwable)e, new String[0]);
                    Bukkit.getPluginManager().disablePlugin((Plugin)Skript.getInstance());
                }
                return false;
            }
            catch (IOException e) {
                Skript.exception((Throwable)e, new String[0]);
                return false;
            }
        });
    }

    private static void loadMissingAliases() {
        if (!Skript.methodExists(Material.class, "getKey", new Class[0])) {
            return;
        }
        boolean modItemRegistered = false;
        for (Material material : Material.values()) {
            if (material.isLegacy() || provider.hasAliasForMaterial(material)) continue;
            NamespacedKey key = material.getKey();
            if ("minecraft".equals(key.getNamespace())) {
                parser.loadAlias(key.getKey().replace("_", " ") + "\u00a6s", key.toString());
            } else {
                if (!modItemRegistered) {
                    modItemRegistered = true;
                }
                parser.loadAlias((key.getNamespace() + "'s " + key.getKey() + "\u00a6s").replace("_", " "), key.toString());
                parser.loadAlias((key.getKey() + "\u00a6s from " + key.getNamespace()).replace("_", " "), key.toString());
            }
            Skript.debug(String.valueOf(ChatColor.YELLOW) + "Creating temporary alias for: " + String.valueOf(key));
        }
        if (modItemRegistered) {
            Skript.warning("==============================================================");
            Skript.warning("Some materials were found that seem to be modded.");
            Skript.warning("An item that has the id 'mod:item' can be used as 'mod's item' or 'item from mod'.");
            Skript.warning("WARNING: Skript does not officially support any modded servers.");
            Skript.warning("Any issues you encounter related to modded items will be your responsibility to fix.");
            Skript.warning("==============================================================");
        }
    }

    private static void loadInternal() throws IOException {
        Path aliasesFolder;
        Path dataFolder;
        block24: {
            dataFolder = Skript.getInstance().getDataFolder().toPath();
            Path zipPath = dataFolder.resolve("aliases-english.zip");
            if (SkriptConfig.loadDefaultAliases.value().booleanValue()) {
                if (Files.exists(zipPath, new LinkOption[0])) {
                    try (FileSystem zipFs = FileSystems.newFileSystem(zipPath, Skript.class.getClassLoader());){
                        assert (zipFs != null);
                        Path aliasesPath = zipFs.getPath("/", new String[0]);
                        assert (aliasesPath != null);
                        Aliases.loadDirectory(aliasesPath);
                    }
                }
                try {
                    URI jarUri = Skript.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                    try (FileSystem zipFs = FileSystems.newFileSystem(Paths.get(jarUri), Skript.class.getClassLoader());){
                        assert (zipFs != null);
                        Path aliasesPath = zipFs.getPath("/", "aliases-english");
                        assert (aliasesPath != null);
                        Aliases.loadDirectory(aliasesPath);
                    }
                }
                catch (URISyntaxException e) {
                    if ($assertionsDisabled) break block24;
                    throw new AssertionError();
                }
            }
        }
        if (Files.exists(aliasesFolder = dataFolder.resolve("aliases"), new LinkOption[0])) {
            assert (aliasesFolder != null);
            Aliases.loadDirectory(aliasesFolder);
        }
        Aliases.loadMissingAliases();
        for (Map.Entry<String, ItemType> entry : trackedTypes.entrySet()) {
            ItemType type = Aliases.parseItemType(entry.getKey());
            if (type == null) {
                Skript.warning("Alias '" + entry.getKey() + "' is required by Skript, but does not exist anymore. Make sure to fix this before restarting the server.");
                continue;
            }
            entry.getValue().setTo(type);
        }
    }

    public static void loadDirectory(Path dir) throws IOException {
        try {
            Files.list(dir).sorted().forEach(f -> {
                assert (f != null);
                try {
                    String name = f.getFileName().toString();
                    if (Files.isDirectory(f, new LinkOption[0]) && !name.startsWith(".")) {
                        Aliases.loadDirectory(f);
                    } else if (name.endsWith(".sk")) {
                        Aliases.load(f);
                    }
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public static void load(Path f) throws IOException {
        Config config = new Config(f, false, false, "=");
        Aliases.load(config);
    }

    public static void load(Config config) {
        for (Node n : config.getMainNode()) {
            if (!(n instanceof SectionNode)) {
                Skript.error(m_outside_section.toString());
                continue;
            }
            parser.load((SectionNode)n);
        }
    }

    @Nullable
    public static String getMinecraftId(ItemData data) {
        ScriptAliases aliases = Aliases.getScriptAliases();
        if (aliases != null) {
            return aliases.provider.getMinecraftId(data);
        }
        return provider.getMinecraftId(data);
    }

    @Nullable
    public static EntityData<?> getRelatedEntity(ItemData data) {
        ScriptAliases aliases = Aliases.getScriptAliases();
        if (aliases != null) {
            return aliases.provider.getRelatedEntity(data);
        }
        return provider.getRelatedEntity(data);
    }

    @Deprecated(since="2.9.0", forRemoval=true)
    public static ItemType javaItemType(String name) {
        ItemType type = Aliases.parseItemType(name);
        if (type == null) {
            if (noHardExceptions) {
                Skript.error("type " + name + " not found");
                type = new ItemType();
            } else {
                throw new IllegalArgumentException("type " + name + " not found");
            }
        }
        trackedTypes.put(name, type);
        return type;
    }

    public static AliasesProvider getAddonProvider(@Nullable SkriptAddon addon) {
        if (addon == null) {
            throw new IllegalArgumentException("addon needed");
        }
        return provider;
    }

    public static ScriptAliases createScriptAliases(Script script) {
        AliasesProvider localProvider = Aliases.createProvider(10, provider);
        ScriptAliases aliases = new ScriptAliases(localProvider, Aliases.createParser(localProvider));
        script.addData(aliases);
        return aliases;
    }

    @Nullable
    private static ScriptAliases getScriptAliases() {
        ParserInstance parser = ParserInstance.get();
        if (parser.isActive()) {
            return Aliases.getScriptAliases(parser.getCurrentScript());
        }
        return null;
    }

    @Nullable
    public static ScriptAliases getScriptAliases(Script script) {
        return script.getData(ScriptAliases.class);
    }

    static {
        everything.setAll(true);
        ItemData all = new ItemData(Material.AIR);
        all.isAnything = true;
        everything.add(all);
    }
}

