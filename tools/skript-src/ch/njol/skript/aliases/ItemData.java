/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.ItemFactory
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.MusicInstrumentMeta
 *  org.bukkit.inventory.meta.PotionMeta
 *  org.bukkit.potion.PotionEffect
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.aliases;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.aliases.MatchQuality;
import ch.njol.skript.bukkitutil.BukkitUnsafe;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.bukkitutil.block.BlockCompat;
import ch.njol.skript.bukkitutil.block.BlockValues;
import ch.njol.skript.localization.Message;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

public class ItemData
implements Cloneable,
YggdrasilSerializable.YggdrasilExtendedSerializable {
    static final ItemFactory itemFactory;
    private static final Message m_named;
    @Deprecated(since="2.7.0", forRemoval=true)
    public static final boolean itemDataValues = false;
    transient Material type;
    boolean isAnything;
    @Nullable
    transient ItemStack stack;
    @Nullable
    BlockValues blockValues;
    boolean itemForm;
    boolean isAlias = false;
    private boolean plain = false;
    int itemFlags;
    private static final Material[] materials;
    private static final boolean HAS_CUSTOM_NAME;

    public ItemData(Material type, @Nullable String tags) {
        this.type = type;
        if (type.isItem()) {
            this.stack = new ItemStack(type);
        }
        this.blockValues = BlockCompat.INSTANCE.getBlockValues(type);
        if (tags != null) {
            this.applyTags(tags);
        }
    }

    public ItemData(Material type, int amount) {
        this.type = type;
        if (type.isItem()) {
            this.stack = new ItemStack(type, Math.abs(amount));
        }
        this.blockValues = BlockCompat.INSTANCE.getBlockValues(type);
    }

    public ItemData(Material type) {
        this(type, 1);
    }

    public ItemData(ItemData data) {
        this.stack = data.stack != null ? data.stack.clone() : null;
        this.type = data.type;
        this.blockValues = data.blockValues;
        this.isAlias = data.isAlias;
        this.plain = data.plain;
        this.itemFlags = data.itemFlags;
    }

    public ItemData(Material material, @Nullable BlockValues values) {
        this.type = material;
        this.blockValues = values;
    }

    public ItemData(ItemStack stack, @Nullable BlockValues values) {
        this.stack = stack;
        this.type = stack.getType();
        this.blockValues = values;
        if (this.type.getMaxDurability() != 0) {
            this.itemFlags |= 1;
        }
        this.itemFlags |= 2;
    }

    public ItemData(ItemStack stack) {
        this(stack, BlockCompat.INSTANCE.getBlockValues(stack));
        this.itemForm = true;
    }

    @Deprecated(since="2.8.4", forRemoval=true)
    public ItemData(BlockState blockState) {
        this(blockState.getBlockData());
    }

    public ItemData(BlockData blockData) {
        this.type = blockData.getMaterial();
        if (this.type.isItem()) {
            this.stack = new ItemStack(this.type);
        }
        this.blockValues = BlockCompat.INSTANCE.getBlockValues(blockData);
    }

    public ItemData(Block block) {
        this(block.getBlockData());
    }

    public ItemData() {
    }

    public boolean isOfType(@Nullable ItemStack item) {
        if (item == null) {
            return this.type == Material.AIR;
        }
        if (this.type != item.getType()) {
            return false;
        }
        if (this.stack != null && this.itemFlags != 0) {
            if (ItemUtils.getDamage(this.stack) != ItemUtils.getDamage(item)) {
                return false;
            }
            if (this.stack.hasItemMeta() == item.hasItemMeta()) {
                return !this.stack.hasItemMeta() || itemFactory.equals(this.stack.getItemMeta(), item.getItemMeta());
            }
            return false;
        }
        return true;
    }

    public String toString() {
        return this.toString(false, false);
    }

    public String toString(boolean debug, boolean plural) {
        ItemMeta meta;
        StringBuilder builder = new StringBuilder(Aliases.getMaterialName(this, plural));
        ItemMeta itemMeta = meta = this.stack != null ? this.stack.getItemMeta() : null;
        if (meta != null && meta.hasDisplayName()) {
            builder.append(" ").append(m_named).append(" ");
            builder.append(meta.getDisplayName());
        }
        return builder.toString();
    }

    public int getGender() {
        return Aliases.getGender(this);
    }

    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ItemData)) {
            return false;
        }
        ItemData other = (ItemData)obj;
        if (this.isAlias()) {
            return other.matchAlias(this).isAtLeast(MatchQuality.SAME_ITEM);
        }
        return this.matchAlias(other).isAtLeast(MatchQuality.SAME_ITEM);
    }

    public int hashCode() {
        int hash = this.type.hashCode();
        if (this.blockValues == null || this.blockValues.isDefault()) {
            hash = hash * 37 + 1;
        }
        return hash;
    }

    public MatchQuality matchAlias(ItemData item) {
        if (this.isAnything || item.isAnything) {
            return MatchQuality.EXACT;
        }
        if (item.getType() != this.getType()) {
            return MatchQuality.DIFFERENT;
        }
        BlockValues values = this.blockValues;
        if (this.itemForm && item.blockValues != null && !item.blockValues.isDefault()) {
            return MatchQuality.SAME_MATERIAL;
        }
        MatchQuality quality = MatchQuality.EXACT;
        if (values != null) {
            quality = item.blockValues != null ? values.match(item.blockValues) : MatchQuality.SAME_MATERIAL;
        }
        if (this.itemForm && ItemUtils.getDamage(this.stack) != ItemUtils.getDamage(item.stack)) {
            quality = item.hasFlag(1) ? MatchQuality.SAME_MATERIAL : MatchQuality.SAME_ITEM;
        }
        if (quality.isAtLeast(MatchQuality.SAME_ITEM) && this.hasItemMeta() || item.hasItemMeta()) {
            MatchQuality metaQuality = ItemData.compareItemMetas(this.getItemMeta(), item.getItemMeta());
            if (metaQuality == MatchQuality.SAME_MATERIAL && !item.hasFlag(2)) {
                quality = MatchQuality.SAME_ITEM;
            } else if (quality.isBetter(metaQuality)) {
                quality = metaQuality;
            }
        }
        return quality;
    }

    private boolean hasFlag(int flag) {
        return (this.itemFlags & flag) != 0;
    }

    private static MatchQuality compareItemMetas(ItemMeta first, ItemMeta second) {
        Set theirFlags;
        Set ourFlags;
        Map theirEnchants;
        Map ourEnchants;
        List theirLore;
        MatchQuality newQuality;
        String theirName;
        MatchQuality quality = MatchQuality.EXACT;
        String ourName = first.hasDisplayName() ? first.getDisplayName() : null;
        String string = theirName = second.hasDisplayName() ? second.getDisplayName() : null;
        if (!Objects.equals(ourName, theirName)) {
            MatchQuality matchQuality = newQuality = ourName != null && theirName == null ? MatchQuality.SAME_ITEM : MatchQuality.SAME_MATERIAL;
            if (!newQuality.isBetter(quality)) {
                quality = newQuality;
            }
        }
        List ourLore = first.hasLore() ? first.getLore() : null;
        List list = theirLore = second.hasLore() ? second.getLore() : null;
        if (!Objects.equals(ourLore, theirLore)) {
            MatchQuality matchQuality = newQuality = ourLore != null && theirLore == null ? MatchQuality.SAME_ITEM : MatchQuality.SAME_MATERIAL;
            if (!newQuality.isBetter(quality)) {
                quality = newQuality;
            }
        }
        if (!Objects.equals(ourEnchants = first.getEnchants(), theirEnchants = second.getEnchants())) {
            MatchQuality matchQuality = newQuality = !ourEnchants.isEmpty() && theirEnchants.isEmpty() ? MatchQuality.SAME_ITEM : MatchQuality.SAME_MATERIAL;
            if (!newQuality.isBetter(quality)) {
                quality = newQuality;
            }
        }
        if (!Objects.equals(ourFlags = first.getItemFlags(), theirFlags = second.getItemFlags())) {
            MatchQuality matchQuality = newQuality = !ourFlags.isEmpty() && theirFlags.isEmpty() ? MatchQuality.SAME_ITEM : MatchQuality.SAME_MATERIAL;
            if (!newQuality.isBetter(quality)) {
                quality = newQuality;
            }
        }
        first = first.clone();
        second = second.clone();
        first.setDisplayName(null);
        second.setDisplayName(null);
        first.setLore(null);
        second.setLore(null);
        for (Enchantment ourEnchant : ourEnchants.keySet()) {
            first.removeEnchant(ourEnchant);
        }
        for (Enchantment theirEnchant : theirEnchants.keySet()) {
            second.removeEnchant(theirEnchant);
        }
        for (ItemFlag ourFlag : ourFlags) {
            first.removeItemFlags(new ItemFlag[]{ourFlag});
        }
        for (ItemFlag theirFlag : theirFlags) {
            second.removeItemFlags(new ItemFlag[]{theirFlag});
        }
        return first.equals((Object)second) ? quality : MatchQuality.SAME_MATERIAL;
    }

    public boolean isDefault() {
        return this.itemFlags == 0 && this.blockValues == null;
    }

    public boolean isAlias() {
        return this.isAlias || this.itemFlags == 0 && this.blockValues == null;
    }

    @Nullable
    public ItemData intersection(ItemData other) {
        if (other.type != this.type) {
            return null;
        }
        return this;
    }

    @Nullable
    public ItemStack getStack() {
        return this.stack;
    }

    private boolean hasItemMeta() {
        return this.stack != null && this.stack.hasItemMeta();
    }

    public ItemData clone() {
        return new ItemData(this);
    }

    public Material getType() {
        return this.type;
    }

    @Nullable
    public BlockValues getBlockValues() {
        return this.blockValues;
    }

    public ItemMeta getItemMeta() {
        ItemMeta meta;
        ItemMeta itemMeta = meta = this.stack != null ? this.stack.getItemMeta() : null;
        if (meta == null) {
            meta = itemFactory.getItemMeta(Material.STONE);
        }
        assert (meta != null);
        return meta;
    }

    public void setItemMeta(ItemMeta meta) {
        if (this.stack == null) {
            return;
        }
        this.stack.setItemMeta(meta);
        this.isAlias = false;
        this.plain = false;
        this.itemFlags |= 2;
    }

    public int getDurability() {
        if (this.stack == null) {
            return 0;
        }
        return ItemUtils.getDamage(this.stack);
    }

    public void setDurability(int durability) {
        if (this.stack == null) {
            return;
        }
        ItemUtils.setDamage(this.stack, durability);
        this.isAlias = false;
        this.plain = false;
        this.itemFlags |= 1;
    }

    public boolean isPlain() {
        return this.plain;
    }

    public void setPlain(boolean plain) {
        this.plain = plain;
    }

    public boolean matchPlain(ItemData other) {
        return this.getType() == other.getType() && (this.isPlain() && other.isPlain() || this.isPlain() && other.isAlias() || this.isAlias() && other.isPlain());
    }

    @Override
    public Fields serialize() throws NotSerializableException {
        Fields fields = new Fields(this);
        fields.putObject("key", this.type.getKey().toString());
        fields.putObject("meta", this.stack != null ? this.stack.getItemMeta() : null);
        return fields;
    }

    @Override
    public void deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
        if (fields.hasField("key")) {
            String key = fields.getAndRemoveObject("key", String.class);
            if (key == null) {
                throw new StreamCorruptedException("Material key is null");
            }
            this.type = Material.matchMaterial((String)key);
        } else {
            this.type = materials[fields.getAndRemovePrimitive("id", Integer.TYPE)];
        }
        ItemMeta meta = fields.getAndRemoveObject("meta", ItemMeta.class);
        if (meta != null && this.type.isItem()) {
            this.stack = new ItemStack(this.type);
            this.stack.setItemMeta(meta);
        }
        fields.setFields(this);
    }

    @Deprecated(since="INSERT_VERSION", forRemoval=true)
    public ItemData aliasCopy() {
        ItemData data = new ItemData();
        if (this.stack != null) {
            data.stack = new ItemStack(this.type, 1);
            if (this.stack.hasItemMeta()) {
                ItemMeta meta = this.stack.getItemMeta();
                meta.setDisplayName(null);
                if (!itemFactory.getItemMeta(this.type).equals((Object)meta)) {
                    data.itemFlags |= 2;
                }
                data.stack.setItemMeta(meta);
            }
            if (ItemUtils.getDamage(this.stack) > 0) {
                ItemUtils.setDamage(data.stack, 0);
            }
        }
        data.type = this.type;
        data.blockValues = this.blockValues;
        data.itemForm = this.itemForm;
        return data;
    }

    @Nullable
    public ItemData getBaseCopy() {
        ItemData data = new ItemData();
        data.type = this.type;
        data.blockValues = this.blockValues;
        data.itemForm = this.itemForm;
        if (this.stack != null) {
            data.stack = new ItemStack(this.type, 1);
            if (this.stack.hasItemMeta()) {
                ItemMeta meta = this.stack.getItemMeta();
                if (meta instanceof PotionMeta) {
                    PotionMeta potionMeta = (PotionMeta)meta;
                    ItemData.copyPotionInfo(potionMeta, data);
                } else if (meta instanceof MusicInstrumentMeta) {
                    MusicInstrumentMeta musicMeta = (MusicInstrumentMeta)meta;
                    ItemData.copyMusicInfo(musicMeta, data);
                }
            }
        }
        return data;
    }

    private static void copyPotionInfo(PotionMeta potionMeta, ItemData data) {
        PotionMeta newMeta = (PotionMeta)itemFactory.getItemMeta(data.type);
        if (newMeta.equals((Object)potionMeta)) {
            return;
        }
        if (potionMeta.hasBasePotionType()) {
            newMeta.setBasePotionType(potionMeta.getBasePotionType());
        }
        if (potionMeta.hasCustomEffects()) {
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                newMeta.addCustomEffect(effect, false);
            }
        }
        if (potionMeta.hasColor()) {
            newMeta.setColor(potionMeta.getColor());
        }
        if (HAS_CUSTOM_NAME && potionMeta.hasCustomPotionName()) {
            newMeta.setCustomPotionName(potionMeta.getCustomPotionName());
        }
        data.itemFlags = 2;
        data.setItemMeta((ItemMeta)newMeta);
    }

    private static void copyMusicInfo(MusicInstrumentMeta musicMeta, ItemData data) {
        MusicInstrumentMeta newMeta = (MusicInstrumentMeta)itemFactory.getItemMeta(data.type);
        if (newMeta.equals((Object)musicMeta)) {
            return;
        }
        newMeta.setInstrument(musicMeta.getInstrument());
        data.itemFlags = 2;
        data.setItemMeta((ItemMeta)newMeta);
    }

    public void applyTags(String tags) {
        if (this.stack == null) {
            return;
        }
        BukkitUnsafe.modifyItemStack(this.stack, tags);
        this.itemFlags |= 2;
    }

    static {
        Variables.yggdrasil.registerSingleClass(ItemData.class, "NewItemData");
        Variables.yggdrasil.registerSingleClass(OldItemData.class, "ItemData");
        itemFactory = Bukkit.getServer().getItemFactory();
        Path materialsFile = Paths.get(Skript.getInstance().getDataFolder().getAbsolutePath(), "materials.json");
        if (Files.exists(materialsFile, new LinkOption[0])) {
            try {
                Files.delete(materialsFile);
            }
            catch (IOException e) {
                Skript.exception((Throwable)e, "Failed to remove legacy material registry file!");
            }
        }
        m_named = new Message("aliases.named");
        materials = Material.values();
        HAS_CUSTOM_NAME = Skript.methodExists(PotionMeta.class, "hasCustomPotionName", new Class[0]);
    }

    public static class OldItemData {
        int typeid = -1;
        public short dataMin = (short)-1;
        public short dataMax = (short)-1;
    }
}

