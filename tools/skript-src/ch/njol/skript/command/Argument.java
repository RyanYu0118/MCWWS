/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.command;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.Variables;
import java.util.WeakHashMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class Argument<T> {
    @Nullable
    private final String name;
    @Nullable
    private final Expression<? extends T> def;
    private final ClassInfo<T> type;
    private final boolean single;
    private final int index;
    private final boolean optional;
    private transient WeakHashMap<Event, T[]> current = new WeakHashMap();

    private Argument(@Nullable String name, @Nullable Expression<? extends T> def, ClassInfo<T> type, boolean single, int index, boolean optional) {
        this.name = name;
        this.def = def;
        this.type = type;
        this.single = single;
        this.index = index;
        this.optional = optional;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static <T> Argument<T> newInstance(@Nullable String name, ClassInfo<T> type, @Nullable String def, int index, boolean single, boolean forceOptional) {
        if (name != null && !Variable.isValidVariableName(name, false, false)) {
            Skript.error("An argument's name must be a valid variable name, and cannot be a list variable.");
            return null;
        }
        Expression<String> d = null;
        if (def != null) {
            if (def.startsWith("%") && def.endsWith("%")) {
                RetainingLogHandler log = SkriptLogger.startRetainingLog();
                try {
                    d = new SkriptParser(def.substring(1, def.length() - 1), 1, ParseContext.COMMAND).parseExpression(type.getC());
                    if (d == null) {
                        log.printErrors("Can't understand this expression: " + def);
                        Argument<T> argument = null;
                        return argument;
                    }
                    log.printLog();
                }
                finally {
                    log.stop();
                }
            }
            RetainingLogHandler log = SkriptLogger.startRetainingLog();
            try {
                d = type.getC() == String.class ? (def.startsWith("\"") && def.endsWith("\"") ? VariableString.newInstance(def.substring(1, def.length() - 1)) : new SimpleLiteral<String>(def, false)) : new SkriptParser(def, 2, ParseContext.DEFAULT).parseExpression(type.getC());
                if (d == null) {
                    log.printErrors("Can't understand this expression: '" + def + "'");
                    Argument<T> argument = null;
                    return argument;
                }
                log.printLog();
            }
            finally {
                log.stop();
            }
        }
        return new Argument<T>(name, d, type, single, index, def != null || forceOptional);
    }

    public String toString() {
        Expression<T> def = this.def;
        return "<" + (String)(this.name != null ? this.name + ": " : "") + Utils.toEnglishPlural(this.type.getCodeName(), !this.single) + (String)(def == null ? "" : " = " + def.toString()) + ">";
    }

    public boolean isOptional() {
        return this.optional;
    }

    public void setToDefault(ScriptCommandEvent event) {
        if (this.def != null) {
            this.set(event, this.def.getArray(event));
        }
    }

    public void set(ScriptCommandEvent e, Object[] o) {
        if (!this.type.getC().isAssignableFrom(o.getClass().getComponentType())) {
            throw new IllegalArgumentException();
        }
        this.current.put(e, o);
        String name = this.name;
        if (name != null) {
            if (this.single) {
                if (o.length > 0) {
                    Variables.setVariable(name, o[0], e, true);
                }
            } else {
                for (int i = 0; i < o.length; ++i) {
                    Variables.setVariable(name + "::" + (i + 1), o[i], e, true);
                }
            }
        }
    }

    @Nullable
    public T[] getCurrent(Event e) {
        return this.current.get(e);
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public Class<T> getType() {
        return this.type.getC();
    }

    public int getIndex() {
        return this.index;
    }

    public boolean isSingle() {
        return this.single;
    }
}

