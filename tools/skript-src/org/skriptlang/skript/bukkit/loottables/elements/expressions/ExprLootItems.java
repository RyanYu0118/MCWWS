/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.loot.LootContext
 *  org.bukkit.loot.LootTable
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootContextWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Loot of Loot Table")
@Description(value={"Returns the items of a loot table using a loot context. Not specifying a loot context will use a loot context with a location at the world's origin."})
@Example.Examples(value={@Example(value="set {_items::*} to loot items of the loot table \"minecraft:chests/simple_dungeon\" with loot context {_context}\n# this will set {_items::*} to the items that would be dropped from the simple dungeon loot table with the given loot context\n"), @Example(value="give player loot items of entity's loot table with loot context {_context}\n# this will give the player the items that the entity would drop with the given loot context\n")})
@Since(value={"2.10"})
public class ExprLootItems
extends SimpleExpression<ItemStack> {
    private Expression<LootTable> lootTables;
    private Expression<LootContext> context;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprLootItems.class, ItemStack.class).addPatterns("[the] loot of %loottables% [(with|using) %-lootcontext%]", "%loottables%'[s] loot [(with|using) %-lootcontext%]")).supplier(ExprLootItems::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.lootTables = exprs[0];
        this.context = exprs[1];
        return true;
    }

    protected ItemStack @Nullable [] get(Event event) {
        LootContext context;
        if (this.context != null) {
            context = this.context.getSingle(event);
            if (context == null) {
                return new ItemStack[0];
            }
        } else {
            context = new LootContextWrapper(((World)Bukkit.getWorlds().get(0)).getSpawnLocation()).getContext();
        }
        ArrayList items = new ArrayList();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (LootTable lootTable : this.lootTables.getArray(event)) {
            try {
                items.addAll(lootTable.populateLoot((Random)random, context));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        return items.toArray(new ItemStack[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("the loot of", this.lootTables);
        if (this.context != null) {
            builder.append("with", this.context);
        }
        return builder.toString();
    }
}

