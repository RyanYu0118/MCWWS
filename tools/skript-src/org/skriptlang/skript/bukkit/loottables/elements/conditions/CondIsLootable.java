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

@Name(value="Is Lootable")
@Description(value={"Checks whether an entity or block is lootable. Lootables are entities or blocks that can have a loot table."})
@Example.Examples(value={@Example(value="spawn a pig at event-location\nset {_pig} to last spawned entity\nif {_pig} is lootable:\n\tset loot table of {_pig} to \"minecraft:entities/cow\"\n\t# the pig will now drop the loot of a cow when killed, because it is indeed a lootable entity.\n"), @Example(value="set block at event-location to chest\nif block at event-location is lootable:\n\tset loot table of block at event-location to \"minecraft:chests/simple_dungeon\"\n\t# the chest will now generate the loot of a simple dungeon when opened, because it is indeed a lootable block.\n"), @Example(value="set block at event-location to white wool\nif block at event-location is lootable:\n\t# uh oh, nothing will happen because a wool is not a lootable block.\n")})
@Since(value={"2.10"})
public class CondIsLootable
extends PropertyCondition<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondIsLootable.infoBuilder(CondIsLootable.class, PropertyCondition.PropertyType.BE, "lootable", "blocks/entities").supplier(CondIsLootable::new).build());
    }

    @Override
    public boolean check(Object object) {
        return LootTableUtils.isLootable(object);
    }

    @Override
    protected String getPropertyName() {
        return "lootable";
    }
}

