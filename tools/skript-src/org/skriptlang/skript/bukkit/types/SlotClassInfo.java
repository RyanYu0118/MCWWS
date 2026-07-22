/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.types;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.util.slot.Slot;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Internal
public class SlotClassInfo
extends ClassInfo<Slot> {
    public SlotClassInfo() {
        super(Slot.class, "slot");
        this.user("(inventory )?slots?").name("Slot").description("Represents a single slot of an <a href='#inventory'>inventory</a>. Notable slots are the <a href='#ExprArmorSlot'>armour slots</a> and <a href='./expressions/#ExprFurnaceSlot'>furnace slots</a>. ", "The most important property that distinguishes a slot from an <a href='#itemstack'>item</a> is its ability to be changed, e.g. it can be set, deleted, enchanted, etc. (Some item expressions can be changed as well, e.g. items stored in variables. For that matter: slots are never saved to variables, only the items they represent at the time when the variable is set).", "Please note that <a href='#ExprTool'>tool</a> can be regarded a slot, but it can actually change it's position, i.e. doesn't represent always the same slot.").usage("").examples("set tool of player to dirt", "delete helmet of the victim", "set the color of the player's tool to green", "enchant the player's chestplate with projectile protection 5").since("").defaultExpression(new EventValueExpression<Slot>(Slot.class)).changer(new SlotChanger()).parser(new SlotParser()).serializeAs(ItemStack.class).property(Property.NAME, "The custom name of the item in the slot, if it has one. Can be set or reset.", Skript.instance(), new SlotNameHandler()).property(Property.DISPLAY_NAME, "The custom name of the item in the slot, if it has one. Can be set or reset.", Skript.instance(), new SlotNameHandler()).property(Property.AMOUNT, "The amount of items in the slot's stack. Can be set.", Skript.instance(), new SlotAmountHandler()).property(Property.IS_EMPTY, "Whether this slot does not contain a (non-air) item.", Skript.instance(), ConditionPropertyHandler.of(slot -> {
            ItemStack item = slot.getItem();
            return item == null || item.getType() == Material.AIR;
        }));
    }

    public static class SlotChanger
    implements Changer<Slot> {
        @Override
        public Class<Object> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.RESET) {
                return null;
            }
            if (mode == Changer.ChangeMode.SET) {
                return new Class[]{ItemType[].class, ItemStack[].class};
            }
            return new Class[]{ItemType.class, ItemStack.class};
        }

        public void change(Slot[] slots, Object @Nullable [] deltas, Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET) {
                if (deltas != null) {
                    if (deltas.length == 1) {
                        Object delta = deltas[0];
                        for (Slot slot : slots) {
                            slot.setItem(delta instanceof ItemStack ? (ItemStack)delta : ((ItemType)delta).getItem().getRandom());
                        }
                    } else if (deltas.length == slots.length) {
                        for (int i = 0; i < slots.length; ++i) {
                            Object delta = deltas[i];
                            slots[i].setItem(delta instanceof ItemStack ? (ItemStack)delta : ((ItemType)delta).getItem().getRandom());
                        }
                    }
                }
                return;
            }
            Object delta = deltas == null ? null : deltas[0];
            block8: for (Slot slot : slots) {
                switch (mode) {
                    case ADD: {
                        ItemStack i;
                        assert (delta != null);
                        if (delta instanceof ItemStack) {
                            i = slot.getItem();
                            if (i != null && i.getType() != Material.AIR && !ItemUtils.itemStacksEqual(i, (ItemStack)delta)) continue block8;
                            if (i != null && i.getType() != Material.AIR) {
                                i.setAmount(Math.min(i.getAmount() + ((ItemStack)delta).getAmount(), i.getMaxStackSize()));
                                slot.setItem(i);
                                continue block8;
                            }
                            slot.setItem((ItemStack)delta);
                            continue block8;
                        }
                        slot.setItem(((ItemType)delta).getItem().addTo(slot.getItem()));
                        continue block8;
                    }
                    case REMOVE: 
                    case REMOVE_ALL: {
                        ItemStack i;
                        assert (delta != null);
                        if (delta instanceof ItemStack) {
                            int a;
                            i = slot.getItem();
                            if (i == null || !ItemUtils.itemStacksEqual(i, (ItemStack)delta)) continue block8;
                            int n = a = mode == Changer.ChangeMode.REMOVE_ALL ? 0 : i.getAmount() - ((ItemStack)delta).getAmount();
                            if (a <= 0) {
                                slot.setItem(null);
                                continue block8;
                            }
                            i.setAmount(a);
                            slot.setItem(i);
                            continue block8;
                        }
                        if (mode == Changer.ChangeMode.REMOVE) {
                            slot.setItem(((ItemType)delta).removeFrom(slot.getItem()));
                            continue block8;
                        }
                        slot.setItem(((ItemType)delta).removeAll(slot.getItem()));
                        continue block8;
                    }
                    case DELETE: {
                        slot.setItem(null);
                        continue block8;
                    }
                    case RESET: {
                        assert (false);
                        continue block8;
                    }
                }
            }
        }
    }

    private static class SlotParser
    extends Parser<Slot> {
        private SlotParser() {
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(Slot o, int flags) {
            ItemStack i = o.getItem();
            if (i == null) {
                return new ItemType(Material.AIR).toString(flags);
            }
            return ItemType.toString(i, flags);
        }

        @Override
        public String toVariableNameString(Slot o) {
            return "slot:" + o.toString();
        }
    }

    private static class SlotNameHandler
    implements ExpressionPropertyHandler<Slot, Component> {
        private SlotNameHandler() {
        }

        @Override
        public Component convert(Slot slot) {
            ItemStack stack = slot.getItem();
            if (stack != null && stack.hasItemMeta()) {
                ItemMeta meta = stack.getItemMeta();
                return meta.hasDisplayName() ? meta.displayName() : null;
            }
            return null;
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            Class[] classArray;
            switch (mode) {
                case DELETE: 
                case RESET: 
                case SET: {
                    Class[] classArray2 = new Class[1];
                    classArray = classArray2;
                    classArray2[0] = Component.class;
                    break;
                }
                default: {
                    classArray = null;
                }
            }
            return classArray;
        }

        @Override
        public void change(Slot named, Object @Nullable [] delta, Changer.ChangeMode mode) {
            Component name = delta == null ? null : (Component)delta[0];
            ItemStack stack = named.getItem();
            if (stack != null && !ItemUtils.isAir(stack.getType())) {
                ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : Bukkit.getItemFactory().getItemMeta(stack.getType());
                meta.displayName(name);
                stack.setItemMeta(meta);
                named.setItem(stack);
            }
        }

        @Override
        @NotNull
        public Class<Component> returnType() {
            return Component.class;
        }
    }

    private static class SlotAmountHandler
    implements ExpressionPropertyHandler<Slot, Number> {
        private SlotAmountHandler() {
        }

        @Override
        public Number convert(Slot slot) {
            return slot.getAmount();
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET) {
                return new Class[]{Integer.class};
            }
            return null;
        }

        @Override
        public void change(Slot slot, Object @Nullable [] delta, Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET) {
                assert (delta != null);
                slot.setAmount((Integer)delta[0]);
            }
        }

        @Override
        @NotNull
        public Class<Number> returnType() {
            return Number.class;
        }
    }
}

