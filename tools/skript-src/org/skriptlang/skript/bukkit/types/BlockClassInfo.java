/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.Nameable
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.data.BlockData
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
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.util.BlockUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Internal
public class BlockClassInfo
extends ClassInfo<Block> {
    public BlockClassInfo() {
        super(Block.class, "block");
        this.user("blocks?").name("Block").description("A block in a <a href='#world'>world</a>. It has a <a href='#location'>location</a> and a <a href='#itemstack'>type</a>, and can also have a <a href='#direction'>direction</a> (mostly a <a href='#ExprFacing'>facing</a>), an <a href='#inventory'>inventory</a>, or other special properties.").usage("").examples("").since("1.0").defaultExpression(new EventValueExpression<Block>(Block.class)).parser(new BlockParser()).changer(new BlockChanger()).serializer(new BlockSerializer()).property(Property.NAME, "The custom name of the block, if it has one. Only TileEntities like chests and furnaces can have names. Can be set or reset.", Skript.instance(), new BlockNameHandler());
    }

    private static class BlockParser
    extends Parser<Block> {
        private BlockParser() {
        }

        @Override
        @Nullable
        public Block parse(String s, ParseContext context) {
            return null;
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(Block b, int flags) {
            return BlockUtils.blockToString(b, flags);
        }

        @Override
        public String toVariableNameString(Block b) {
            return b.getWorld().getName() + ":" + b.getX() + "," + b.getY() + "," + b.getZ();
        }

        @Override
        public String getDebugMessage(Block b) {
            return this.toString(b, 0) + " block (" + b.getWorld().getName() + ":" + b.getX() + "," + b.getY() + "," + b.getZ() + ")";
        }
    }

    public static class BlockChanger
    implements Changer<Block> {
        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.RESET) {
                return null;
            }
            if (mode == Changer.ChangeMode.SET) {
                return CollectionUtils.array(ItemType.class, BlockData.class);
            }
            return CollectionUtils.array(ItemType[].class, Inventory[].class);
        }

        public void change(Block[] blocks, Object @Nullable [] delta, Changer.ChangeMode mode) {
            block6: for (Block block : blocks) {
                assert (block != null);
                switch (mode) {
                    case SET: {
                        assert (delta != null);
                        Object object = delta[0];
                        if (object instanceof ItemType) {
                            ItemType itemType = (ItemType)object;
                            itemType.getBlock().setBlock(block, true);
                            continue block6;
                        }
                        if (!(object instanceof BlockData)) continue block6;
                        BlockData blockData = (BlockData)object;
                        block.setBlockData(blockData);
                        continue block6;
                    }
                    case DELETE: {
                        block.setType(Material.AIR, true);
                        continue block6;
                    }
                    case ADD: 
                    case REMOVE: 
                    case REMOVE_ALL: {
                        assert (delta != null);
                        BlockState state = block.getState();
                        if (!(state instanceof InventoryHolder)) continue block6;
                        InventoryHolder inventoryHolder = (InventoryHolder)state;
                        Inventory invi = inventoryHolder.getInventory();
                        if (mode == Changer.ChangeMode.ADD) {
                            for (Object obj : delta) {
                                if (obj instanceof Inventory) {
                                    Inventory itemStacks = (Inventory)obj;
                                    for (ItemStack itemStack : itemStacks) {
                                        if (itemStack == null) continue;
                                        invi.addItem(new ItemStack[]{itemStack});
                                    }
                                    continue;
                                }
                                ((ItemType)obj).addTo(invi);
                            }
                        } else {
                            for (Object obj : delta) {
                                if (obj instanceof Inventory) {
                                    Inventory inventory = (Inventory)obj;
                                    invi.removeItem((ItemStack[])Arrays.stream(inventory.getContents()).filter(Objects::nonNull).toArray(ItemStack[]::new));
                                    continue;
                                }
                                if (mode == Changer.ChangeMode.REMOVE) {
                                    ((ItemType)obj).removeFrom(invi);
                                    continue;
                                }
                                ((ItemType)obj).removeAll(invi);
                            }
                        }
                        state.update();
                        continue block6;
                    }
                    case RESET: {
                        assert (false);
                        continue block6;
                    }
                }
            }
        }
    }

    private static class BlockSerializer
    extends Serializer<Block> {
        private BlockSerializer() {
        }

        @Override
        public Fields serialize(Block b) {
            Fields f = new Fields();
            f.putObject("world", b.getWorld());
            f.putPrimitive("x", b.getX());
            f.putPrimitive("y", b.getY());
            f.putPrimitive("z", b.getZ());
            return f;
        }

        @Override
        public void deserialize(Block o, Fields f) {
            assert (false);
        }

        @Override
        protected Block deserialize(Fields fields) throws StreamCorruptedException {
            World w = fields.getObject("world", World.class);
            int x = fields.getPrimitive("x", Integer.TYPE);
            int y = fields.getPrimitive("y", Integer.TYPE);
            int z = fields.getPrimitive("z", Integer.TYPE);
            if (w == null) {
                throw new StreamCorruptedException();
            }
            return w.getBlockAt(x, y, z);
        }

        @Override
        public boolean mustSyncDeserialization() {
            return true;
        }

        @Override
        public boolean canBeInstantiated() {
            return false;
        }
    }

    private static class BlockNameHandler
    implements ExpressionPropertyHandler<Block, Component> {
        private BlockNameHandler() {
        }

        @Override
        public Component convert(Block block) {
            BlockState state = block.getState();
            if (state instanceof Nameable) {
                Nameable nameable = (Nameable)state;
                return nameable.customName();
            }
            return null;
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
                return new Class[]{Component.class};
            }
            return null;
        }

        @Override
        public void change(Block named, Object @Nullable [] delta, Changer.ChangeMode mode) {
            assert (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET);
            Component name = delta == null ? null : (Component)delta[0];
            BlockState state = named.getState();
            if (state instanceof Nameable) {
                Nameable nameable = (Nameable)state;
                nameable.customName(name);
                state.update(true, false);
            }
        }

        @Override
        @NotNull
        public Class<Component> returnType() {
            return Component.class;
        }
    }
}

