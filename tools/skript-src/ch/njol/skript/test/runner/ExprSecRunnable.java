/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoDoc
public class ExprSecRunnable
extends SectionExpression<Object> {
    private Trigger trigger;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
        this.loadCode(node);
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        return new Runnable[]{() -> this.runSection(event)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public boolean isSectionOnly() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return Runnable.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a new runnable";
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerExpression(ExprSecRunnable.class, Object.class, ExpressionType.SIMPLE, "[a] new runnable");
        }
    }

    public static class RunnableEvent
    extends Event {
        @NotNull
        public HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }
}

