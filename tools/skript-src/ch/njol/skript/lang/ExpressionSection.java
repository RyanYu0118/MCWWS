/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class ExpressionSection
extends Section {
    protected final SectionExpression<?> expression;

    public ExpressionSection(SectionExpression<?> expression) {
        this.expression = expression;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Section.SectionContext context = this.getParser().getData(Section.SectionContext.class);
        if (context.sectionNode == null && this.expression.isSectionOnly()) {
            Skript.error("This expression requires a section.");
            return false;
        }
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            boolean claimedSection = context.claim(this, parseResult.expr);
            if (claimedSection) {
                boolean init = this.expression.init(expressions, matchedPattern, isDelayed, parseResult, context.sectionNode, context.triggerItems);
                if (init) {
                    log.printLog();
                    boolean bl = true;
                    return bl;
                }
                context.unclaim(this);
                log.printError();
                boolean bl = false;
                return bl;
            }
            if (this.expression.isSectionOnly() || context.owner instanceof ExpressionSection) {
                log.printError();
                boolean bl = false;
                return bl;
            }
            log.clear();
        }
        return this.expression.init(expressions, matchedPattern, isDelayed, parseResult, null, null);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, @Nullable SectionNode sectionNode, List<TriggerItem> triggerItems) {
        return this.expression.init(expressions, matchedPattern, isDelayed, parseResult, sectionNode, triggerItems);
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        return super.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.expression.toString(event, debug);
    }

    public SectionExpression<?> getAsExpression() {
        return this.expression;
    }

    @Override
    public void loadCode(SectionNode sectionNode) {
        super.loadCode(sectionNode);
    }

    @Override
    public void loadOptionalCode(SectionNode sectionNode) {
        super.loadOptionalCode(sectionNode);
    }

    public boolean runSection(Event event) {
        return TriggerItem.walk(this.first, event);
    }

    @Override
    public void setTriggerItems(List<TriggerItem> items) {
        super.setTriggerItems(items);
    }

    @SafeVarargs
    public final Trigger loadCodeTask(SectionNode sectionNode, String name, @Nullable Runnable beforeLoading, @Nullable Runnable afterLoading, Class<? extends Event> ... events) {
        return super.loadCode(sectionNode, name, beforeLoading, afterLoading, events);
    }
}

