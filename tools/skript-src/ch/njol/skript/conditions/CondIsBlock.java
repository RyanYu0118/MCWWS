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

@Name(value="Is Block")
@Description(value={"Checks whether an item is a block."})
@Example.Examples(value={@Example(value="player's held item is a block"), @Example(value="{list::*} are blocks")})
@Since(value={"2.4"})
public class CondIsBlock
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType itemType) {
        return itemType.getMaterial().isBlock();
    }

    @Override
    protected String getPropertyName() {
        return "block";
    }

    static {
        CondIsBlock.register(CondIsBlock.class, "([a] block|blocks)", "itemtypes");
    }
}

