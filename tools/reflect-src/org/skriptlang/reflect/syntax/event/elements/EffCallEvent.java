/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.util.Kleenean
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffCallEvent
extends Effect {
    private Expression<Event> eventExpr;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.eventExpr = SkriptUtil.defendExpression(exprs[0]);
        return SkriptUtil.canInitSafely(this.eventExpr);
    }

    protected void execute(Event e) {
        for (Event event : (Event[])this.eventExpr.getArray(e)) {
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "call event " + this.eventExpr.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffCallEvent.class, (String[])new String[]{"call [event] %events%"});
    }
}

