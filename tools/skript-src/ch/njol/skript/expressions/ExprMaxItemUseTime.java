/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Max Item Use Time")
@Description(value={"Returns the max duration an item can be used for before the action completes. E.g. it takes 1.6 seconds to drink a potion, or 1.4 seconds to load an unenchanted crossbow.", "Some items, like bows and shields, do not have a limit to their use. They will return 1 hour."})
@Example(value="on right click:\n\tbroadcast max usage duration of player's tool\n")
@Since(value={"2.8.0"})
public class ExprMaxItemUseTime
extends SimplePropertyExpression<ItemStack, Timespan> {
    @Override
    @Nullable
    public Timespan convert(ItemStack item) {
        return new Timespan(Timespan.TimePeriod.TICK, item.getMaxItemUseDuration());
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "maximum usage time";
    }

    static {
        if (Skript.methodExists(ItemStack.class, "getMaxItemUseDuration", new Class[0])) {
            ExprMaxItemUseTime.register(ExprMaxItemUseTime.class, Timespan.class, "max[imum] [item] us(e|age) (time|duration)", "itemstacks");
        }
    }
}

