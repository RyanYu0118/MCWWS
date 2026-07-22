/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.PatternedParser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.util.NonNullPair;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

public class EnumParser<E extends Enum<E>>
extends PatternedParser<E>
implements Converter<String, E> {
    private final Class<E> enumClass;
    private final String languageNode;
    private String[] names;
    protected final Map<String, E> parseMap = new HashMap<String, E>();
    private String[] patterns;

    public EnumParser(Class<E> enumClass, String languageNode) {
        assert (enumClass.isEnum()) : enumClass;
        assert (!languageNode.isEmpty() && !languageNode.endsWith(".")) : languageNode;
        this.enumClass = enumClass;
        this.languageNode = languageNode;
        this.refresh();
        Language.addListener(this::refresh);
    }

    void refresh() {
        Enum[] constants = (Enum[])this.enumClass.getEnumConstants();
        this.names = new String[constants.length];
        this.parseMap.clear();
        for (Enum constant : constants) {
            String[] options;
            String key = this.languageNode + "." + constant.name();
            int ordinal = constant.ordinal();
            for (String option : options = Language.getList(key)) {
                option = option.toLowerCase(Locale.ENGLISH);
                if (options.length == 1 && option.equals(key.toLowerCase(Locale.ENGLISH))) {
                    String[] splitKey = key.split("\\.");
                    String newKey = splitKey[1].replace('_', ' ').toLowerCase(Locale.ENGLISH) + " " + splitKey[0];
                    this.parseMap.put(newKey, constant);
                    Skript.debug("Missing lang enum constant for '" + key + "'. Using '" + newKey + "' for now.");
                    continue;
                }
                NonNullPair<String, Integer> strippedOption = Noun.stripGender(option, key);
                String first = strippedOption.getFirst();
                Integer second = strippedOption.getSecond();
                Noun.PluralPair singlePlural = Noun.parsePlural(first);
                String single = singlePlural.singular();
                String plural = singlePlural.plural();
                if (this.names[ordinal] == null) {
                    this.names[ordinal] = single;
                }
                this.parseMap.put(single, constant);
                if (!plural.isEmpty()) {
                    this.parseMap.put(plural, constant);
                }
                if (second == -1) continue;
                this.parseMap.put(Noun.getArticleWithSpace(second, 4) + single, constant);
            }
        }
        this.patterns = (String[])this.parseMap.keySet().toArray(String[]::new);
    }

    @Override
    @Nullable
    public E parse(String string, ParseContext context) {
        return (E)((Enum)this.parseMap.get(string.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    @Nullable
    public E convert(String string) {
        return (E)this.parse(string, ParseContext.DEFAULT);
    }

    @Override
    public String toVariableNameString(E object) {
        return this.toString(object, 0);
    }

    @Override
    public String[] getPatterns() {
        return this.patterns;
    }

    @Override
    public String toString(E object, int flags) {
        String name = this.names[((Enum)object).ordinal()];
        return name != null ? name : ((Enum)object).name();
    }
}

