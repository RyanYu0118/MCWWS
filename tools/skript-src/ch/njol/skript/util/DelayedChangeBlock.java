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

public class DelayedChangeBlock
implements Block {
    private static final boolean ISPASSABLE_METHOD_EXISTS = Skript.methodExists(Block.class, "isPassable", new Class[0]);
    final Block block;
    @Nullable
    private final BlockState newState;
    private final boolean isPassable;

    public DelayedChangeBlock(Block block) {
        this(block, null);
    }

    public DelayedChangeBlock(Block block, @Nullable BlockState newState) {
        assert (block != null);
        this.block = block;
        this.newState = newState;
        this.isPassable = ISPASSABLE_METHOD_EXISTS && newState != null ? newState.getBlock().isPassable() : false;
    }

    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.block.setMetadata(metadataKey, newMetadataValue);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.block.getMetadata(metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return this.block.hasMetadata(metadataKey);
    }

    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.block.removeMetadata(metadataKey, owningPlugin);
    }

    public byte getData() {
        return this.block.getData();
    }

    public Block getRelative(int modX, int modY, int modZ) {
        return this.block.getRelative(modX, modY, modZ);
    }

    public Block getRelative(BlockFace face) {
        return this.block.getRelative(face);
    }

    public Block getRelative(BlockFace face, int distance) {
        return this.block.getRelative(face, distance);
    }

    public Material getType() {
        return this.block.getType();
    }

    public byte getLightLevel() {
        return this.block.getLightLevel();
    }

    public byte getLightFromSky() {
        return this.block.getLightFromSky();
    }

    public byte getLightFromBlocks() {
        return this.block.getLightFromBlocks();
    }

    public World getWorld() {
        return this.block.getWorld();
    }

    public int getX() {
        return this.block.getX();
    }

    public int getY() {
        return this.block.getY();
    }

    public int getZ() {
        return this.block.getZ();
    }

    public Location getLocation() {
        return this.block.getLocation();
    }

    public Chunk getChunk() {
        return this.block.getChunk();
    }

    public void setType(final Material type) {
        if (this.newState != null) {
            this.newState.setType(type);
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ DelayedChangeBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.block.setType(type);
                }
            });
        }
    }

    @Nullable
    public BlockFace getFace(Block block) {
        return block.getFace(block);
    }

    public BlockState getState() {
        return this.block.getState();
    }

    public BlockState getState(boolean useSnapshot) {
        return this.block.getState(useSnapshot);
    }

    public Biome getBiome() {
        return this.block.getBiome();
    }

    @NotNull
    public Biome getComputedBiome() {
        return this.block.getComputedBiome();
    }

    public void setBiome(Biome bio) {
        this.block.setBiome(bio);
    }

    public boolean isBlockPowered() {
        return this.block.isBlockPowered();
    }

    public boolean isBlockIndirectlyPowered() {
        return this.block.isBlockIndirectlyPowered();
    }

    public boolean isBlockFacePowered(BlockFace face) {
        return this.block.isBlockFacePowered(face);
    }

    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return this.block.isBlockFaceIndirectlyPowered(face);
    }

    public int getBlockPower(BlockFace face) {
        return this.block.getBlockPower(face);
    }

    public int getBlockPower() {
        return this.block.getBlockPower();
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
        return this.block.isBuildable();
    }

    public boolean isBurnable() {
        return this.block.isBurnable();
    }

    public boolean isReplaceable() {
        return this.block.isReplaceable();
    }

    public boolean isSolid() {
        return this.block.isSolid();
    }

    public boolean isCollidable() {
        return this.block.isCollidable();
    }

    public double getTemperature() {
        return this.block.getTemperature();
    }

    public double getHumidity() {
        return this.block.getHumidity();
    }

    public PistonMoveReaction getPistonMoveReaction() {
        return this.block.getPistonMoveReaction();
    }

    public boolean breakNaturally() {
        if (this.newState != null) {
            return false;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){

            @Override
            public void run() {
                DelayedChangeBlock.this.block.breakNaturally();
            }
        });
        return true;
    }

    public boolean breakNaturally(final @Nullable ItemStack tool) {
        if (this.newState != null) {
            return false;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
            final /* synthetic */ DelayedChangeBlock this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            public void run() {
                this.this$0.block.breakNaturally(tool);
            }
        });
        return true;
    }

    public boolean breakNaturally(final boolean triggerEffect) {
        if (this.newState != null) {
            return false;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
            final /* synthetic */ DelayedChangeBlock this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            public void run() {
                this.this$0.block.breakNaturally(triggerEffect);
            }
        });
        return true;
    }

    public boolean breakNaturally(final ItemStack tool, final boolean triggerEffect) {
        if (this.newState != null) {
            return false;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
            final /* synthetic */ DelayedChangeBlock this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            public void run() {
                this.this$0.block.breakNaturally(tool, triggerEffect);
            }
        });
        return true;
    }

    public boolean breakNaturally(final @NotNull ItemStack tool, final boolean triggerEffect, final boolean dropExperience, final boolean forceEffect) {
        if (this.newState != null) {
            return false;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
            final /* synthetic */ DelayedChangeBlock this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            public void run() {
                this.this$0.block.breakNaturally(tool, triggerEffect, dropExperience, forceEffect);
            }
        });
        return true;
    }

    public void tick() {
        this.block.tick();
    }

    public void fluidTick() {
        this.block.fluidTick();
    }

    public void randomTick() {
        this.block.randomTick();
    }

    public boolean applyBoneMeal(BlockFace blockFace) {
        return this.block.applyBoneMeal(blockFace);
    }

    public Collection<ItemStack> getDrops() {
        return this.block.getDrops();
    }

    public Collection<ItemStack> getDrops(@Nullable ItemStack tool) {
        return this.block.getDrops(tool);
    }

    public Collection<ItemStack> getDrops(ItemStack tool, @Nullable Entity entity) {
        return this.block.getDrops(tool, entity);
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
        if (this.newState != null) {
            this.newState.setType(type);
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){
                final /* synthetic */ DelayedChangeBlock this$0;
                {
                    this.this$0 = this$0;
                }

                @Override
                public void run() {
                    this.this$0.block.setType(type, applyPhysics);
                }
            });
        }
    }

    public BlockData getBlockData() {
        return this.block.getBlockData();
    }

    public void setBlockData(BlockData data) {
        this.setBlockData(data, true);
    }

    public void setBlockData(BlockData data, boolean applyPhysics) {
        if (this.newState != null) {
            this.newState.setBlockData(data);
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), () -> this.block.setBlockData(data, applyPhysics));
        }
    }

    @Nullable
    public RayTraceResult rayTrace(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode) {
        return this.block.rayTrace(start, direction, maxDistance, fluidCollisionMode);
    }

    public boolean isPassable() {
        return this.isPassable;
    }

    public BoundingBox getBoundingBox() {
        return this.block.getBoundingBox();
    }

    public BlockSoundGroup getSoundGroup() {
        return this.block.getSoundGroup();
    }

    @NotNull
    public SoundGroup getBlockSoundGroup() {
        return this.block.getBlockSoundGroup();
    }

    public String getTranslationKey() {
        return this.block.getTranslationKey();
    }

    public float getDestroySpeed(ItemStack itemStack) {
        return this.block.getDestroySpeed(itemStack);
    }

    public boolean isPreferredTool(@NotNull ItemStack tool) {
        return this.block.isPreferredTool(tool);
    }

    public boolean isValidTool(@NotNull ItemStack itemStack) {
        return this.block.isValidTool(itemStack);
    }

    public float getDestroySpeed(@NotNull ItemStack itemStack, boolean considerEnchants) {
        return this.block.getDestroySpeed(itemStack, considerEnchants);
    }

    public boolean isSuffocating() {
        return this.block.isSuffocating();
    }

    @NotNull
    public VoxelShape getCollisionShape() {
        return this.block.getCollisionShape();
    }

    public boolean canPlace(@NotNull BlockData data) {
        return this.block.canPlace(data);
    }

    public float getBreakSpeed(@NotNull Player player) {
        return this.block.getBreakSpeed(player);
    }

    @NotNull
    public String translationKey() {
        return this.block.getTranslationKey();
    }

    public boolean breakNaturally(boolean triggerEffect, boolean dropExperience) {
        return this.block.breakNaturally(triggerEffect, dropExperience);
    }

    public boolean breakNaturally(@NotNull ItemStack tool, boolean triggerEffect, boolean dropExperience) {
        return this.block.breakNaturally(tool, triggerEffect, dropExperience);
    }
}

