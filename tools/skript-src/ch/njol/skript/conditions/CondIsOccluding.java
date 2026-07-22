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

@Name(value="Is Occluding")
@Description(value={"Checks whether an item is a block and completely blocks vision."})
@Example(value="player's tool is occluding")
@Since(value={"2.5.1"})
public class CondIsOccluding
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType item) {
        return item.getMaterial().isOccluding();
    }

    @Override
    protected String getPropertyName() {
        return "occluding";
    }

    static {
        CondIsOccluding.register(CondIsOccluding.class, "occluding", "itemtypes");
    }
}

