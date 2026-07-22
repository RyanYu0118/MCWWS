/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions.base;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionSection;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public abstract class SectionExpression<Value>
extends SimpleExpression<Value> {
    protected final ExpressionSection section = new ExpressionSection(this);

    public abstract boolean init(Expression<?>[] var1, int var2, Kleenean var3, SkriptParser.ParseResult var4, @Nullable SectionNode var5, @Nullable List<TriggerItem> var6);

    @Override
    public final boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return this.section.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    public boolean isSectionOnly() {
        return false;
    }

    public final Section getAsSection() {
        return this.section;
    }

    @Deprecated(since="2.12", forRemoval=true)
    protected Trigger loadCode(SectionNode sectionNode, String name, @Nullable Runnable afterLoading, Class<? extends Event> ... events) {
        return this.loadCode(sectionNode, name, null, afterLoading, events);
    }

    @SafeVarargs
    protected final Trigger loadCode(SectionNode sectionNode, String name, @Nullable Runnable beforeLoading, @Nullable Runnable afterLoading, Class<? extends Event> ... events) {
        return this.section.loadCodeTask(sectionNode, name, beforeLoading, afterLoading, events);
    }

    protected void loadCode(SectionNode sectionNode) {
        this.section.loadCode(sectionNode);
    }

    protected void loadOptionalCode(SectionNode sectionNode) {
        this.section.loadOptionalCode(sectionNode);
    }

    protected void setTriggerItems(List<TriggerItem> items) {
        this.section.setTriggerItems(items);
    }

    protected boolean runSection(Event event) {
        return this.section.runSection(event);
    }
}

