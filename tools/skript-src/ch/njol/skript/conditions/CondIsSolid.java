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

@Name(value="Is Solid")
@Description(value={"Checks whether an item is solid."})
@Example.Examples(value={@Example(value="grass block is solid"), @Example(value="player's tool isn't solid")})
@Since(value={"2.2-dev36"})
public class CondIsSolid
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType itemType) {
        return itemType.getMaterial().isSolid();
    }

    @Override
    protected String getPropertyName() {
        return "solid";
    }

    static {
        CondIsSolid.register(CondIsSolid.class, "solid", "itemtypes");
    }
}

