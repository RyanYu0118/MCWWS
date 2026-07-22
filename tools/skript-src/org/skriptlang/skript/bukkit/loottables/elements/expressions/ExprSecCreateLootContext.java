/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.bukkit.loot.LootContext
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Direction;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootContextCreateEvent;
import org.skriptlang.skript.bukkit.loottables.LootContextWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Create Loot Context")
@Description(value={"Create a loot context."})
@Example(value="set {_player} to player\nset {_context} to a loot context at player:\n    set loot luck value to 10\n    set looter to {_player}\n    set looted entity to last spawned pig\ngive player loot items of loot table \"minecraft:entities/iron_golem\" with loot context {_context}\n")
@Since(value={"2.10"})
public class ExprSecCreateLootContext
extends SectionExpression<LootContext> {
    private Trigger trigger;
    private Expression<Location> location;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprSecCreateLootContext.class, LootContext.class).addPatterns("[a] loot context %direction% %location%")).supplier(ExprSecCreateLootContext::new)).build());
        EventValues.registerEventValue(LootContextCreateEvent.class, LootContext.class, event -> event.getContextWrapper().getContext());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, SkriptParser.ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
        this.location = Direction.combine(exprs[0], exprs[1]);
        if (node != null) {
            this.trigger = SectionUtils.loadLinkedCode("create loot context", (beforeLoading, afterLoading) -> this.loadCode(node, "create loot context", (Runnable)beforeLoading, (Runnable)afterLoading, (Class<? extends Event>)LootContextCreateEvent.class));
            return this.trigger != null;
        }
        return true;
    }

    protected LootContext @Nullable [] get(Event event) {
        Location loc = this.location.getSingle(event);
        if (loc == null) {
            return new LootContext[0];
        }
        LootContextWrapper wrapper = new LootContextWrapper(loc);
        if (this.trigger != null) {
            LootContextCreateEvent contextEvent = new LootContextCreateEvent(wrapper);
            Variables.withLocalVariables(event, contextEvent, () -> TriggerItem.walk(this.trigger, contextEvent));
        }
        return new LootContext[]{wrapper.getContext()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends LootContext> getReturnType() {
        return LootContext.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a loot context " + this.location.toString(event, debug);
    }
}

