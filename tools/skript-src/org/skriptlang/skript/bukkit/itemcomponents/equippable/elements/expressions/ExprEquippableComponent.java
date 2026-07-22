/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.DataComponentType
 *  io.papermc.paper.datacomponent.DataComponentTypes
 *  io.papermc.paper.datacomponent.item.Equippable
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions;

import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.util.common.AnyAmount;
import ch.njol.skript.util.ItemSource;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component")
@Description(value={"The equippable component of an item. Any changes made to the equippable component will be present on the item.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="set {_component} to the equippable component of {_item}\nset the equipment slot of {_component} to helmet slot\n"), @Example(value="clear the equippable component of {_item}"), @Example(value="reset the equippable component of {_item}")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class ExprEquippableComponent
extends SimplePropertyExpression<Object, EquippableWrapper>
implements EquippableExperimentSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprEquippableComponent.infoBuilder(ExprEquippableComponent.class, EquippableWrapper.class, "equippable component[s]", "slots/itemtypes", false).supplier(ExprEquippableComponent::new)).build());
    }

    @Override
    public EquippableWrapper convert(Object object) {
        ItemSource<AnyAmount> itemSource = null;
        if (object instanceof ItemType) {
            ItemType itemType = (ItemType)object;
            itemSource = new ItemSource<ItemType>(itemType);
        } else if (object instanceof Slot) {
            Slot slot = (Slot)object;
            itemSource = ItemSource.fromSlot(slot);
        }
        return itemSource == null ? null : new EquippableWrapper(itemSource);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(EquippableWrapper.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Equippable component = null;
        if (delta != null) {
            component = (Equippable)((EquippableWrapper)delta[0]).getComponent();
        }
        for (Object object : this.getExpr().getArray(event)) {
            if (object instanceof ItemType) {
                ItemType itemType = (ItemType)object;
                this.changeItemType(itemType, mode, component);
                continue;
            }
            if (!(object instanceof Slot)) continue;
            Slot slot = (Slot)object;
            this.changeSlot(slot, mode, component);
        }
    }

    public void changeItemType(ItemType itemType, Changer.ChangeMode mode, Equippable component) {
        for (ItemData itemData : itemType) {
            ItemStack dataStack = itemData.getStack();
            if (dataStack == null) continue;
            this.changeItemStack(dataStack, mode, component);
        }
    }

    public void changeSlot(Slot slot, Changer.ChangeMode mode, Equippable component) {
        ItemStack itemStack = slot.getItem();
        if (itemStack == null) {
            return;
        }
        itemStack = this.changeItemStack(itemStack, mode, component);
        slot.setItem(itemStack);
    }

    public ItemStack changeItemStack(ItemStack itemStack, Changer.ChangeMode mode, Equippable component) {
        switch (mode) {
            case SET: {
                itemStack.setData(DataComponentTypes.EQUIPPABLE, (Object)component);
                break;
            }
            case DELETE: {
                itemStack.unsetData((DataComponentType)DataComponentTypes.EQUIPPABLE);
                break;
            }
            case RESET: {
                itemStack.resetData((DataComponentType)DataComponentTypes.EQUIPPABLE);
            }
        }
        return itemStack;
    }

    @Override
    public Class<EquippableWrapper> getReturnType() {
        return EquippableWrapper.class;
    }

    @Override
    protected String getPropertyName() {
        return "equippable component";
    }
}

