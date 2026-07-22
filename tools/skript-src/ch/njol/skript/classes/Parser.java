/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes;

import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.util.StringMode;
import org.jetbrains.annotations.Nullable;

public abstract class Parser<T> {
    @Nullable
    public T parse(String s, ParseContext context) {
        throw new UnsupportedOperationException("Parsing not implemented (remember to override parse method): " + this.getClass().getName());
    }

    public boolean canParse(ParseContext context) {
        return true;
    }

    public abstract String toString(T var1, int var2);

    public final String toString(T o, StringMode mode) {
        switch (mode) {
            case MESSAGE: {
                return this.toString(o, 0);
            }
            case DEBUG: {
                return this.getDebugMessage(o);
            }
            case VARIABLE_NAME: {
                return this.toVariableNameString(o);
            }
            case COMMAND: {
                return this.toCommandString(o);
            }
        }
        assert (false);
        return "";
    }

    public String toCommandString(T o) {
        return this.toString(o, 0);
    }

    public abstract String toVariableNameString(T var1);

    public String getDebugMessage(T o) {
        return this.toString(o, 0);
    }
}

