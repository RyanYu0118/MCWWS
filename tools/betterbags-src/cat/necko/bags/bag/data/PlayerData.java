/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.permissions.PermissionAttachmentInfo
 *  org.bukkit.persistence.PersistentDataType
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package cat.necko.bags.bag.data;

import cat.necko.bags.Plugin;
import cat.necko.bags.config.bags.BagsData;
import cat.necko.bags.config.bags.data.BagLevel;
import cat.necko.bags.utils.StringUtil;
import cat.necko.bags.utils.Tuple;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerData {
    private List<Inventory> items = new ArrayList<Inventory>();
    private float multiplier;
    private boolean ignoreItemValue;
    private int itemsAmount;
    private final UUID uuid;
    private BagLevel.Level bagLevel;

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    public void setIgnoreItemValue(boolean ignoreItemValue) {
        this.ignoreItemValue = ignoreItemValue;
    }

    public PlayerData(UUID uuid) {
        this(Plugin.getInstance(), uuid, 1);
    }

    public PlayerData(Plugin plugin, UUID uuid, int bagLevel) {
        this.uuid = uuid;
        this.itemsAmount = 0;
        this.multiplier = 1.0f;
        this.ignoreItemValue = false;
        if (!this.load(plugin)) {
            this.setBagLevel(bagLevel);
        }
    }

    @Nullable
    public Inventory getInventoryPage(int page) {
        this.update();
        Inventory currentPage = this.items.get(Math.max(0, Math.min(page, this.items.size() - 1)));
        Inventory newInv = Bukkit.createInventory((InventoryHolder)currentPage.getHolder(), (int)currentPage.getSize(), (Component)StringUtil.prepareFor(this, Plugin.getInstance().getConfigData().bagInventoryLabel));
        newInv.setContents(currentPage.getContents());
        return newInv;
    }

    public boolean upgradeBag() {
        Player player = Bukkit.getPlayer((UUID)this.uuid);
        if (player == null) {
            return false;
        }
        boolean hasMoney = Plugin.getEconomy().has((OfflinePlayer)player, (double)this.getNextLevel().cost());
        if (!hasMoney) {
            return false;
        }
        this.setBagLevel(this.bagLevel.level() + 1);
        Plugin.getEconomy().withdrawPlayer((OfflinePlayer)player, (double)this.bagLevel.cost());
        for (String command : this.bagLevel.commands()) {
            Bukkit.getServer().dispatchCommand((CommandSender)Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
        return true;
    }

    public Tuple<Float, Integer> sellAndDeposit() {
        return this.sellAndDeposit(this.ignoreItemValue);
    }

    public Tuple<Float, Integer> sellAndDeposit(boolean ignoreItemValue) {
        Tuple<Float, Integer> result = this.sellAll(ignoreItemValue);
        float toGive = Math.max(0.0f, result.a().floatValue());
        if (toGive > 0.0f) {
            Player player = Bukkit.getPlayer((UUID)this.uuid);
            Plugin.getEconomy().depositPlayer((OfflinePlayer)player, (double)(toGive *= (float)PlayerData.getMultiplierFromPermissions(player)));
        }
        return Tuple.of(Float.valueOf(toGive), result.b());
    }

    public static int getMultiplierFromPermissions(@Nullable Player player) {
        if (player == null) {
            return 1;
        }
        int multiplier = 1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String permission = info.getPermission();
            if (!permission.startsWith("betterbags.multiplier.") || !info.getValue()) continue;
            multiplier *= Integer.parseInt(permission.replace("betterbags.multiplier.", ""));
        }
        return Math.max(1, multiplier);
    }

    public Tuple<Float, Integer> sellAll() {
        return this.sellAll(this.ignoreItemValue);
    }

    public Tuple<Float, Integer> sellAll(boolean ignoreItemValue) {
        int soldAmount = 0;
        int totalCost = 0;
        for (Inventory inventory : this.items) {
            for (ItemStack item : inventory.getContents()) {
                if (item == null || item.getPersistentDataContainer().has(BagsData.ItemHash.KEY, PersistentDataType.STRING)) continue;
                Material material = item.getType();
                if (!Plugin.getInstance().getItemsData().isSellable(material) && !ignoreItemValue) continue;
                int amount = item.getAmount();
                int cost = Plugin.getInstance().getItemsData().getCost(material);
                item.setAmount(0);
                totalCost += cost * amount;
                soldAmount += amount;
            }
        }
        this.itemsAmount -= soldAmount;
        return Tuple.of(Float.valueOf((float)totalCost * this.multiplier), soldAmount);
    }

    public int addItem(@NotNull ItemStack item) {
        int freeAmount;
        int thisAmount;
        int toReturn;
        int add;
        if (item == null) {
            PlayerData.$$$reportNull$$$0(0);
        }
        int n = add = (toReturn = Math.max(0, (thisAmount = item.getAmount()) - (freeAmount = this.bagLevel.capacity() - this.itemsAmount))) > 0 ? freeAmount : thisAmount;
        if (this.itemsAmount >= this.bagLevel.capacity()) {
            return thisAmount;
        }
        if (item.getPersistentDataContainer().has(BagsData.ItemHash.KEY, PersistentDataType.STRING)) {
            return thisAmount;
        }
        item.setAmount(add);
        boolean added = false;
        for (Inventory inventory : this.items) {
            HashMap returned = inventory.addItem(new ItemStack[]{item});
            if (!returned.isEmpty()) continue;
            item.setAmount(add);
            added = true;
            break;
        }
        if (!added) {
            return thisAmount;
        }
        this.itemsAmount += add;
        return toReturn;
    }

    public boolean removeItem(@NotNull ItemStack item) {
        if (item == null) {
            PlayerData.$$$reportNull$$$0(1);
        }
        int thisAmount = item.getAmount();
        boolean removed = false;
        for (Inventory inventory : this.items) {
            HashMap returned = inventory.removeItem(new ItemStack[]{item});
            if (!returned.isEmpty()) continue;
            removed = true;
            break;
        }
        if (!removed) {
            return false;
        }
        this.itemsAmount -= thisAmount;
        return true;
    }

    public BagLevel.Level setBagLevel(int bagLevel) {
        Inventory inventory;
        BagLevel.Level level;
        this.bagLevel = level = Plugin.getInstance().getBagsData().getBagLevels().getLevel(bagLevel);
        ArrayList<Inventory> items = new ArrayList<Inventory>();
        Player player = Bukkit.getPlayer((UUID)this.uuid);
        int page = 0;
        while ((inventory = page < this.items.size() ? this.expandInventory(this.items.get(page), level.slots(), page) : this.createInventory((InventoryHolder)player, level.slots(), page)) != null) {
            items.add(inventory);
            ++page;
        }
        this.items = items;
        return level;
    }

    public void clearBag() {
        Inventory inventory;
        Player player = Bukkit.getPlayer((UUID)this.uuid);
        this.itemsAmount = 0;
        int page = 0;
        while ((inventory = this.createInventory((InventoryHolder)player, this.bagLevel.slots(), page)) != null) {
            this.items.set(page, inventory);
            ++page;
        }
    }

    private int getInventorySize(int slots) {
        int slots_9 = (int)Math.ceil((double)slots / 9.0);
        int max_min = Math.max(0, Math.min(5, slots_9));
        return max_min * 9;
    }

    @Nullable
    private Inventory expandInventory(@NotNull Inventory inventory, int slots, int page) {
        Inventory newInv;
        if (inventory == null) {
            PlayerData.$$$reportNull$$$0(2);
        }
        if ((newInv = this.createInventory(inventory.getHolder(), slots, page)) == null) {
            return null;
        }
        for (int i = 0; i < inventory.getSize() - 9; ++i) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getPersistentDataContainer().has(BagsData.ItemHash.KEY, PersistentDataType.STRING)) continue;
            newInv.setItem(i, item);
        }
        return newInv;
    }

    @Nullable
    private Inventory createInventory(@Nullable InventoryHolder owner, int slots, int page) {
        int availableSlots = slots - page * 45;
        if (availableSlots < 0) {
            return null;
        }
        int bagSize = this.getInventorySize(availableSlots);
        Inventory inv = Bukkit.createInventory((InventoryHolder)owner, (int)(bagSize + 9));
        ItemStack filler = BagsData.getItemFor(BagsData.FILLER, this);
        for (int i = 0; i < bagSize; ++i) {
            if (i < availableSlots) continue;
            inv.setItem(i, filler);
        }
        return this.setFunctionalItems(inv, page);
    }

    @Contract(value="!null, _ -> !null")
    @Nullable
    public Inventory setFunctionalItems(@Nullable Inventory inv, int page) {
        if (inv == null) {
            return null;
        }
        int availableSlots = this.bagLevel.slots() - page * 45;
        if (availableSlots < 0) {
            return null;
        }
        int bagSize = inv.getSize() - 9;
        ItemStack filler = BagsData.getItemFor(BagsData.FILLER, this);
        for (int i = bagSize; i < bagSize + 9; ++i) {
            inv.setItem(i, filler);
        }
        if (page > 0) {
            inv.setItem(bagSize, BagsData.getItemFor(BagsData.PREV_PAGE, this));
        }
        if (availableSlots > 45) {
            inv.setItem(bagSize + 8, BagsData.getItemFor(BagsData.NEXT_PAGE, this));
        }
        if (Plugin.getInstance().getBagsData().getBagLevels().getMaxLevel() != this.bagLevel.level()) {
            inv.setItem(bagSize + 4, BagsData.getItemFor(BagsData.UPGRADE, this));
        }
        if (Plugin.getInstance().getConfigData().sellAll) {
            BagsData.ItemHash iiv = this.ignoreItemValue ? BagsData.IIV_TRUE : BagsData.IIV_FALSE;
            inv.setItem(bagSize + 3, BagsData.getItemFor(BagsData.SELL_ALL, this));
            inv.setItem(bagSize + 5, BagsData.getItemFor(iiv, this));
        }
        return inv;
    }

    public void update() {
        for (int page = 0; page < this.items.size(); ++page) {
            Inventory inventory = this.items.get(page);
            this.setFunctionalItems(inventory, page);
        }
    }

    public void save(Plugin plugin) throws IOException {
        String fileName = "data/%s.yml".formatted(this.uuid);
        File tempFile = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration tempYaml = YamlConfiguration.loadConfiguration((File)tempFile);
        ArrayList content = new ArrayList();
        this.getItems().forEach(inventory -> {
            for (ItemStack item : inventory.getContents()) {
                if (item == null || item.getPersistentDataContainer().has(BagsData.ItemHash.KEY, PersistentDataType.STRING)) continue;
                content.add(item.serialize());
            }
        });
        Player player = plugin.getServer().getPlayer(this.uuid);
        if (player != null) {
            tempYaml.set("last-known-name", (Object)player.getName());
        }
        tempYaml.set("level", (Object)this.bagLevel.level());
        tempYaml.set("items", content);
        tempYaml.set("multiplier", (Object)Float.valueOf(this.multiplier));
        tempYaml.set("ignore-item-value", (Object)this.ignoreItemValue);
        tempYaml.save(tempFile);
    }

    public boolean load(Plugin plugin) {
        String fileName = "data/%s.yml".formatted(this.uuid);
        File tempFile = new File(plugin.getDataFolder(), fileName);
        if (!tempFile.exists()) {
            return false;
        }
        YamlConfiguration tempYaml = YamlConfiguration.loadConfiguration((File)tempFile);
        List items = tempYaml.getList("items");
        if (items == null) {
            return false;
        }
        this.bagLevel = this.setBagLevel(tempYaml.getInt("level", 1));
        this.multiplier = tempYaml.getInt("multiplier", 1);
        this.ignoreItemValue = tempYaml.getBoolean("ignore-item-value", false);
        items.forEach(item -> {
            ItemStack itemStack = ItemStack.deserialize((Map)((Map)item));
            this.addItem(itemStack);
        });
        this.update();
        return true;
    }

    private List<Inventory> getItems() {
        return this.items;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public BagLevel.Level getLevel() {
        return this.bagLevel;
    }

    public BagLevel.Level getNextLevel() {
        return Plugin.getInstance().getBagsData().getBagLevels().getLevel(this.bagLevel.level() + 1);
    }

    public int getItemsAmount() {
        return this.itemsAmount;
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        Object[] objectArray;
        Object[] objectArray2;
        Object[] objectArray3 = new Object[3];
        switch (n) {
            default: {
                objectArray2 = objectArray3;
                objectArray3[0] = "item";
                break;
            }
            case 2: {
                objectArray2 = objectArray3;
                objectArray3[0] = "inventory";
                break;
            }
        }
        objectArray2[1] = "cat/necko/bags/bag/data/PlayerData";
        switch (n) {
            default: {
                objectArray = objectArray2;
                objectArray2[2] = "addItem";
                break;
            }
            case 1: {
                objectArray = objectArray2;
                objectArray2[2] = "removeItem";
                break;
            }
            case 2: {
                objectArray = objectArray2;
                objectArray2[2] = "expandInventory";
                break;
            }
        }
        throw new IllegalArgumentException(String.format("Argument for @NotNull parameter '%s' of %s.%s must not be null", objectArray));
    }
}

