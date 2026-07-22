/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Condition
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

public class CondIsInstanceOf
extends Condition {
    private Expression<Object> objects;
    private Expression<JavaType> type;

    public boolean check(Event e) {
        return this.objects.check(e, o -> this.type.check(e, t -> t.getJavaClass().isAssignableFrom(SkriptMirrorUtil.getClass(o)), this.isNegated()));
    }

    public String toString(Event e, boolean debug) {
        return String.format("%s instanceof %s", this.objects.toString(e, debug), this.type.toString(e, debug));
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.objects = SkriptUtil.defendExpression(exprs[0]);
        this.type = SkriptUtil.defendExpression(exprs[1]);
        this.setNegated(matchedPattern == 1);
        return true;
    }

    static {
        Skript.registerCondition(CondIsInstanceOf.class, (String[])new String[]{"%objects% (is|are) [a[n]] instance[s] of %javatypes%", "%objects% (is not|isn't|are not|aren't) [a[n]] instance[s] of %javatypes%"});
    }
}

