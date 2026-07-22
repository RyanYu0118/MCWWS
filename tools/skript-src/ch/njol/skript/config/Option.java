/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.config.Config;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import java.util.Locale;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

public class Option<T> {
    public final String key;
    private boolean optional = false;
    @Nullable
    private String value = null;
    private final Converter<String, ? extends T> parser;
    private final T defaultValue;
    private T parsedValue;
    @Nullable
    private Consumer<? super T> setter;

    public Option(String key, T defaultValue) {
        this.key = key.toLowerCase(Locale.ENGLISH);
        this.defaultValue = defaultValue;
        this.parsedValue = defaultValue;
        Class<?> c = defaultValue.getClass();
        if (c == String.class) {
            this.parser = s -> s;
        } else {
            Parser<?> p;
            final ClassInfo<?> ci = Classes.getExactClassInfo(c);
            if (ci == null || (p = ci.getParser()) == null) {
                throw new IllegalArgumentException(c.getName());
            }
            this.parser = new Converter<String, T>(){

                @Override
                @Nullable
                public T convert(String s) {
                    Object t = p.parse(s, ParseContext.CONFIG);
                    if (t != null) {
                        return t;
                    }
                    Skript.error("'" + s + "' is not " + ci.getName().withIndefiniteArticle());
                    return null;
                }
            };
        }
    }

    public Option(String key, T defaultValue, Converter<String, ? extends T> parser) {
        this.key = key.toLowerCase(Locale.ENGLISH);
        this.defaultValue = defaultValue;
        this.parsedValue = defaultValue;
        this.parser = parser;
    }

    public final Option<T> setter(Consumer<? super T> setter) {
        this.setter = setter;
        return this;
    }

    public final Option<T> optional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public final void set(Config config, String path) {
        String oldValue = this.value;
        this.value = config.getByPath(path + this.key);
        if (this.value == null && !this.optional) {
            Skript.error("Required entry '" + path + this.key + "' is missing in " + config.getFileName() + ". Please make sure that you have the latest version of the config.");
        }
        if (this.value == null ^ oldValue == null || this.value != null && !this.value.equals(oldValue)) {
            T parsedValue;
            T t = parsedValue = this.value != null ? this.parser.convert(this.value) : this.defaultValue;
            if (parsedValue == null) {
                parsedValue = this.defaultValue;
            }
            this.parsedValue = parsedValue;
            this.onValueChange();
        }
    }

    protected void onValueChange() {
        if (this.setter != null) {
            this.setter.accept(this.parsedValue);
        }
    }

    public final T value() {
        return this.parsedValue;
    }

    public final T defaultValue() {
        return this.defaultValue;
    }

    public final boolean isOptional() {
        return this.optional;
    }
}

