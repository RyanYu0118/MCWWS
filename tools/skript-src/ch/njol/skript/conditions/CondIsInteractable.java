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

@Name(value="Is Interactable")
@Description(value={"Checks wether or not a block is interactable."})
@Example(value="on block break:\n\tif event-block is interactable:\n\t\tcancel event\n\t\tsend \"You cannot break interactable blocks!\"\n")
@Since(value={"2.5.2"})
public class CondIsInteractable
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType item) {
        return item.getMaterial().isInteractable();
    }

    @Override
    protected String getPropertyName() {
        return "interactable";
    }

    static {
        CondIsInteractable.register(CondIsInteractable.class, "interactable", "itemtypes");
    }
}

