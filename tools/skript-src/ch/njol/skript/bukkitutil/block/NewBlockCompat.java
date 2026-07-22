/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Keyed
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Tag
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.BlockSupport
 *  org.bukkit.block.data.Bisected
 *  org.bukkit.block.data.Bisected$Half
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.Directional
 *  org.bukkit.block.data.type.Bed
 *  org.bukkit.block.data.type.Bed$Part
 *  org.bukkit.block.data.type.Snow
 *  org.bukkit.entity.FallingBlock
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil.block;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.MatchQuality;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.bukkitutil.block.BlockCompat;
import ch.njol.skript.bukkitutil.block.BlockSetter;
import ch.njol.skript.bukkitutil.block.BlockValues;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NewBlockCompat
implements BlockCompat {
    private NewBlockSetter setter = new NewBlockSetter();

    @Override
    @Deprecated(since="2.8.4", forRemoval=true)
    @Nullable
    public BlockValues getBlockValues(BlockState blockState) {
        return this.getBlockValues(blockState.getBlockData());
    }

    @Override
    @Nullable
    public BlockValues getBlockValues(Material material) {
        if (material.isBlock()) {
            return new NewBlockValues(material, Bukkit.createBlockData((Material)material), true);
        }
        return null;
    }

    @Override
    @Nullable
    public BlockValues getBlockValues(BlockData blockData) {
        return new NewBlockValues(blockData.getMaterial(), blockData, false);
    }

    @Override
    @Nullable
    public BlockValues getBlockValues(ItemStack stack) {
        Material type = stack.getType();
        if (type.isBlock()) {
            return new NewBlockValues(type, Bukkit.createBlockData((Material)type), true);
        }
        return null;
    }

    @Override
    public BlockSetter getSetter() {
        return this.setter;
    }

    @Override
    @Deprecated(since="2.8.4", forRemoval=true)
    public BlockState fallingBlockToState(FallingBlock entity) {
        BlockState state = entity.getLocation().getBlock().getState();
        state.setBlockData(entity.getBlockData());
        return state;
    }

    @Override
    @Nullable
    public BlockValues createBlockValues(Material type, Map<String, String> states, @Nullable ItemStack item, int itemFlags) {
        if (states.isEmpty()) {
            if (type.isBlock()) {
                BlockData data = Bukkit.createBlockData((Material)type, (String)"[]");
                assert (data != null);
                return new NewBlockValues(type, data, true);
            }
            return null;
        }
        StringBuilder combined = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, String> entry : states.entrySet()) {
            if (first) {
                first = false;
            } else {
                combined.append(',');
            }
            combined.append(entry.getKey()).append('=').append(entry.getValue());
        }
        combined.append(']');
        try {
            BlockData data = Bukkit.createBlockData((Material)type, (String)combined.toString());
            assert (data != null);
            return new NewBlockValues(type, data, false);
        }
        catch (IllegalArgumentException e) {
            Skript.error("Parsing block state " + String.valueOf(combined) + " failed!");
            if (Skript.debug()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public boolean isEmpty(Material type) {
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    @Override
    public boolean isLiquid(Material type) {
        return type == Material.WATER || type == Material.LAVA;
    }

    private static class NewBlockSetter
    implements BlockSetter {
        private static final BlockFace[] CARDINAL_FACES = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        private static final boolean HAS_BLOCK_SUPPORT = Skript.classExists("org.bukkit.block.BlockSupport");

        @Override
        public void setBlock(Block block, Material type, @Nullable BlockValues values, int flags) {
            Bed data;
            boolean rotate = (flags & 1) != 0;
            boolean rotateForce = (flags & 2) != 0;
            boolean rotateFixType = (flags & 4) != 0;
            boolean multipart = (flags & 8) != 0;
            boolean applyPhysics = (flags & 0x10) != 0;
            NewBlockValues ourValues = null;
            if (values != null) {
                ourValues = (NewBlockValues)values;
            }
            Class dataType = type.data;
            boolean placed = false;
            if (rotate) {
                Block relative;
                BlockFace face;
                if (type == Material.TORCH || rotateFixType && type == Material.WALL_TORCH) {
                    Block under = block.getRelative(0, -1, 0);
                    boolean canPlace = NewBlockSetter.canSupportFloorTorch(under);
                    if (!canPlace && (face = this.findWallTorchSide(block)) != null) {
                        block.setType(Material.WALL_TORCH);
                        Directional data2 = (Directional)block.getBlockData();
                        data2.setFacing(face);
                        block.setBlockData((BlockData)data2, applyPhysics);
                        placed = true;
                    }
                } else if (type == Material.WALL_TORCH && (!NewBlockSetter.canSupportWallTorch(relative = block.getRelative((data = ourValues != null ? (Directional)ourValues.data : (Directional)Bukkit.createBlockData((Material)type)).getFacing()), data.getFacing().getOppositeFace()) || rotateForce) && (face = this.findWallTorchSide(block)) != null) {
                    block.setType(type);
                    data.setFacing(face);
                    block.setBlockData((BlockData)data, applyPhysics);
                    placed = true;
                }
            }
            if (multipart) {
                if (Bed.class.isAssignableFrom(dataType)) {
                    BlockFace facing;
                    data = ourValues != null ? (Bed)ourValues.data.clone() : (Bed)Bukkit.createBlockData((Material)type);
                    block.setType(type, false);
                    block.setBlockData((BlockData)data, applyPhysics);
                    BlockFace otherFacing = facing = data.getFacing();
                    Bed.Part otherPart = Bed.Part.HEAD;
                    if (data.getPart().equals((Object)Bed.Part.HEAD)) {
                        facing = facing.getOppositeFace();
                        otherPart = Bed.Part.FOOT;
                    }
                    Block other = block.getRelative(facing);
                    other.setType(type, false);
                    data.setPart(otherPart);
                    data.setFacing(otherFacing);
                    other.setBlockData((BlockData)data, applyPhysics);
                    placed = true;
                }
                if (Bisected.class.isAssignableFrom(dataType) && !Tag.STAIRS.isTagged((Keyed)type) && !Tag.TRAPDOORS.isTagged((Keyed)type)) {
                    data = ourValues != null ? (Bisected)ourValues.data.clone() : (Bisected)Bukkit.createBlockData((Material)type);
                    block.setType(type, false);
                    block.setBlockData((BlockData)data, applyPhysics);
                    BlockFace facing = BlockFace.DOWN;
                    Bisected.Half otherHalf = Bisected.Half.BOTTOM;
                    if (data.getHalf().equals((Object)Bisected.Half.BOTTOM)) {
                        facing = BlockFace.UP;
                        otherHalf = Bisected.Half.TOP;
                    }
                    Block other = block.getRelative(facing);
                    other.setType(type, false);
                    data.setHalf(otherHalf);
                    other.setBlockData((BlockData)data, applyPhysics);
                    placed = true;
                }
            }
            if (!placed) {
                block.setType(type);
                if (ourValues != null && !ourValues.isDefault()) {
                    block.setBlockData(ourValues.data, applyPhysics);
                }
            }
        }

        private static boolean canSupportFloorTorch(Block block) {
            if (HAS_BLOCK_SUPPORT) {
                return block.getBlockData().isFaceSturdy(BlockFace.UP, BlockSupport.CENTER);
            }
            Material material = block.getType();
            return material.isOccluding() || NewBlockSetter.canSupportWallTorch(block, null) || ItemUtils.isFence(block) || ItemUtils.isGlass(material) || material == Material.HOPPER || material == Material.SNOW && ((Snow)block.getBlockData()).getLayers() == 8;
        }

        private static boolean canSupportWallTorch(Block block, @Nullable BlockFace face) {
            if (HAS_BLOCK_SUPPORT && face != null) {
                return block.getBlockData().isFaceSturdy(face, BlockSupport.FULL);
            }
            Material material = block.getType();
            return material.isOccluding() || material == Material.SOUL_SAND || material == Material.SPAWNER;
        }

        @Nullable
        private BlockFace findWallTorchSide(Block block) {
            for (BlockFace face : CARDINAL_FACES) {
                Block relative = block.getRelative(face);
                if (!relative.getType().isOccluding() && !NewBlockSetter.canSupportWallTorch(relative, face.getOppositeFace())) continue;
                return face.getOppositeFace();
            }
            return null;
        }

        @Override
        public void sendBlockChange(Player player, Location location, Material type, @Nullable BlockValues values) {
            BlockData blockData = values != null ? ((NewBlockValues)values).data : type.createBlockData();
            player.sendBlockChange(location, blockData);
        }
    }

    private static class NewBlockValues
    extends BlockValues {
        Material type;
        BlockData data;
        boolean isDefault;

        public NewBlockValues(Material type, BlockData data, boolean isDefault) {
            if (type != data.getMaterial()) {
                throw new IllegalArgumentException("'type' does not match material of 'data'");
            }
            this.type = type;
            this.data = data;
            this.isDefault = isDefault;
        }

        private NewBlockValues() {
        }

        @Override
        public boolean isDefault() {
            return this.isDefault;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (!(other instanceof NewBlockValues)) {
                return false;
            }
            NewBlockValues n = (NewBlockValues)other;
            return (this.data.matches(n.data) || n.data.matches(this.data)) && this.type.equals((Object)n.type);
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + (this.data == null ? 0 : this.data.hashCode());
            result = prime * result + this.type.hashCode();
            return result;
        }

        public String toString() {
            return this.data.toString() + (this.isDefault ? " (default)" : "");
        }

        @Override
        public MatchQuality match(BlockValues other) {
            if (!(other instanceof NewBlockValues)) {
                throw new IllegalArgumentException("wrong block compat");
            }
            NewBlockValues n = (NewBlockValues)other;
            if (this.type == n.type) {
                if (this.data.equals((Object)n.data)) {
                    return MatchQuality.EXACT;
                }
                if (this.data.matches(n.data)) {
                    return MatchQuality.SAME_ITEM;
                }
                return MatchQuality.SAME_MATERIAL;
            }
            return MatchQuality.DIFFERENT;
        }

        @Override
        public Fields serialize() {
            Fields fields = new Fields();
            fields.putObject("data", this.data.getAsString());
            fields.putPrimitive("isDefault", this.isDefault);
            return fields;
        }

        @Override
        public void deserialize(@NotNull Fields fields) throws StreamCorruptedException {
            String data = fields.getObject("data", String.class);
            boolean isDefault = fields.getPrimitive("isDefault", Boolean.class);
            if (data == null) {
                throw new StreamCorruptedException("'data' is missing.");
            }
            try {
                this.data = Bukkit.createBlockData((String)data);
            }
            catch (IllegalArgumentException e) {
                throw new StreamCorruptedException("Invalid block data: " + data);
            }
            this.type = this.data.getMaterial();
            this.isDefault = isDefault;
        }

        static {
            Variables.yggdrasil.registerSingleClass(NewBlockValues.class, "NewBlockValues");
        }
    }
}

