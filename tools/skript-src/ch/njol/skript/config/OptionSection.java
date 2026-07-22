/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.config.Option;
import java.lang.reflect.Field;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public class OptionSection {
    public final String key;

    public OptionSection(String key) {
        this.key = key;
    }

    @Nullable
    public final <T> T get(String key) {
        if (this.getClass() == OptionSection.class) {
            return null;
        }
        key = ((String)key).toLowerCase(Locale.ENGLISH);
        for (Field f : this.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (!Option.class.isAssignableFrom(f.getType())) continue;
            try {
                Option o = (Option)f.get(this);
                if (!o.key.equals(key)) continue;
                return o.value();
            }
            catch (IllegalArgumentException e) {
                assert (false);
            }
            catch (IllegalAccessException e) {
                assert (false);
            }
        }
        return null;
    }
}

