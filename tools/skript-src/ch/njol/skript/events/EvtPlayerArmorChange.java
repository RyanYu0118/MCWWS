/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
 *  com.destroystokyo.paper.event.player.PlayerArmorChangeEvent$SlotType
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.slot.EquipmentSlot;
import ch.njol.skript.util.slot.Slot;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import java.util.Map;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

public class EvtPlayerArmorChange
extends SkriptEvent {
    private static final boolean BODY_SLOT_EXISTS = Skript.fieldExists(org.bukkit.inventory.EquipmentSlot.class, "BODY");
    private static Converter<PlayerArmorChangeEvent, org.bukkit.inventory.EquipmentSlot> GET_SLOT;
    @Nullable
    private org.bukkit.inventory.EquipmentSlot slot = null;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (args.length == 1) {
            Literal<?> slotLiteral = args[0];
            this.slot = (org.bukkit.inventory.EquipmentSlot)slotLiteral.getSingle();
            if (this.slot == org.bukkit.inventory.EquipmentSlot.HAND || this.slot == org.bukkit.inventory.EquipmentSlot.OFF_HAND || BODY_SLOT_EXISTS && this.slot == org.bukkit.inventory.EquipmentSlot.BODY) {
                Skript.error("You can't detect an armor change event for " + Utils.a(Classes.toString(this.slot)) + ".");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        PlayerArmorChangeEvent changeEvent = (PlayerArmorChangeEvent)event;
        if (this.slot == null) {
            return true;
        }
        return this.slot == GET_SLOT.convert(changeEvent);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.slot == null) {
            builder.append((Object)"armor change");
        } else {
            builder.append((Object)(switch (this.slot) {
                case org.bukkit.inventory.EquipmentSlot.HEAD -> "helmet";
                case org.bukkit.inventory.EquipmentSlot.CHEST -> "chestplate";
                case org.bukkit.inventory.EquipmentSlot.LEGS -> "legging";
                case org.bukkit.inventory.EquipmentSlot.FEET -> "boots";
                default -> "";
            }));
            builder.append((Object)"changed");
        }
        return builder.toString();
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.player.PlayerArmorChangeEvent")) {
            Skript.registerEvent("Armor Change", EvtPlayerArmorChange.class, PlayerArmorChangeEvent.class, "[player] armo[u]r change[d]", "[player] %equipmentslot% change[d]").description("Called when armor pieces of a player are changed.").keywords("armor", "armour").examples("on armor change:", "\tbroadcast the old armor item", "on helmet change:").since("2.5, 2.11 (equipment slots)");
            if (Skript.methodExists(PlayerArmorChangeEvent.class, "getSlot", new Class[0])) {
                GET_SLOT = PlayerArmorChangeEvent::getSlot;
            } else {
                Map<PlayerArmorChangeEvent.SlotType, org.bukkit.inventory.EquipmentSlot> slotTypeMap = Map.of(Enum.valueOf(PlayerArmorChangeEvent.SlotType.class, "HEAD"), org.bukkit.inventory.EquipmentSlot.HEAD, Enum.valueOf(PlayerArmorChangeEvent.SlotType.class, "CHEST"), org.bukkit.inventory.EquipmentSlot.CHEST, Enum.valueOf(PlayerArmorChangeEvent.SlotType.class, "LEGS"), org.bukkit.inventory.EquipmentSlot.LEGS, Enum.valueOf(PlayerArmorChangeEvent.SlotType.class, "FEET"), org.bukkit.inventory.EquipmentSlot.FEET);
                GET_SLOT = event -> (org.bukkit.inventory.EquipmentSlot)slotTypeMap.get(event.getSlotType());
            }
            EventValues.registerEventValue(PlayerArmorChangeEvent.class, org.bukkit.inventory.EquipmentSlot.class, GET_SLOT);
            EventValues.registerEventValue(PlayerArmorChangeEvent.class, ItemStack.class, PlayerArmorChangeEvent::getOldItem, EventValues.TIME_PAST);
            EventValues.registerEventValue(PlayerArmorChangeEvent.class, ItemStack.class, PlayerArmorChangeEvent::getNewItem, EventValues.TIME_FUTURE);
            EventValues.registerEventValue(PlayerArmorChangeEvent.class, Slot.class, event -> new EquipmentSlot(event.getPlayer().getEquipment(), event.getSlot()));
        }
    }
}

