/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes.data;

import ch.njol.skript.classes.Changer;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.types.BlockClassInfo;
import org.skriptlang.skript.bukkit.types.EntityClassInfo;
import org.skriptlang.skript.bukkit.types.InventoryClassInfo;
import org.skriptlang.skript.bukkit.types.PlayerClassInfo;

public class DefaultChangers {
    public static final Changer<Entity> entityChanger = new EntityClassInfo.EntityChanger();
    public static final Changer<Player> playerChanger = new PlayerClassInfo.PlayerChanger();
    public static final Changer<Entity> nonLivingEntityChanger = new Changer<Entity>(){

        @Override
        @Nullable
        public Class<Object>[] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.DELETE) {
                return CollectionUtils.array(new Class[0]);
            }
            return null;
        }

        public void change(Entity[] entities, @Nullable Object[] delta, Changer.ChangeMode mode) {
            assert (mode == Changer.ChangeMode.DELETE);
            for (Entity e : entities) {
                if (e instanceof Player) continue;
                e.remove();
            }
        }
    };
    public static final Changer<Item> itemChanger = new Changer<Item>(){

        @Override
        @Nullable
        public Class<?>[] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET) {
                return CollectionUtils.array(ItemStack.class);
            }
            return nonLivingEntityChanger.acceptChange(mode);
        }

        public void change(Item[] what, @Nullable Object[] delta, Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET) {
                assert (delta != null);
                for (Item i : what) {
                    i.setItemStack((ItemStack)delta[0]);
                }
            } else {
                nonLivingEntityChanger.change((Entity[])what, delta, mode);
            }
        }
    };
    public static final Changer<Inventory> inventoryChanger = new InventoryClassInfo.InventoryChanger();
    public static final Changer<Block> blockChanger = new BlockClassInfo.BlockChanger();
}

