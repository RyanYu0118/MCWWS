/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

@NoDoc
public class EffRunRunnable
extends Effect {
    private Expression<?> task;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        this.task = LiteralUtils.defendExpression(expressions[0]);
        return LiteralUtils.canInitSafely(this.task);
    }

    @Override
    protected void execute(Event event) {
        Object single = this.task.getSingle(event);
        if (single instanceof Runnable) {
            Runnable runnable = (Runnable)single;
            runnable.run();
        } else {
            Bukkit.getLogger().severe("Tried to run a non-runnable " + String.valueOf(single));
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "run runnable " + this.task.toString(event, debug);
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerEffect(EffRunRunnable.class, "run runnable %object%");
        }
    }
}

