/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.block.BlockSoundGroup
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.FluidCollisionMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.SoundGroup
 *  org.bukkit.World
 *  org.bukkit.block.Biome
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.PistonMoveReaction
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.util.BoundingBox
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Vector
 *  org.bukkit.util.VoxelShape
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.block.BlockCompat;
import com.destroystokyo.paper.block.BlockSoundGroup;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockStateBlock
implements Block {
    private static final boolean ISPASSABLE_METHOD_EXISTS = Skript.methodExists(Block.class, "isPassable", new Class[0]);
    final BlockState state;
    private final boolean delayChanges;
    private final boolean isPassable;

    public BlockStateBlock(BlockState state) {
        this(state, false);
    }

    public BlockStateBlock(BlockState state, boolean delayChanges) {
        assert (state != null);
        this.state = state;
        this.isPassable = ISPASSABLE_METHOD_EXISTS && state.isPlaced() ? state.getBlock().isPassable() : false;
        this.delayChanges = delayChanges;
    }

    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.state.setMetadata(metadataKey, newMetadataValue);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.state.getMetadata(metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return this.state.hasMetadata(metadataKey);
    }

    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.state.removeMetadata(metadataKey, owningPlugin);
    }

    public byte getData() {
        return this.state.getRawData();
    }

    public Block getRelative(int modX, int modY, int modZ) {
        return this.state.getBlock().getRelative(modX, modY, modZ);
    }

    public Block getRelative(BlockFace face) {
        return this.state.getBlock().getRelative(face);
    }

    public Block getRelative(BlockFace face, int distance) {
        return this.state.getBlock().getRelative(face, distance);
    }

    public Material getType() {
        return this.state.getType();
    }

    public byte getLightLevel() {
        return this.state.getLightLevel();
    }

    public byte getLightFromSky() {
        return this.state.getBlock().getLightFromSky();
    }

    public byte getLightFromBlocks() {
        return this.state.getBlock().getLightFromBlocks();
    }

    public World getWorld() {
        return this.state.getWorld();
    }

    public int getX() {
        return this.state.getX();
    }

    public int getY() {
        return this.state.getY();
    }

    public int getZ() {
        return this.state.getZ();
    }

    public Location getLocation() {
        return this.state.getLocation();
    }

    public Chunk getChunk() {
        return this.state.getChunk();
    }

    public void setType(final Material type) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().setType(type);
                }
            });
        } else {
            this.state.setType(type);
        }
    }

    @Nullable
    public BlockFace getFace(Block block) {
        return this.state.getBlock().getFace(block);
    }

    public BlockState getState() {
        return this.state;
    }

    public BlockState getState(boolean useSnapshot) {
        return this.state;
    }

    public Biome getBiome() {
        return this.state.getBlock().getBiome();
    }

    @NotNull
    public Biome getComputedBiome() {
        return this.state.getBlock().getComputedBiome();
    }

    public void setBiome(Biome bio) {
        this.state.getBlock().setBiome(bio);
    }

    public boolean isBlockPowered() {
        return this.state.getBlock().isBlockPowered();
    }

    public boolean isBlockIndirectlyPowered() {
        return this.state.getBlock().isBlockIndirectlyPowered();
    }

    public boolean isBlockFacePowered(BlockFace face) {
        return this.state.getBlock().isBlockFacePowered(face);
    }

    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return this.state.getBlock().isBlockFaceIndirectlyPowered(face);
    }

    public int getBlockPower(BlockFace face) {
        return this.state.getBlock().getBlockPower(face);
    }

    public int getBlockPower() {
        return this.state.getBlock().getBlockPower();
    }

    public boolean isEmpty() {
        Material type = this.getType();
        assert (type != null);
        return BlockCompat.INSTANCE.isEmpty(type);
    }

    public boolean isLiquid() {
        Material type = this.getType();
        assert (type != null);
        return BlockCompat.INSTANCE.isLiquid(type);
    }

    public boolean isBuildable() {
        return this.state.getBlock().isBuildable();
    }

    public boolean isBurnable() {
        return this.state.getBlock().isBurnable();
    }

    public boolean isReplaceable() {
        return this.state.getBlock().isReplaceable();
    }

    public boolean isSolid() {
        return this.state.getBlock().isSolid();
    }

    public boolean isCollidable() {
        return this.state.getBlock().isCollidable();
    }

    public double getTemperature() {
        return this.state.getBlock().getTemperature();
    }

    public double getHumidity() {
        return this.state.getBlock().getHumidity();
    }

    public PistonMoveReaction getPistonMoveReaction() {
        throw new UnsupportedOperationException();
    }

    public boolean breakNaturally() {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){

                @Override
                public void run() {
                    BlockStateBlock.this.state.getBlock().breakNaturally();
                }
            });
            return true;
        }
        return false;
    }

    public boolean breakNaturally(final @Nullable ItemStack tool) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().breakNaturally(tool);
                }
            });
            return true;
        }
        return false;
    }

    public boolean breakNaturally(final boolean triggerEffect) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().breakNaturally(triggerEffect);
                }
            });
            return true;
        }
        return false;
    }

    public boolean breakNaturally(final ItemStack tool, final boolean triggerEffect) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().breakNaturally(tool, triggerEffect);
                }
            });
            return true;
        }
        return false;
    }

    public boolean breakNaturally(final @NotNull ItemStack tool, final boolean triggerEffect, final boolean dropExperience, final boolean forceEffect) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().breakNaturally(tool, triggerEffect, dropExperience, forceEffect);
                }
            });
            return true;
        }
        return false;
    }

    public void tick() {
        this.state.getBlock().tick();
    }

    public void fluidTick() {
        this.state.getBlock().fluidTick();
    }

    public void randomTick() {
        this.state.getBlock().randomTick();
    }

    public boolean applyBoneMeal(BlockFace blockFace) {
        return this.state.getBlock().applyBoneMeal(blockFace);
    }

    public Collection<ItemStack> getDrops() {
        assert (false);
        return Collections.emptySet();
    }

    public Collection<ItemStack> getDrops(@Nullable ItemStack tool) {
        assert (false);
        return Collections.emptySet();
    }

    public Collection<ItemStack> getDrops(ItemStack tool, @Nullable Entity entity) {
        assert (false);
        return Collections.emptySet();
    }

    @Nullable
    public Location getLocation(@Nullable Location loc) {
        if (loc != null) {
            loc.setWorld(this.getWorld());
            loc.setX((double)this.getX());
            loc.setY((double)this.getY());
            loc.setZ((double)this.getZ());
            loc.setPitch(0.0f);
            loc.setYaw(0.0f);
        }
        return loc;
    }

    public void setType(final Material type, final boolean applyPhysics) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().setType(type, applyPhysics);
                }
            });
        } else {
            this.state.setType(type);
        }
    }

    public BlockData getBlockData() {
        return this.state.getBlockData();
    }

    public void setBlockData(final BlockData data) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().setBlockData(data);
                }
            });
        } else {
            this.state.setBlockData(data);
        }
    }

    public void setBlockData(final BlockData data, final boolean applyPhysics) {
        if (this.delayChanges) {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ BlockStateBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.state.getBlock().setBlockData(data, applyPhysics);
                }
            });
        } else {
            this.state.setBlockData(data);
        }
    }

    @Nullable
    public RayTraceResult rayTrace(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode) {
        return this.state.getBlock().rayTrace(start, direction, maxDistance, fluidCollisionMode);
    }

    public boolean isPassable() {
        return this.isPassable;
    }

    public BoundingBox getBoundingBox() {
        return this.state.getBlock().getBoundingBox();
    }

    public BlockSoundGroup getSoundGroup() {
        return this.state.getBlock().getSoundGroup();
    }

    @NotNull
    public SoundGroup getBlockSoundGroup() {
        return this.state.getBlock().getBlockSoundGroup();
    }

    public String getTranslationKey() {
        return this.state.getBlock().getTranslationKey();
    }

    public float getDestroySpeed(ItemStack itemStack) {
        return this.state.getBlock().getDestroySpeed(itemStack);
    }

    public boolean isPreferredTool(@NotNull ItemStack tool) {
        return this.state.getBlock().isPreferredTool(tool);
    }

    public boolean isValidTool(@NotNull ItemStack itemStack) {
        return this.state.getBlock().isValidTool(itemStack);
    }

    @NotNull
    public float getDestroySpeed(@NotNull ItemStack itemStack, boolean considerEnchants) {
        return this.state.getBlock().getDestroySpeed(itemStack, considerEnchants);
    }

    public boolean isSuffocating() {
        return this.state.getBlock().isSuffocating();
    }

    @NotNull
    public VoxelShape getCollisionShape() {
        return this.state.getBlock().getCollisionShape();
    }

    public boolean canPlace(@NotNull BlockData data) {
        return this.state.getBlock().canPlace(data);
    }

    public float getBreakSpeed(@NotNull Player player) {
        return this.state.getBlock().getBreakSpeed(player);
    }

    @NotNull
    public String translationKey() {
        return this.state.getBlock().getTranslationKey();
    }

    public boolean breakNaturally(boolean triggerEffect, boolean dropExperience) {
        return this.state.getBlock().breakNaturally(triggerEffect, dropExperience);
    }

    public boolean breakNaturally(@NotNull ItemStack tool, boolean triggerEffect, boolean dropExperience) {
        return this.state.getBlock().breakNaturally(tool, triggerEffect, dropExperience);
    }
}

