/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.persistence.PersistentDataContainerView
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.TileState
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataHolder
 */
package org.skriptlang.skript.bukkit.pdc;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.slot.Slot;
import io.papermc.paper.persistence.PersistentDataContainerView;
import java.lang.runtime.SwitchBootstraps;
import java.util.function.Consumer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;

public final class PDCUtils {
    public static PersistentDataContainerView getPersistentDataContainer(Object holder) {
        return PDCUtils.getPersistentDataContainer(holder, container -> {});
    }

    public static PersistentDataContainerView getPersistentDataContainer(Object holder, Consumer<PersistentDataContainerView> consumer) {
        PersistentDataContainer persistentDataContainer;
        Object object = holder;
        int n = 0;
        block7: while (true) {
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{PersistentDataHolder.class, ItemType.class, ItemStack.class, Slot.class, Block.class}, (Object)object, n)) {
                case 0: {
                    PersistentDataHolder dataHolder = (PersistentDataHolder)object;
                    persistentDataContainer = dataHolder.getPersistentDataContainer();
                    break block7;
                }
                case 1: {
                    ItemType itemType = (ItemType)object;
                    persistentDataContainer = itemType.getItemMeta().getPersistentDataContainer();
                    break block7;
                }
                case 2: {
                    ItemStack itemStack = (ItemStack)object;
                    if (!itemStack.hasItemMeta()) {
                        persistentDataContainer = null;
                        break block7;
                    }
                    persistentDataContainer = itemStack.getPersistentDataContainer();
                    break block7;
                }
                case 3: {
                    Slot slot = (Slot)object;
                    ItemStack item = slot.getItem();
                    if (item == null || !item.hasItemMeta()) {
                        persistentDataContainer = null;
                        break block7;
                    }
                    persistentDataContainer = item.getPersistentDataContainer();
                    break block7;
                }
                case 4: {
                    Block block = (Block)object;
                    BlockState blockState = block.getState();
                    if (!(blockState instanceof TileState)) {
                        n = 5;
                        continue block7;
                    }
                    TileState tileState = (TileState)blockState;
                    persistentDataContainer = tileState.getPersistentDataContainer();
                    break block7;
                }
                default: {
                    persistentDataContainer = null;
                    break block7;
                }
            }
            break;
        }
        PersistentDataContainer container = persistentDataContainer;
        if (container == null) {
            return null;
        }
        consumer.accept((PersistentDataContainerView)container);
        return container;
    }

    public static void editPersistentDataContainer(Object holder, Consumer<PersistentDataContainer> consumer) {
        Object object = holder;
        int n = 0;
        block7: while (true) {
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{PersistentDataHolder.class, ItemType.class, ItemStack.class, Slot.class, Block.class}, (Object)object, n)) {
                case 0: {
                    PersistentDataHolder dataHolder = (PersistentDataHolder)object;
                    consumer.accept(dataHolder.getPersistentDataContainer());
                    break block7;
                }
                case 1: {
                    ItemType itemType = (ItemType)object;
                    ItemMeta meta = itemType.getItemMeta();
                    consumer.accept(meta.getPersistentDataContainer());
                    itemType.setItemMeta(meta);
                    break block7;
                }
                case 2: {
                    ItemStack itemStack = (ItemStack)object;
                    if (!itemStack.hasItemMeta()) {
                        return;
                    }
                    itemStack.editPersistentDataContainer(consumer);
                    break block7;
                }
                case 3: {
                    Slot slot = (Slot)object;
                    ItemStack item = slot.getItem();
                    if (item == null || !item.hasItemMeta()) {
                        return;
                    }
                    item.editPersistentDataContainer(consumer);
                    slot.setItem(item);
                    break block7;
                }
                case 4: {
                    Block block = (Block)object;
                    BlockState blockState = block.getState();
                    if (!(blockState instanceof TileState)) {
                        n = 5;
                        continue block7;
                    }
                    TileState tileState = (TileState)blockState;
                    consumer.accept(tileState.getPersistentDataContainer());
                    tileState.update();
                    break block7;
                }
            }
            break;
        }
    }
}

