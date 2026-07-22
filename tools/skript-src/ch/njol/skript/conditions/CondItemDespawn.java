/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Item
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Item;

@Name(value="Will Despawn")
@Description(value={"Checks if the dropped item will be despawned naturally through Minecraft's timer."})
@Example(value="if all dropped items can despawn naturally:\n\tprevent all dropped items from naturally despawning\n")
@Since(value={"2.11"})
public class CondItemDespawn
extends PropertyCondition<Item> {
    @Override
    public boolean check(Item item) {
        return !item.isUnlimitedLifetime();
    }

    @Override
    protected String getPropertyName() {
        return "naturally despawn";
    }

    static {
        PropertyCondition.register(CondItemDespawn.class, PropertyCondition.PropertyType.WILL, "(despawn naturally|naturally despawn)", "itementities");
        PropertyCondition.register(CondItemDespawn.class, PropertyCondition.PropertyType.CAN, "(despawn naturally|naturally despawn)", "itementities");
    }
}

