/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.util.StringMode;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import java.util.HashMap;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.12", forRemoval=true)
public final class EnumUtils<E extends Enum<E>> {
    private final Class<E> enumClass;
    private final String languageNode;
    private String[] names;
    private final HashMap<String, E> parseMap = new HashMap();

    public EnumUtils(Class<E> enumClass, String languageNode) {
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
                if (this.names[ordinal] == null) {
                    this.names[ordinal] = first;
                }
                this.parseMap.put(first, constant);
                if (second == -1) continue;
                this.parseMap.put(Noun.getArticleWithSpace(second, 4) + first, constant);
            }
        }
    }

    @Nullable
    public E parse(String input) {
        return (E)((Enum)this.parseMap.get(input.toLowerCase(Locale.ENGLISH)));
    }

    public String toString(E enumerator, int flags) {
        String s = this.names[((Enum)enumerator).ordinal()];
        return s != null ? s : ((Enum)enumerator).name();
    }

    public String toString(E enumerator, StringMode flag) {
        return this.toString(enumerator, flag.ordinal());
    }

    public String getAllNames() {
        return StringUtils.join(this.parseMap.keySet(), ", ");
    }
}

