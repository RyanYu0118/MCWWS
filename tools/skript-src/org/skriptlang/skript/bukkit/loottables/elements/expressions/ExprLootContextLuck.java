/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.loot.LootContext
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootContextCreateEvent;
import org.skriptlang.skript.bukkit.loottables.LootContextWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Luck of Loot Context")
@Description(value={"Returns the luck of a loot context as a float. This represents the luck potion effect that an entity can have."})
@Example.Examples(value={@Example(value="set {_luck} to loot luck value of {_context}"), @Example(value="set {_context} to a loot context at player:\n\tset loot luck value to 10\n\tset looter to player\n\tset looted entity to last spawned pig\n")})
@Since(value={"2.10"})
public class ExprLootContextLuck
extends SimplePropertyExpression<LootContext, Float> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprLootContextLuck.infoBuilder(ExprLootContextLuck.class, Float.class, "loot[ing] [context] luck [value|factor]", "lootcontexts", true).supplier(ExprLootContextLuck::new)).build());
    }

    @Override
    @Nullable
    public Float convert(LootContext context) {
        return Float.valueOf(context.getLuck());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)LootContextCreateEvent.class)) {
            Skript.error("You cannot set the loot context luck of an existing loot context.");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Float.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof LootContextCreateEvent)) {
            return;
        }
        LootContextCreateEvent createEvent = (LootContextCreateEvent)event;
        LootContextWrapper wrapper = createEvent.getContextWrapper();
        float luck = delta != null ? ((Float)delta[0]).floatValue() : 0.0f;
        switch (mode) {
            case SET: 
            case DELETE: 
            case RESET: {
                wrapper.setLuck(luck);
                break;
            }
            case ADD: {
                wrapper.setLuck(wrapper.getLuck() + luck);
                break;
            }
            case REMOVE: {
                wrapper.setLuck(wrapper.getLuck() - luck);
            }
        }
    }

    @Override
    public Class<? extends Float> getReturnType() {
        return Float.class;
    }

    @Override
    protected String getPropertyName() {
        return "loot luck factor";
    }
}

