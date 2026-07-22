/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.item.Equippable
 *  org.bukkit.inventory.ItemStack
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.ItemSource;
import ch.njol.skript.util.slot.Slot;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.inventory.ItemStack;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.conditions.CondEquipCompDamage;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.conditions.CondEquipCompDispensable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.conditions.CondEquipCompInteract;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.conditions.CondEquipCompShearable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.conditions.CondEquipCompSwapEquipment;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.effects.EffEquipCompDamageable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.effects.EffEquipCompDispensable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.effects.EffEquipCompInteract;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.effects.EffEquipCompShearable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.effects.EffEquipCompSwapEquipment;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprEquipCompCameraOverlay;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprEquipCompEntities;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprEquipCompEquipSound;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprEquipCompModel;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprEquipCompShearSound;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprEquipCompSlot;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprEquippableComponent;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions.ExprSecBlankEquipComp;
import org.skriptlang.skript.lang.converter.Converters;

public class EquippableModule
extends HierarchicalAddonModule {
    public EquippableModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected boolean canLoadSelf(SkriptAddon addon) {
        return Skript.classExists("io.papermc.paper.datacomponent.item.Equippable");
    }

    @Override
    protected void initSelf(SkriptAddon addon) {
        Classes.registerClass(new ClassInfo<EquippableWrapper>(EquippableWrapper.class, "equippablecomponent").user("equippable ?components?").name("Equippable Components").description("Represents an equippable component used for items.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n").requiredPlugins("Minecraft 1.21.2+").since("2.13").defaultExpression(new EventValueExpression<EquippableWrapper>(EquippableWrapper.class)).parser(new Parser<EquippableWrapper>(this){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(EquippableWrapper wrapper, int flags) {
                return "equippable component";
            }

            @Override
            public String toVariableNameString(EquippableWrapper wrapper) {
                return "equippable component#" + wrapper.hashCode();
            }
        }).after("itemstack", "itemtype", "slot"));
        Converters.registerConverter(Equippable.class, EquippableWrapper.class, EquippableWrapper::new, 2);
        Converters.registerConverter(ItemStack.class, EquippableWrapper.class, EquippableWrapper::new, 2);
        Converters.registerConverter(ItemType.class, EquippableWrapper.class, itemType -> new EquippableWrapper(new ItemSource<ItemType>((ItemType)itemType)), 2);
        Converters.registerConverter(Slot.class, EquippableWrapper.class, slot -> {
            ItemSource<Slot> itemSource = ItemSource.fromSlot(slot);
            if (itemSource == null) {
                return null;
            }
            return new EquippableWrapper(itemSource);
        }, 2);
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, CondEquipCompDamage::register, CondEquipCompDispensable::register, CondEquipCompInteract::register, CondEquipCompShearable::register, CondEquipCompSwapEquipment::register, EffEquipCompDamageable::register, EffEquipCompDispensable::register, EffEquipCompInteract::register, EffEquipCompShearable::register, EffEquipCompSwapEquipment::register, ExprEquipCompCameraOverlay::register, ExprEquipCompEntities::register, ExprEquipCompEquipSound::register, ExprEquipCompModel::register, ExprEquipCompShearSound::register, ExprEquipCompSlot::register, ExprEquippableComponent::register, ExprSecBlankEquipComp::register);
    }

    @Override
    public String name() {
        return "equippable component";
    }
}

