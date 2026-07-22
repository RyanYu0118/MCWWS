/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.loot.Lootable
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.loot.Lootable;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootTableUtils;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Seed of Loot Table")
@Description(value={"Returns the seed of a loot table. Setting the seed of a block or entity that does not have a loot table will not do anything."})
@Example.Examples(value={@Example(value="set {_seed} loot table seed of block"), @Example(value="set loot table seed of entity to 123456789")})
@Since(value={"2.10"})
public class ExprLootTableSeed
extends SimplePropertyExpression<Object, Long> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprLootTableSeed.infoBuilder(ExprLootTableSeed.class, Long.class, "loot[[ ]table] seed[s]", "entities/blocks", false).supplier(ExprLootTableSeed::new)).build());
    }

    @Override
    @Nullable
    public Long convert(Object object) {
        Lootable lootable = LootTableUtils.getAsLootable(object);
        return lootable != null ? Long.valueOf(lootable.getSeed()) : null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Number.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        long seedValue = ((Number)delta[0]).longValue();
        for (Object object : this.getExpr().getArray(event)) {
            if (!LootTableUtils.isLootable(object)) continue;
            Lootable lootable = LootTableUtils.getAsLootable(object);
            lootable.setSeed(seedValue);
            LootTableUtils.updateState(lootable);
        }
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "loot table seed";
    }
}

