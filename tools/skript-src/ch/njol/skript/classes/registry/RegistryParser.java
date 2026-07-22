/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes.registry;

import ch.njol.skript.classes.PatternedParser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.util.NonNullPair;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RegistryParser<R extends Keyed>
extends PatternedParser<R> {
    private final Registry<R> registry;
    private final String languageNode;
    private final Map<R, String> names = new HashMap<R, String>();
    private final Map<String, R> parseMap = new HashMap<String, R>();
    private String[] patterns;

    public RegistryParser(Registry<R> registry, String languageNode) {
        assert (!languageNode.isEmpty() && !languageNode.endsWith(".")) : languageNode;
        this.registry = registry;
        this.languageNode = languageNode;
        this.refresh();
        Language.addListener(this::refresh);
    }

    private void refresh() {
        this.names.clear();
        this.parseMap.clear();
        for (Keyed registryObject : this.registry) {
            Object languageKey;
            NamespacedKey namespacedKey = registryObject.getKey();
            String namespace = namespacedKey.getNamespace();
            String key = namespacedKey.getKey();
            String keyWithSpaces = key.replace("_", " ");
            this.parseMap.put(namespacedKey.toString(), registryObject);
            if (namespace.equalsIgnoreCase("minecraft")) {
                this.parseMap.put(keyWithSpaces, registryObject);
                languageKey = this.languageNode + "." + key;
            } else {
                languageKey = namespacedKey.toString();
            }
            String[] options = Language.getList((String)languageKey);
            if (options.length == 1 && options[0].equals(((String)languageKey).toLowerCase(Locale.ENGLISH))) {
                if (namespace.equalsIgnoreCase("minecraft")) {
                    this.names.put(registryObject, keyWithSpaces);
                    continue;
                }
                this.names.put(registryObject, namespacedKey.toString());
                continue;
            }
            for (String option : options) {
                option = option.toLowerCase(Locale.ENGLISH);
                NonNullPair<String, Integer> strippedOption = Noun.stripGender(option, (String)languageKey);
                String first = strippedOption.getFirst();
                Integer second = strippedOption.getSecond();
                this.names.putIfAbsent(registryObject, first);
                this.parseMap.put(first, registryObject);
                if (second == -1) continue;
                this.parseMap.put(Noun.getArticleWithSpace(second, 4) + first, registryObject);
            }
        }
        this.patterns = (String[])this.parseMap.keySet().stream().filter(pattern -> !pattern.startsWith("minecraft:")).sorted().toArray(String[]::new);
    }

    @Override
    @Nullable
    public R parse(String input, @NotNull ParseContext context) {
        return (R)((Keyed)this.parseMap.get(input.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    @NotNull
    public String toString(R object, int flags) {
        return this.names.get(object);
    }

    @Override
    @NotNull
    public String toVariableNameString(R object) {
        return this.toString(object, 0);
    }

    @Override
    public String[] getPatterns() {
        return this.patterns;
    }
}

