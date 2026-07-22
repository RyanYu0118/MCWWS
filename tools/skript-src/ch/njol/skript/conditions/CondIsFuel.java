/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import org.bukkit.Material;

@Name(value="Is Fuel")
@Description(value={"Checks whether an item can be used as fuel in a furnace."})
@Example(value="on right click on furnace:\n\tif player's tool is not fuel:\n\t\tsend \"Please hold a valid fuel item in your hand\"\n\t\tcancel event\n")
@Since(value={"2.5.1"})
public class CondIsFuel
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType item) {
        return item.getMaterial().isFuel();
    }

    @Override
    public Condition simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return "fuel";
    }

    static {
        if (Skript.methodExists(Material.class, "isFuel", new Class[0])) {
            CondIsFuel.register(CondIsFuel.class, "[furnace] fuel", "itemtypes");
        }
    }
}

