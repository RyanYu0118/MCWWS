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

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootContextCreateEvent;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Loot Location of Loot Context")
@Description(value={"Returns the loot location of a loot context."})
@Example(value="set {_player} to player\nset {_context} to a loot context at player:\n\tif {_player} is in \"world_nether\":\n\t\tset loot location to location of last spawned pig\nsend loot location of {_context} to player\n")
@Since(value={"2.10"})
public class ExprLootContextLocation
extends SimplePropertyExpression<LootContext, Location> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprLootContextLocation.infoBuilder(ExprLootContextLocation.class, Location.class, "loot[ing] [context] location", "lootcontexts", true).supplier(ExprLootContextLocation::new)).build());
    }

    @Override
    public Location convert(LootContext context) {
        return context.getLocation();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)LootContextCreateEvent.class)) {
            Skript.error("You cannot set the loot context location of an existing loot context.");
            return null;
        }
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Location.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof LootContextCreateEvent)) {
            return;
        }
        LootContextCreateEvent createEvent = (LootContextCreateEvent)event;
        assert (delta != null);
        createEvent.getContextWrapper().setLocation((Location)delta[0]);
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    protected String getPropertyName() {
        return "loot location";
    }
}

