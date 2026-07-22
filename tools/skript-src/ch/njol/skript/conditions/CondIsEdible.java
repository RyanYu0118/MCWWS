/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.conditions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;

@Name(value="Is Edible")
@Description(value={"Checks whether an item is edible."})
@Example.Examples(value={@Example(value="cooked beef is edible"), @Example(value="player's tool is edible")})
@Since(value={"2.2-dev36"})
public class CondIsEdible
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType itemType) {
        return itemType.getMaterial().isEdible();
    }

    @Override
    protected String getPropertyName() {
        return "edible";
    }

    static {
        PropertyCondition.register(CondIsEdible.class, "edible", "itemtypes");
    }
}

