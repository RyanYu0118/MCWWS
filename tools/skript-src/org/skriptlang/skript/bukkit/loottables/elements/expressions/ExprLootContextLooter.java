/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootContextCreateEvent;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Looter of Loot Context")
@Description(value={"Returns the looter of a loot context. Note that setting the looter will read the looter's tool enchantments (e.g. looting) when generating loot."})
@Example.Examples(value={@Example(value="set {_killer} to looter of {_context}"), @Example(value="set {_context} to a loot context at player:\n\tset loot luck value to 10\n\tset looter to player\n\tset looted entity to last spawned pig\n")})
@Since(value={"2.10"})
public class ExprLootContextLooter
extends SimplePropertyExpression<LootContext, Player> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprLootContextLooter.infoBuilder(ExprLootContextLooter.class, Player.class, "(looter|looting player)", "lootcontexts", true).supplier(ExprLootContextLooter::new)).build());
    }

    @Override
    @Nullable
    public Player convert(LootContext context) {
        HumanEntity humanEntity = context.getKiller();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            return player;
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)LootContextCreateEvent.class)) {
            Skript.error("You cannot set the looting player of an existing loot context.");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Player.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof LootContextCreateEvent)) {
            return;
        }
        LootContextCreateEvent createEvent = (LootContextCreateEvent)event;
        Player player = delta != null ? (Player)delta[0] : null;
        createEvent.getContextWrapper().setKiller(player);
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }

    @Override
    protected String getPropertyName() {
        return "looting player";
    }
}

