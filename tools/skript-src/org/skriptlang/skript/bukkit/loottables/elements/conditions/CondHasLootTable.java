/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.loottables.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.skriptlang.skript.bukkit.loottables.LootTableUtils;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Has Loot Table")
@Description(value={"Checks whether an entity or block has a loot table. The loot tables of chests will be deleted when the chest is opened or broken."})
@Example(value="set event-block to chest\nif event-block has a loot table:\n\t# this will never happen, because it doesn't have a loot table.\n\nset loot table of event-block to \"minecraft:chests/simple_dungeon\"\nif event-block has a loot table:\n\t# this will happen, because it now has a loot table.\n")
@Since(value={"2.10"})
public class CondHasLootTable
extends PropertyCondition<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondHasLootTable.infoBuilder(CondHasLootTable.class, PropertyCondition.PropertyType.HAVE, "[a] loot[ ]table", "blocks/entities").supplier(CondHasLootTable::new).build());
    }

    @Override
    public boolean check(Object object) {
        return LootTableUtils.isLootable(object) && LootTableUtils.getLootTable(object) != null;
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "a loot table";
    }
}

