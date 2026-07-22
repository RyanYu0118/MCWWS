/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.localization;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.localization.LanguageChangeListener;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.localization.Localizer;

public class Language {
    public static final int F_PLURAL = 1;
    public static final int F_DEFINITE_ARTICLE = 2;
    public static final int F_INDEFINITE_ARTICLE = 4;
    public static final int NO_ARTICLE_MASK = -7;
    private static String name = "english";
    private static final HashMap<String, String> defaultLanguage = new HashMap();
    @Nullable
    private static HashMap<String, String> localizedLanguage = null;
    private static final HashMap<String, Version> langVersion = new HashMap();
    private static final Pattern listSplitPattern = Pattern.compile("\\s*,\\s*");
    private static final List<LanguageChangeListener> listeners = new ArrayList<LanguageChangeListener>();
    private static final int[] priorityStartIndices = new int[LanguageListenerPriority.values().length];

    public static String getName() {
        return name;
    }

    @Nullable
    private static String get_i(String key) {
        String value = defaultLanguage.get(key);
        if (value != null) {
            return value;
        }
        if (localizedLanguage != null && (value = localizedLanguage.get(key)) != null) {
            return value;
        }
        if (Skript.testing()) {
            Language.missingEntryError(key);
        }
        return null;
    }

    public static String get(String key) {
        String s = Language.get_i(key.toLowerCase(Locale.ENGLISH));
        return s == null ? key.toLowerCase(Locale.ENGLISH) : s;
    }

    @Nullable
    public static String get_(String key) {
        return Language.get_i(key.toLowerCase(Locale.ENGLISH));
    }

    public static void missingEntryError(String key) {
        Skript.error("Missing entry '" + key.toLowerCase(Locale.ENGLISH) + "' in the default/english language file");
    }

    public static String format(String key, Object ... args) {
        String value = Language.get_i((String)(key = ((String)key).toLowerCase(Locale.ENGLISH)));
        if (value == null) {
            return key;
        }
        try {
            return String.format(value, args);
        }
        catch (Exception e) {
            Skript.error("Invalid format string at '" + (String)key + "' in the " + Language.getName() + " language file: " + value);
            return key;
        }
    }

    public static String getSpaced(String key) {
        String s = Language.get(key);
        if (s.isEmpty()) {
            return " ";
        }
        return " " + s + " ";
    }

    public static String[] getList(String key) {
        String s = Language.get_i(key.toLowerCase(Locale.ENGLISH));
        if (s == null) {
            return new String[]{key.toLowerCase(Locale.ENGLISH)};
        }
        String[] r = listSplitPattern.split(s);
        assert (r != null);
        return r;
    }

    public static boolean keyExists(String key) {
        return defaultLanguage.containsKey(key = key.toLowerCase(Locale.ENGLISH)) || localizedLanguage != null && localizedLanguage.containsKey(key);
    }

    public static boolean keyExistsDefault(String key) {
        return defaultLanguage.containsKey(key.toLowerCase(Locale.ENGLISH));
    }

    public static boolean isInitialized() {
        return !defaultLanguage.isEmpty();
    }

    @Nullable
    private static String getSanitizedLanguageDirectory(SkriptAddon addon) {
        Localizer localizer = addon.localizer();
        if (localizer == null) {
            return null;
        }
        String languageFileDirectory = localizer.languageFileDirectory();
        if (languageFileDirectory == null) {
            return null;
        }
        if ((languageFileDirectory = languageFileDirectory.replace('\\', '/')).startsWith("/")) {
            languageFileDirectory = languageFileDirectory.substring(1);
        }
        if (languageFileDirectory.endsWith("/")) {
            languageFileDirectory = languageFileDirectory.substring(0, languageFileDirectory.length() - 1);
        }
        return languageFileDirectory;
    }

    public static void loadDefault(SkriptAddon addon) {
        String languageFileDirectory = Language.getSanitizedLanguageDirectory(addon);
        if (languageFileDirectory == null) {
            return;
        }
        Class<?> source = addon.source();
        assert (source != null);
        try (InputStream defaultIs = source.getResourceAsStream("/" + languageFileDirectory + "/default.lang");
             InputStream englishIs = source.getResourceAsStream("/" + languageFileDirectory + "/english.lang");){
            InputStream defaultLangIs = defaultIs;
            InputStream englishLangIs = englishIs;
            if (defaultLangIs == null) {
                if (englishLangIs == null) {
                    throw new IllegalStateException(String.valueOf(addon) + " is missing the required default.lang file!");
                }
                defaultLangIs = englishLangIs;
                englishLangIs = null;
            }
            Map<String, String> def = Language.load(defaultLangIs, "default", false);
            Map<String, String> en = Language.load(englishLangIs, "english", addon instanceof org.skriptlang.skript.Skript);
            String v = def.get("version");
            if (v == null) {
                Skript.warning("Missing version in default.lang");
            }
            langVersion.put(addon.name(), v == null ? Skript.getVersion() : new Version(v));
            def.remove("version");
            defaultLanguage.putAll(def);
            if (localizedLanguage == null) {
                localizedLanguage = new HashMap();
            }
            localizedLanguage.putAll(en);
            for (LanguageChangeListener l : listeners) {
                l.onLanguageChange();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean load(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        localizedLanguage = new HashMap();
        boolean exists = Language.load(Skript.instance(), name, true);
        for (SkriptAddon addon : Skript.instance().addons()) {
            exists |= Language.load(addon, name, false);
        }
        if (!exists) {
            if (name.equals("english")) {
                throw new RuntimeException("English language is missing (english.lang)");
            }
            Language.load("english");
            return false;
        }
        Language.name = name;
        for (LanguageChangeListener l : listeners) {
            l.onLanguageChange();
        }
        return true;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static boolean load(SkriptAddon addon, String name, boolean tryUpdate) {
        Map<String, String> l;
        block35: {
            String languageFileDirectory = Language.getSanitizedLanguageDirectory(addon);
            if (languageFileDirectory == null) {
                return false;
            }
            Class<?> source = addon.source();
            if (name.equals("english")) {
                try (InputStream is = source.getResourceAsStream("/" + languageFileDirectory + "/default.lang");){
                    if (is == null) {
                        boolean bl = true;
                        return bl;
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            try (InputStream is = source.getResourceAsStream("/" + languageFileDirectory + "/" + name + ".lang");){
                l = Language.load(is, name, tryUpdate);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            String dataFileDirectory = addon.localizer().dataFileDirectory();
            if (dataFileDirectory != null) {
                File file = new File(dataFileDirectory, File.separator + name + ".lang");
                try {
                    if (file.exists()) {
                        l.putAll(Language.load(new FileInputStream(file), name, tryUpdate));
                    }
                }
                catch (FileNotFoundException e) {
                    if ($assertionsDisabled) break block35;
                    throw new AssertionError();
                }
            }
        }
        if (l.isEmpty()) {
            return false;
        }
        if (!l.containsKey("version")) {
            Skript.error(String.valueOf(addon) + "'s language file " + name + ".lang does not provide a version number!");
        } else {
            try {
                Version v = new Version(l.get("version"));
                Version lv = langVersion.get(addon.name());
                assert (lv != null);
                if (v.isSmallerThan(lv)) {
                    Skript.warning(String.valueOf(addon) + "'s language file " + name + ".lang is outdated, some messages will be english.");
                }
            }
            catch (IllegalArgumentException e) {
                Skript.error("Illegal version syntax in " + String.valueOf(addon) + "'s language file " + name + ".lang: " + e.getLocalizedMessage());
            }
        }
        l.remove("version");
        if (localizedLanguage == null) {
            assert (false) : String.valueOf(addon) + "; " + name;
            return true;
        }
        Iterator<Map.Entry<String, String>> iterator = l.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();
            if (defaultLanguage.containsKey(key)) {
                Skript.warning("'" + key + "' is part of the default language file, and can therefore not be modified in a localized language file.");
                continue;
            }
            localizedLanguage.put(key, value);
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Map<String, String> load(@Nullable InputStream in, String name, boolean tryUpdate) {
        Map<String, String> map;
        if (in == null) {
            return new HashMap<String, String>();
        }
        try {
            Config langConfig = new Config(in, name + ".lang", false, false, ":");
            String langVersion = langConfig.getValue("version");
            if (tryUpdate && (langVersion == null || Skript.getVersion().compareTo(new Version(langVersion)) != 0)) {
                String langFileName = "lang/" + name + ".lang";
                InputStream newConfigIn = Skript.getInstance().getResource(langFileName);
                if (newConfigIn == null) {
                    Skript.error("The lang file '" + name + ".lang' is outdated, but Skript couldn't find the newest version of it in its jar.");
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    return hashMap;
                }
                Config newLangConfig = new Config(newConfigIn, "Skript.jar/" + langFileName, false, false, ":");
                newConfigIn.close();
                File langFile = new File(Skript.getInstance().getDataFolder(), langFileName);
                if (!newLangConfig.compareValues(langConfig, "version")) {
                    File langFileBackup = FileUtils.backup(langFile);
                    newLangConfig.getMainNode().set("version", Skript.getVersion().toString());
                    langConfig = newLangConfig;
                    langConfig.save(langFile);
                    Skript.info("The lang file '" + name + ".lang' has been updated to the latest version. A backup of your old lang file has been created as " + langFileBackup.getName());
                } else {
                    langConfig.getMainNode().set("version", Skript.getVersion().toString());
                    langConfig.save(langFile);
                }
            }
            map = langConfig.toMap(".");
        }
        catch (IOException e) {
            Skript.exception((Throwable)e, "Could not load the language file '" + name + ".lang': " + ExceptionUtils.toString(e));
            HashMap<String, String> hashMap = new HashMap<String, String>();
            return hashMap;
        }
        finally {
            try {
                in.close();
            }
            catch (IOException iOException) {}
        }
        return map;
    }

    public static void addListener(LanguageChangeListener listener) {
        Language.addListener(listener, LanguageListenerPriority.NORMAL);
    }

    public static void addListener(LanguageChangeListener listener, LanguageListenerPriority priority) {
        listeners.add(priorityStartIndices[priority.ordinal()], listener);
        int i = priority.ordinal() + 1;
        while (i < LanguageListenerPriority.values().length) {
            int n = i++;
            priorityStartIndices[n] = priorityStartIndices[n] + 1;
        }
        if (Language.isInitialized()) {
            listener.onLanguageChange();
        }
    }

    public static enum LanguageListenerPriority {
        EARLIEST,
        NORMAL,
        LATEST;

    }
}

