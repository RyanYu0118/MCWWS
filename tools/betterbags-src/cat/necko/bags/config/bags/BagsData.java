/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.profile.PlayerProfile
 *  com.destroystokyo.paper.profile.ProfileProperty
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.persistence.PersistentDataType
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package cat.necko.bags.config.bags;

import cat.necko.bags.Plugin;
import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.config.bags.BagsFile;
import cat.necko.bags.config.bags.data.BagLevel;
import cat.necko.bags.utils.StringUtil;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BagsData {
    private final BagLevel bagLevels;
    public static ItemHash BAG;
    public static ItemHash UPGRADE;
    public static ItemHash SELL_ALL;
    public static ItemHash NEXT_PAGE;
    public static ItemHash PREV_PAGE;
    public static ItemHash IIV_TRUE;
    public static ItemHash IIV_FALSE;
    public static ItemHash FILLER;

    public BagLevel getBagLevels() {
        return this.bagLevels;
    }

    public BagsData(BagsFile bagsFile) {
        FileConfiguration config = bagsFile.getConfig();
        ConfigurationSection items = config.getConfigurationSection("items");
        assert (items != null);
        BAG = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("bag")));
        UPGRADE = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("upgrade")));
        SELL_ALL = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("sell-all")));
        NEXT_PAGE = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("next-page")));
        PREV_PAGE = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("previous-page")));
        FILLER = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("filler")));
        IIV_TRUE = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("ignore-item-value-true")));
        IIV_FALSE = new ItemHash(Objects.requireNonNull(items.getConfigurationSection("ignore-item-value-false")));
        this.bagLevels = new BagLevel(Objects.requireNonNull(config.getConfigurationSection("levels")));
    }

    @NotNull
    public static ItemStack getItemFor(ItemHash itemHash, PlayerData playerData) {
        ItemStack newItem = itemHash.item.clone();
        newItem.editMeta(itemMeta -> {
            if (itemHash.name != null) {
                itemMeta.displayName(StringUtil.prepareFor(playerData, itemHash.name));
            }
            ArrayList<Component> lore = new ArrayList<Component>();
            for (String line : itemHash.lore) {
                lore.add(StringUtil.prepareFor(playerData, line));
            }
            itemMeta.lore(lore);
        });
        ItemStack itemStack = newItem;
        if (itemStack == null) {
            BagsData.$$$reportNull$$$0(0);
        }
        return itemStack;
    }

    public static void giveBagToPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        int bagSlot = Plugin.getInstance().getConfigData().bagSlot;
        for (ItemStack item : inv.getContents()) {
            if (item == null || !item.getPersistentDataContainer().has(ItemHash.KEY, PersistentDataType.STRING)) continue;
            item.setAmount(0);
        }
        ItemStack itemBefore = inv.getItem(bagSlot);
        inv.setItem(bagSlot, BagsData.getItemFor(BAG, Plugin.getInstance().getPlayerData(player.getUniqueId())));
        if (itemBefore != null) {
            inv.addItem(new ItemStack[]{itemBefore});
        }
    }

    public static void updatePlayerBag(Player player) {
        PlayerInventory inv = player.getInventory();
        int bagSlot = -1;
        for (int slot = 0; slot < inv.getSize(); ++slot) {
            ItemStack item = inv.getItem(slot);
            if (item == null || !BAG.compareTags(item)) continue;
            bagSlot = slot;
            break;
        }
        if (bagSlot == -1) {
            BagsData.giveBagToPlayer(player);
            return;
        }
        inv.setItem(bagSlot, BagsData.getItemFor(BAG, Plugin.getInstance().getPlayerData(player.getUniqueId())));
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", "cat/necko/bags/config/bags/BagsData", "getItemFor"));
    }

    public static class ItemHash {
        public static final NamespacedKey KEY = new NamespacedKey("betterbags", "item");
        public final String section;
        public final ItemStack item;
        public final List<String> lore;
        public final String name;

        public ItemHash(@NotNull ConfigurationSection itemSection) {
            String texture;
            if (itemSection == null) {
                ItemHash.$$$reportNull$$$0(0);
            }
            this.section = itemSection.getName();
            Material material = Material.valueOf((String)itemSection.getString("material"));
            this.item = new ItemStack(material);
            if (material == Material.PLAYER_HEAD && (texture = itemSection.getString("head-texture")) != null) {
                this.item.editMeta(SkullMeta.class, itemMeta -> {
                    PlayerProfile profile = Bukkit.createProfile((UUID)UUID.fromString("00000000-0000-0000-0000-000000000000"), (String)"");
                    profile.setProperty(new ProfileProperty("textures", texture));
                    itemMeta.setPlayerProfile(profile);
                });
            }
            this.item.editMeta(itemMeta -> {
                itemMeta.setCustomModelData(Integer.valueOf(itemSection.getInt("custom-model-data", 0)));
                itemMeta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, (Object)this.section);
                if (itemSection.getBoolean("glowing", false)) {
                    itemMeta.addEnchant(Enchantment.BINDING_CURSE, 4, true);
                    itemMeta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                }
            });
            this.name = itemSection.getString("name");
            this.lore = itemSection.getStringList("description");
        }

        public boolean compareTags(@Nullable ItemStack item) {
            return item != null && Objects.equals(item.getPersistentDataContainer().get(KEY, PersistentDataType.STRING), this.section);
        }

        private static /* synthetic */ void $$$reportNull$$$0(int n) {
            throw new IllegalArgumentException(String.format("Argument for @NotNull parameter '%s' of %s.%s must not be null", "itemSection", "cat/necko/bags/config/bags/BagsData$ItemHash", "<init>"));
        }
    }
}

