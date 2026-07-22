/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.Skript;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

@Deprecated(since="2.12", forRemoval=true)
public class EnumParser<E extends Enum<E>>
implements Converter<String, E> {
    private final Class<E> enumType;
    @Nullable
    private final String allowedValues;
    private final String type;

    public EnumParser(Class<E> enumType, String type) {
        assert (enumType != null);
        this.enumType = enumType;
        this.type = type;
        if (((Enum[])enumType.getEnumConstants()).length <= 12) {
            StringBuilder b = new StringBuilder(((Enum[])enumType.getEnumConstants())[0].name());
            for (Enum e : (Enum[])enumType.getEnumConstants()) {
                if (b.length() != 0) {
                    b.append(", ");
                }
                b.append(e.name().toLowerCase(Locale.ENGLISH).replace('_', ' '));
            }
            this.allowedValues = b.toString();
        } else {
            this.allowedValues = null;
        }
    }

    @Override
    @Nullable
    public E convert(String s) {
        try {
            return Enum.valueOf(this.enumType, s.toUpperCase(Locale.ENGLISH).replace(' ', '_'));
        }
        catch (IllegalArgumentException e) {
            Skript.error("'" + s + "' is not a valid value for " + this.type + (String)(this.allowedValues == null ? "" : ". Allowed values are: " + this.allowedValues));
            return null;
        }
    }

    public String toString() {
        return "EnumParser{enum=" + String.valueOf(this.enumType) + ",allowedValues=" + this.allowedValues + ",type=" + this.type + "}";
    }
}

