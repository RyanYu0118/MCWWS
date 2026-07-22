/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.types;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.ContainsHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Internal
public class InventoryClassInfo
extends ClassInfo<Inventory> {
    public InventoryClassInfo() {
        super(Inventory.class, "inventory");
        this.user("inventor(y|ies)").name("Inventory").description("An inventory of a <a href='#player'>player</a> or <a href='#block'>block</a>. Inventories have many effects and conditions regarding the items contained.", "An inventory has a fixed amount of <a href='#slot'>slots</a> which represent a specific place in the inventory, e.g. the <a href='#ExprArmorSlot'>helmet slot</a> for players (Please note that slot support is still very limited but will be improved eventually).").usage("").examples("").since("1.0").defaultExpression(new EventValueExpression<Inventory>(Inventory.class)).parser(new InventoryParser()).changer(new InventoryChanger()).property(Property.CONTAINS, "Inventories can contain items.", Skript.instance(), new InventoryContainsHandler()).property(Property.NAME, "The name of the inventory. Can be set or reset.", Skript.instance(), new InventoryNameHandler()).property(Property.DISPLAY_NAME, "The name of the inventory. Can be set or reset.", Skript.instance(), new InventoryNameHandler()).property(Property.IS_EMPTY, "Whether the inventory contains no items (all slots contain air).", Skript.instance(), ConditionPropertyHandler.of(inventory -> {
            for (ItemStack s : inventory.getContents()) {
                if (s == null || s.getType() == Material.AIR) continue;
                return false;
            }
            return true;
        }));
    }

    private static class InventoryParser
    extends Parser<Inventory> {
        private InventoryParser() {
        }

        @Override
        @Nullable
        public Inventory parse(String s, ParseContext context) {
            return null;
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(Inventory i, int flags) {
            return "inventory of " + Classes.toString(i.getHolder());
        }

        @Override
        public String getDebugMessage(Inventory i) {
            return "inventory of " + Classes.getDebugMessage(i.getHolder());
        }

        @Override
        public String toVariableNameString(Inventory i) {
            return "inventory of " + Classes.toString(i.getHolder(), StringMode.VARIABLE_NAME);
        }
    }

    public static class InventoryChanger
    implements Changer<Inventory> {
        private final Material[] cachedMaterials = Material.values();

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.RESET) {
                return null;
            }
            if (mode == Changer.ChangeMode.REMOVE_ALL) {
                return CollectionUtils.array(ItemType[].class);
            }
            if (mode == Changer.ChangeMode.SET) {
                return CollectionUtils.array(ItemType[].class, Inventory.class);
            }
            return CollectionUtils.array(ItemType[].class, Inventory[].class);
        }

        public void change(Inventory[] inventories, Object @Nullable [] delta, Changer.ChangeMode mode) {
            for (Inventory inventory : inventories) {
                assert (inventory != null);
                switch (mode) {
                    case DELETE: {
                        inventory.clear();
                        break;
                    }
                    case SET: {
                        inventory.clear();
                    }
                    case ADD: {
                        assert (delta != null);
                        if (delta instanceof ItemStack[]) {
                            ItemStack[] items = (ItemStack[])delta;
                            if (items.length > 36) {
                                return;
                            }
                            for (Object d : delta) {
                                if (d instanceof Inventory) {
                                    Inventory itemStacks = (Inventory)d;
                                    for (ItemStack itemStack : itemStacks) {
                                        if (itemStack == null) continue;
                                        inventory.addItem(new ItemStack[]{itemStack});
                                    }
                                    continue;
                                }
                                ((ItemType)d).addTo(inventory);
                            }
                            break;
                        }
                        for (Object d : delta) {
                            if (d instanceof ItemStack) {
                                ItemStack stack = (ItemStack)d;
                                new ItemType(stack).addTo(inventory);
                                continue;
                            }
                            if (d instanceof ItemType) {
                                ItemType itemType = (ItemType)d;
                                itemType.addTo(inventory);
                                continue;
                            }
                            if (d instanceof Block) {
                                Block block = (Block)d;
                                new ItemType(block).addTo(inventory);
                                continue;
                            }
                            Skript.error("Can't " + d.toString() + " to an inventory!");
                        }
                        break;
                    }
                    case REMOVE: 
                    case REMOVE_ALL: {
                        assert (delta != null);
                        if (delta.length == this.cachedMaterials.length) {
                            boolean equal = true;
                            for (int i = 0; i < delta.length; ++i) {
                                Object object = delta[i];
                                if (!(object instanceof ItemType)) {
                                    equal = false;
                                    break;
                                }
                                ItemType itemType = (ItemType)object;
                                if (itemType.getMaterial() == this.cachedMaterials[i]) continue;
                                equal = false;
                                break;
                            }
                            if (equal) {
                                inventory.clear();
                                break;
                            }
                        }
                        for (Object d : delta) {
                            if (d instanceof Inventory) {
                                Inventory itemStacks = (Inventory)d;
                                assert (mode == Changer.ChangeMode.REMOVE);
                                for (ItemStack itemStack : itemStacks) {
                                    if (itemStack == null) continue;
                                    inventory.removeItem(new ItemStack[]{itemStack});
                                }
                                continue;
                            }
                            if (mode == Changer.ChangeMode.REMOVE) {
                                ((ItemType)d).removeFrom(inventory);
                                continue;
                            }
                            ((ItemType)d).removeAll(inventory);
                        }
                        break;
                    }
                    case RESET: {
                        assert (false);
                        break;
                    }
                }
                InventoryHolder holder = inventory.getHolder();
                if (!(holder instanceof Player)) continue;
                Player player = (Player)holder;
                player.updateInventory();
            }
        }
    }

    private static class InventoryContainsHandler
    implements ContainsHandler<Inventory, Object> {
        private InventoryContainsHandler() {
        }

        @Override
        public boolean contains(Inventory container, Object element) {
            if (element instanceof ItemType) {
                ItemType type = (ItemType)element;
                return type.isContainedIn((Iterable<ItemStack>)container);
            }
            if (element instanceof ItemStack) {
                ItemStack stack = (ItemStack)element;
                return container.containsAtLeast(stack, stack.getAmount());
            }
            return false;
        }

        @Override
        public Class<?>[] elementTypes() {
            return new Class[]{ItemType.class, ItemStack.class};
        }
    }

    private static class InventoryNameHandler
    implements ExpressionPropertyHandler<Inventory, Component> {
        private InventoryNameHandler() {
        }

        @Override
        public Component convert(Event event, Inventory inventory) {
            InventoryEvent inventoryEvent;
            if (event instanceof InventoryEvent && (inventoryEvent = (InventoryEvent)event).getInventory() == inventory) {
                return inventoryEvent.getView().title();
            }
            return this.convert(inventory);
        }

        @Override
        @Nullable
        public Component convert(Inventory inventory) {
            if (inventory.getViewers().isEmpty()) {
                return null;
            }
            return ((HumanEntity)inventory.getViewers().getFirst()).getOpenInventory().title();
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
                return new Class[]{Component.class};
            }
            return null;
        }

        @Override
        public void change(Inventory inventory, Object @Nullable [] delta, Changer.ChangeMode mode) {
            Component name;
            Component component = name = delta == null || delta.length == 0 ? null : (Component)delta[0];
            if (inventory.getViewers().isEmpty()) {
                return;
            }
            ArrayList viewers = new ArrayList(inventory.getViewers());
            InventoryType type = inventory.getType();
            if (!type.isCreatable()) {
                return;
            }
            if (name == null) {
                name = type.defaultTitle();
            }
            Inventory copy = type == InventoryType.CHEST ? Bukkit.createInventory((InventoryHolder)inventory.getHolder(), (int)inventory.getSize(), (Component)name) : Bukkit.createInventory((InventoryHolder)inventory.getHolder(), (InventoryType)type, (Component)name);
            copy.setContents(inventory.getContents());
            viewers.forEach(viewer -> viewer.openInventory(copy));
        }

        @Override
        @NotNull
        public Class<Component> returnType() {
            return Component.class;
        }
    }
}

