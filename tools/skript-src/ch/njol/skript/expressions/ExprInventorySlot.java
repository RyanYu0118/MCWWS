/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.PlayerInventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.slot.EquipmentSlot;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.skript.util.slot.SlotWithIndex;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Inventory Slot")
@Description(value={"Represents a slot in an inventory. It can be used to change the item in an inventory too."})
@Example(value="if slot 0 of player is air:\n\tset slot 0 of player to 2 stones\n\tremove 1 stone from slot 0 of player\n\tadd 2 stones to slot 0 of player\n\tclear slot 1 of player\n")
@Since(value={"2.2-dev24"})
public class ExprInventorySlot
extends SimpleExpression<Slot> {
    private Expression<Number> slots;
    private Expression<Inventory> invis;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            this.slots = exprs[0];
            this.invis = exprs[1];
        } else {
            this.slots = exprs[1];
            this.invis = exprs[0];
        }
        return true;
    }

    @Nullable
    protected Slot[] get(Event event) {
        Inventory invi = this.invis.getSingle(event);
        if (invi == null) {
            return null;
        }
        ArrayList<SlotWithIndex> inventorySlots = new ArrayList<SlotWithIndex>();
        for (Number slot : this.slots.getArray(event)) {
            if (slot.intValue() < 0 || slot.intValue() >= invi.getSize()) continue;
            int slotIndex = slot.intValue();
            if (invi instanceof PlayerInventory && slotIndex >= 36) {
                HumanEntity holder = ((PlayerInventory)invi).getHolder();
                assert (holder != null);
                inventorySlots.add(new EquipmentSlot(holder, slotIndex));
                continue;
            }
            inventorySlots.add(new InventorySlot(invi, slot.intValue()));
        }
        if (inventorySlots.isEmpty()) {
            return null;
        }
        return inventorySlots.toArray(new Slot[inventorySlots.size()]);
    }

    @Override
    public boolean isSingle() {
        return this.slots.isSingle();
    }

    @Override
    public Class<? extends Slot> getReturnType() {
        return Slot.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "slots " + this.slots.toString(e, debug) + " of " + this.invis.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprInventorySlot.class, Slot.class, ExpressionType.COMBINED, "[the] slot[s] %numbers% of %inventory%", "%inventory%'[s] slot[s] %numbers%");
    }
}

