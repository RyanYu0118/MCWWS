/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 */
package ch.njol.skript.conditions;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.util.common.AnyAmount;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.skriptlang.skript.lang.util.SkriptQueue;

@Name(value="Is Empty")
@Description(value={"Checks whether an inventory, an inventory slot, a queue, or a text is empty."})
@Example(value="player's inventory is empty")
@Since(value={"unknown (before 2.1)"})
@Deprecated(since="2.13", forRemoval=true)
public class CondIsEmpty
extends PropertyCondition<Object> {
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public boolean check(Object object) {
        if (object instanceof String) {
            String string = (String)object;
            return string.isEmpty();
        }
        if (object instanceof SkriptQueue) {
            SkriptQueue queue = (SkriptQueue)object;
            return queue.isEmpty();
        }
        if (object instanceof Inventory) {
            Inventory inventory = (Inventory)object;
            for (ItemStack s : inventory.getContents()) {
                if (s == null || s.getType() == Material.AIR) continue;
                return false;
            }
            return true;
        }
        if (object instanceof Slot) {
            Slot slot = (Slot)object;
            ItemStack item = slot.getItem();
            return item == null || item.getType() == Material.AIR;
        }
        if (object instanceof AnyAmount) {
            AnyAmount numbered = (AnyAmount)object;
            return numbered.isEmpty();
        }
        if (!$assertionsDisabled) {
            throw new AssertionError();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "empty";
    }

    static {
        boolean bl = $assertionsDisabled = !CondIsEmpty.class.desiredAssertionStatus();
        if (!SkriptConfig.useTypeProperties.value().booleanValue()) {
            CondIsEmpty.register(CondIsEmpty.class, "empty", "inventories/slots/strings/numbered");
        }
    }
}

