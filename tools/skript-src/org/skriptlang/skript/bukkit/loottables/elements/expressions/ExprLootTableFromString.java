/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.NamespacedKey
 *  org.bukkit.event.Event
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
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Loot Table from Key")
@Description(value={"Returns the loot table from a namespaced key."})
@Example(value="set {_table} to loot table \"minecraft:chests/simple_dungeon\"")
@Since(value={"2.10"})
public class ExprLootTableFromString
extends SimpleExpression<LootTable> {
    private Expression<String> keys;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprLootTableFromString.class, LootTable.class).addPatterns("[the] loot[ ]table[s] %strings%")).supplier(ExprLootTableFromString::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.keys = exprs[0];
        return true;
    }

    protected LootTable @Nullable [] get(Event event) {
        ArrayList<LootTable> lootTables = new ArrayList<LootTable>();
        for (String key : this.keys.getArray(event)) {
            LootTable lootTable;
            NamespacedKey namespacedKey = NamespacedKey.fromString((String)key);
            if (namespacedKey == null || (lootTable = Bukkit.getLootTable((NamespacedKey)namespacedKey)) == null) continue;
            lootTables.add(lootTable);
        }
        return lootTables.toArray(new LootTable[0]);
    }

    @Override
    public boolean isSingle() {
        return this.keys.isSingle();
    }

    @Override
    public Class<? extends LootTable> getReturnType() {
        return LootTable.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the loot table of " + this.keys.toString(event, debug);
    }
}

