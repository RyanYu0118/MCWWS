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

@Name(value="Is Flammable")
@Description(value={"Checks whether an item is flammable."})
@Example.Examples(value={@Example(value="send whether the tag contents of minecraft tag \"planks\" are flammable"), @Example(value="player's tool is flammable")})
@Since(value={"2.2-dev36"})
public class CondIsFlammable
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType itemType) {
        return itemType.getMaterial().isFlammable();
    }

    @Override
    protected String getPropertyName() {
        return "flammable";
    }

    static {
        CondIsFlammable.register(CondIsFlammable.class, "flammable", "itemtypes");
    }
}

