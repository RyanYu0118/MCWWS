/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
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
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootContextCreateEvent;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Looted Entity of Loot Context")
@Description(value={"Returns the looted entity of a loot context."})
@Example.Examples(value={@Example(value="set {_entity} to looted entity of {_context}"), @Example(value="set {_context} to a loot context at player:\n\tset loot luck value to 10\n\tset looter to player\n\tset looted entity to last spawned pig\n")})
@Since(value={"2.10"})
public class ExprLootContextEntity
extends SimplePropertyExpression<LootContext, Entity> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprLootContextEntity.infoBuilder(ExprLootContextEntity.class, Entity.class, "looted entity", "lootcontexts", true).supplier(ExprLootContextEntity::new)).build());
    }

    @Override
    @Nullable
    public Entity convert(LootContext context) {
        return context.getLootedEntity();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)LootContextCreateEvent.class)) {
            Skript.error("You cannot set the looted entity of an existing loot context.");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Entity.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof LootContextCreateEvent)) {
            return;
        }
        LootContextCreateEvent createEvent = (LootContextCreateEvent)event;
        Entity entity = delta != null ? (Entity)delta[0] : null;
        createEvent.getContextWrapper().setEntity(entity);
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return Entity.class;
    }

    @Override
    protected String getPropertyName() {
        return "looted entity";
    }
}

