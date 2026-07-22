/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.profile.PlayerProfile
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  io.papermc.paper.command.brigadier.argument.ArgumentTypes
 *  org.bukkit.Bukkit
 *  org.bukkit.Keyed
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Tag
 *  org.bukkit.TreeType
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.Fence
 *  org.bukkit.block.data.type.Gate
 *  org.bukkit.block.data.type.Wall
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.slot.Slot;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Wall;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemUtils {
    public static final boolean HAS_MAX_DAMAGE = Skript.methodExists(Damageable.class, "getMaxDamage", new Class[0]);
    public static final boolean HAS_RESET = Skript.methodExists(Damageable.class, "resetDamage", new Class[0]);
    public static final boolean CAN_CREATE_PLAYER_PROFILE = Skript.methodExists(Bukkit.class, "createPlayerProfile", UUID.class, String.class);
    public static final boolean REQUIRES_TEXTURE_LOOKUP = Skript.classExists("com.destroystokyo.paper.profile.PlayerProfile") && Skript.isRunningMinecraft(1, 19, 4);
    private static final boolean IS_AIR_EXISTS = Skript.methodExists(Material.class, "isAir", new Class[0]);
    private static final HashMap<TreeType, Material> TREE_TO_SAPLING_MAP = new HashMap();
    private static final boolean HAS_FENCE_TAGS;

    public static void applyComponentData(ItemStack itemStack, String data) {
        data = String.valueOf(itemStack.getType().getKey()) + (String)data;
        try {
            ItemStack parsed = (ItemStack)ArgumentTypes.itemStack().parse(new StringReader((String)data));
            itemStack.setItemMeta(parsed.getItemMeta());
        }
        catch (CommandSyntaxException e) {
            throw Skript.exception((Throwable)e, "Failed to apply component data to ItemStack");
        }
    }

    public static int getDamage(ItemStack itemStack) {
        return ItemUtils.getDamage(itemStack.getItemMeta());
    }

    public static int getDamage(ItemMeta itemMeta) {
        if (itemMeta instanceof Damageable) {
            return ((Damageable)itemMeta).getDamage();
        }
        return 0;
    }

    public static int getMaxDamage(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (HAS_MAX_DAMAGE && meta instanceof Damageable && ((Damageable)meta).hasMaxDamage()) {
            return ((Damageable)meta).getMaxDamage();
        }
        return itemStack.getType().getMaxDurability();
    }

    public static void setMaxDamage(ItemStack itemStack, int maxDamage) {
        ItemMeta meta = itemStack.getItemMeta();
        if (HAS_MAX_DAMAGE && meta instanceof Damageable) {
            Damageable damageable = (Damageable)meta;
            if (HAS_RESET && maxDamage < 1) {
                damageable.resetDamage();
            } else {
                damageable.setMaxDamage(Integer.valueOf(Math.max(1, maxDamage)));
            }
            itemStack.setItemMeta((ItemMeta)damageable);
        }
    }

    public static void setDamage(ItemStack itemStack, int damage) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof Damageable) {
            ((Damageable)meta).setDamage(Math.max(0, damage));
            itemStack.setItemMeta(meta);
        }
    }

    public static int getDamage(ItemType itemType) {
        ItemMeta meta = itemType.getItemMeta();
        if (meta instanceof Damageable) {
            return ((Damageable)meta).getDamage();
        }
        return 0;
    }

    public static int getMaxDamage(ItemType itemType) {
        ItemMeta meta = itemType.getItemMeta();
        if (HAS_MAX_DAMAGE && meta instanceof Damageable && ((Damageable)meta).hasMaxDamage()) {
            return ((Damageable)meta).getMaxDamage();
        }
        return itemType.getMaterial().getMaxDurability();
    }

    public static void setDamage(ItemType itemType, int damage) {
        ItemMeta meta = itemType.getItemMeta();
        if (meta instanceof Damageable) {
            ((Damageable)meta).setDamage(damage);
            itemType.setItemMeta(meta);
        }
    }

    public static void setHeadOwner(ItemType skull, OfflinePlayer player) {
        ItemMeta meta = skull.getItemMeta();
        if (!(meta instanceof SkullMeta)) {
            return;
        }
        SkullMeta skullMeta = (SkullMeta)meta;
        if (REQUIRES_TEXTURE_LOOKUP) {
            PlayerProfile profile = player.getPlayerProfile();
            if (!profile.hasTextures()) {
                profile.complete(true);
            }
            skullMeta.setPlayerProfile(profile);
        } else if (player.getName() != null) {
            skullMeta.setOwningPlayer(player);
        } else if (CAN_CREATE_PLAYER_PROFILE) {
            skullMeta.setOwnerProfile(Bukkit.createPlayerProfile((UUID)player.getUniqueId(), (String)""));
        } else {
            skullMeta.setOwningPlayer(null);
        }
        skull.setItemMeta((ItemMeta)skullMeta);
    }

    @Nullable
    public static Material asBlock(Material type) {
        if (type.isBlock()) {
            return type;
        }
        return null;
    }

    @Deprecated(since="2.8.4", forRemoval=true)
    public static Material asItem(Material type) {
        return type;
    }

    @Nullable
    public static ItemStack asItemStack(@Nullable Object object) {
        if (object instanceof ItemType) {
            ItemType itemType = (ItemType)object;
            return itemType.getRandom();
        }
        if (object instanceof Slot) {
            Slot slot = (Slot)object;
            return slot.getItem();
        }
        if (object instanceof ItemStack) {
            ItemStack itemStack = (ItemStack)object;
            return itemStack;
        }
        return null;
    }

    public static boolean itemStacksEqual(@Nullable ItemStack itemStack1, @Nullable ItemStack itemStack2) {
        if (itemStack1 == null || itemStack2 == null) {
            return itemStack1 == itemStack2;
        }
        if (itemStack1.getType() != itemStack2.getType()) {
            return false;
        }
        ItemMeta itemMeta1 = itemStack1.getItemMeta();
        ItemMeta itemMeta2 = itemStack2.getItemMeta();
        if (itemMeta1 == null || itemMeta2 == null) {
            return itemMeta1 == itemMeta2;
        }
        return itemStack1.getItemMeta().equals((Object)itemStack2.getItemMeta());
    }

    public static boolean isAir(Material type) {
        if (IS_AIR_EXISTS) {
            return type.isAir();
        }
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    public static Material getTreeSapling(TreeType treeType) {
        return TREE_TO_SAPLING_MAP.get(treeType);
    }

    public static boolean isFence(Block block) {
        if (!HAS_FENCE_TAGS) {
            BlockData data = block.getBlockData();
            return data instanceof Fence || data instanceof Wall || data instanceof Gate;
        }
        Material type = block.getType();
        return Tag.FENCES.isTagged((Keyed)type) || Tag.FENCE_GATES.isTagged((Keyed)type) || Tag.WALLS.isTagged((Keyed)type);
    }

    public static boolean isGlass(Material material) {
        switch (material) {
            case GLASS: 
            case RED_STAINED_GLASS: 
            case ORANGE_STAINED_GLASS: 
            case YELLOW_STAINED_GLASS: 
            case LIGHT_BLUE_STAINED_GLASS: 
            case BLUE_STAINED_GLASS: 
            case CYAN_STAINED_GLASS: 
            case LIME_STAINED_GLASS: 
            case GREEN_STAINED_GLASS: 
            case MAGENTA_STAINED_GLASS: 
            case PURPLE_STAINED_GLASS: 
            case PINK_STAINED_GLASS: 
            case WHITE_STAINED_GLASS: 
            case LIGHT_GRAY_STAINED_GLASS: 
            case GRAY_STAINED_GLASS: 
            case BLACK_STAINED_GLASS: 
            case BROWN_STAINED_GLASS: {
                return true;
            }
        }
        return false;
    }

    public static <T extends ItemMeta> ItemStack changeItemMeta(@NotNull Class<T> metaClass, @NotNull ItemStack itemStack, @NotNull Consumer<T> metaChanger) {
        ItemMeta originalMeta = itemStack.getItemMeta();
        if (metaClass.isInstance(originalMeta)) {
            ItemMeta itemMeta = originalMeta;
            metaChanger.accept(itemMeta);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public static void setItemMeta(Object object, @NotNull ItemMeta itemMeta) {
        if (object instanceof Slot) {
            Slot slot = (Slot)object;
            ItemStack itemStack = slot.getItem();
            if (itemStack == null) {
                return;
            }
            itemStack.setItemMeta(itemMeta);
            slot.setItem(itemStack);
        } else if (object instanceof ItemType) {
            ItemType itemType = (ItemType)object;
            itemType.setItemMeta(itemMeta);
        } else if (object instanceof ItemStack) {
            ItemStack itemStack = (ItemStack)object;
            itemStack.setItemMeta(itemMeta);
        } else {
            throw new IllegalArgumentException("Object was not a Slot, ItemType or ItemStack.");
        }
    }

    static {
        TREE_TO_SAPLING_MAP.put(TreeType.TREE, Material.OAK_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.BIG_TREE, Material.OAK_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.SWAMP, Material.OAK_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.REDWOOD, Material.SPRUCE_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.TALL_REDWOOD, Material.SPRUCE_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.MEGA_REDWOOD, Material.SPRUCE_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.BIRCH, Material.BIRCH_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.TALL_BIRCH, Material.BIRCH_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.JUNGLE, Material.JUNGLE_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.SMALL_JUNGLE, Material.JUNGLE_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.JUNGLE_BUSH, Material.JUNGLE_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.COCOA_TREE, Material.JUNGLE_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.ACACIA, Material.ACACIA_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.DARK_OAK, Material.DARK_OAK_SAPLING);
        TREE_TO_SAPLING_MAP.put(TreeType.BROWN_MUSHROOM, Material.BROWN_MUSHROOM);
        TREE_TO_SAPLING_MAP.put(TreeType.RED_MUSHROOM, Material.RED_MUSHROOM);
        TREE_TO_SAPLING_MAP.put(TreeType.CHORUS_PLANT, Material.CHORUS_FLOWER);
        if (Skript.isRunningMinecraft(1, 16)) {
            TREE_TO_SAPLING_MAP.put(TreeType.WARPED_FUNGUS, Material.WARPED_FUNGUS);
            TREE_TO_SAPLING_MAP.put(TreeType.CRIMSON_FUNGUS, Material.CRIMSON_FUNGUS);
        }
        if (Skript.isRunningMinecraft(1, 17)) {
            TREE_TO_SAPLING_MAP.put(TreeType.AZALEA, Material.AZALEA);
        }
        if (Skript.isRunningMinecraft(1, 19)) {
            TREE_TO_SAPLING_MAP.put(TreeType.MANGROVE, Material.MANGROVE_PROPAGULE);
            TREE_TO_SAPLING_MAP.put(TreeType.TALL_MANGROVE, Material.MANGROVE_PROPAGULE);
        }
        if (Skript.isRunningMinecraft(1, 19, 4)) {
            TREE_TO_SAPLING_MAP.put(TreeType.CHERRY, Material.CHERRY_SAPLING);
        }
        if (Skript.isRunningMinecraft(1, 20, 5)) {
            TREE_TO_SAPLING_MAP.put(TreeType.MEGA_PINE, Material.SPRUCE_SAPLING);
        }
        if (Skript.isRunningMinecraft(1, 21, 3)) {
            TREE_TO_SAPLING_MAP.put(TreeType.PALE_OAK, Material.PALE_OAK_SAPLING);
            TREE_TO_SAPLING_MAP.put(TreeType.PALE_OAK_CREAKING, Material.PALE_OAK_SAPLING);
        }
        HAS_FENCE_TAGS = !Skript.isRunningMinecraft(1, 14);
    }
}

