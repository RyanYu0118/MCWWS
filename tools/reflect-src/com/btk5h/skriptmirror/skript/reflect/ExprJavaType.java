/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import org.bukkit.event.Event;

public class ExprJavaType
extends SimpleExpression<JavaType> {
    private Expression<String> className;

    protected JavaType[] get(Event e) {
        String cls = (String)this.className.getSingle(e);
        if (cls == null) {
            return null;
        }
        try {
            return new JavaType[]{new JavaType(LibraryLoader.getClassLoader().loadClass(cls))};
        }
        catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends JavaType> getReturnType() {
        return JavaType.class;
    }

    public String toString(Event e, boolean debug) {
        return "class " + this.className.toString(e, debug);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.className = exprs[0];
        return true;
    }

    static {
        Skript.registerExpression(ExprJavaType.class, JavaType.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"[the] [java] class %string%"});
    }
}

