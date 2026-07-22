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

@Name(value="Is Transparent")
@Description(value={"Checks whether an item is transparent. Note that this condition may not work for all blocks, due to the transparency list used by Spigot not being completely accurate."})
@Example(value="player's tool is transparent.")
@Since(value={"2.2-dev36"})
public class CondIsTransparent
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType itemType) {
        return itemType.getMaterial().isTransparent();
    }

    @Override
    protected String getPropertyName() {
        return "transparent";
    }

    static {
        CondIsTransparent.register(CondIsTransparent.class, "transparent", "itemtypes");
    }
}

