/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.Tag
 *  org.bukkit.entity.AbstractHorse
 *  org.bukkit.entity.ChestedHorse
 *  org.bukkit.entity.Horse
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Llama
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Steerable
 *  org.bukkit.entity.Wolf
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.HorseInventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.LlamaInventory
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.PlayerUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Steerable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LlamaInventory;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Equip")
@Description(value={"Equips or unequips an entity with the given itemtypes (usually armor).", "This effect will replace any armor that the entity is already wearing."})
@Example.Examples(value={@Example(value="equip player with diamond helmet"), @Example(value="equip player with diamond leggings, diamond chestplate, and diamond boots"), @Example(value="unequip diamond chestplate from player"), @Example(value="unequip player's armor")})
@Since(value={"1.0, 2.7 (multiple entities, unequip), 2.10 (wolves)", "2.12.1 (happy ghasts)"})
public class EffEquip
extends Effect {
    private static final ItemType CHESTPLATE;
    private static final ItemType LEGGINGS;
    private static final ItemType BOOTS;
    private static final ItemType CARPET;
    private static final ItemType WOLF_ARMOR;
    private static final ItemType HORSE_ARMOR;
    private static final ItemType SADDLE;
    private static final ItemType CHEST;
    private static final ItemType HAPPY_GHAST_HARNESS;
    private static final Class<?> HAPPY_GHAST_CLASS;
    private static final ItemType[] ALL_EQUIPMENT;
    private Expression<LivingEntity> entities;
    private @UnknownNullability Expression<ItemType> itemTypes;
    private boolean equip = true;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        if (matchedPattern == 0 || matchedPattern == 1) {
            this.entities = exprs[0];
            this.itemTypes = exprs[1];
        } else if (matchedPattern == 2) {
            this.itemTypes = exprs[0];
            this.entities = exprs[1];
            this.equip = false;
        } else if (matchedPattern == 3) {
            this.entities = exprs[0];
            this.equip = false;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        ItemType[] itemTypes;
        boolean unequipHelmet = false;
        if (this.itemTypes != null) {
            itemTypes = this.itemTypes.getArray(event);
        } else {
            itemTypes = ALL_EQUIPMENT;
            unequipHelmet = true;
        }
        for (LivingEntity entity : this.entities.getArray(event)) {
            EntityEquipment equipment;
            LlamaInventory inv;
            if (entity instanceof Steerable) {
                Steerable steerable = (Steerable)entity;
                for (ItemType itemType : itemTypes) {
                    if (!SADDLE.isOfType(itemType.getMaterial())) continue;
                    steerable.setSaddle(this.equip);
                }
                continue;
            }
            if (entity instanceof Llama) {
                Llama llama = (Llama)entity;
                inv = llama.getInventory();
                for (ItemType itemType : itemTypes) {
                    for (ItemStack item : itemType.getAll()) {
                        if (CARPET.isOfType(item)) {
                            inv.setDecor((ItemStack)(this.equip ? item : null));
                            continue;
                        }
                        if (!CHEST.isOfType(item)) continue;
                        llama.setCarryingChest(this.equip);
                    }
                }
                continue;
            }
            if (entity instanceof AbstractHorse) {
                AbstractHorse horse = (AbstractHorse)entity;
                inv = horse.getInventory();
                for (ItemType itemType : itemTypes) {
                    for (ItemStack item : itemType.getAll()) {
                        if (SADDLE.isOfType(item)) {
                            inv.setSaddle((ItemStack)(this.equip ? item : null));
                            continue;
                        }
                        if (HORSE_ARMOR.isOfType(item) && entity instanceof Horse) {
                            ((HorseInventory)inv).setArmor((ItemStack)(this.equip ? item : null));
                            continue;
                        }
                        if (!CHEST.isOfType(item) || !(entity instanceof ChestedHorse)) continue;
                        ChestedHorse chestedHorse = (ChestedHorse)entity;
                        chestedHorse.setCarryingChest(this.equip);
                    }
                }
                continue;
            }
            if (entity instanceof Wolf) {
                Wolf wolf = (Wolf)entity;
                equipment = wolf.getEquipment();
                for (ItemType itemType : itemTypes) {
                    for (ItemStack item : itemType.getAll()) {
                        if (!WOLF_ARMOR.isOfType(item)) continue;
                        equipment.setItem(EquipmentSlot.BODY, (ItemStack)(this.equip ? item : null));
                    }
                }
                continue;
            }
            if (HAPPY_GHAST_CLASS != null && HAPPY_GHAST_CLASS.isInstance(entity)) {
                equipment = ((Mob)entity).getEquipment();
                for (ItemType itemType : itemTypes) {
                    for (ItemStack itemStack : itemType.getAll()) {
                        if (!HAPPY_GHAST_HARNESS.isOfType(itemStack)) continue;
                        equipment.setItem(EquipmentSlot.BODY, (ItemStack)(this.equip ? itemStack : null));
                    }
                }
                continue;
            }
            equipment = entity.getEquipment();
            if (equipment == null) continue;
            for (ItemType itemType : itemTypes) {
                for (ItemStack item : itemType.getAll()) {
                    if (CHESTPLATE.isOfType(item)) {
                        equipment.setChestplate((ItemStack)(this.equip ? item : null));
                        continue;
                    }
                    if (LEGGINGS.isOfType(item)) {
                        equipment.setLeggings((ItemStack)(this.equip ? item : null));
                        continue;
                    }
                    if (BOOTS.isOfType(item)) {
                        equipment.setBoots((ItemStack)(this.equip ? item : null));
                        continue;
                    }
                    equipment.setHelmet((ItemStack)(this.equip ? item : null));
                }
                if (!unequipHelmet) continue;
                equipment.setHelmet(null);
            }
            if (!(entity instanceof Player)) continue;
            Player player = (Player)entity;
            PlayerUtils.updateInventory(player);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.equip) {
            assert (this.itemTypes != null);
            return "equip " + this.entities.toString(event, debug) + " with " + this.itemTypes.toString(event, debug);
        }
        if (this.itemTypes != null) {
            return "unequip " + this.itemTypes.toString(event, debug) + " from " + this.entities.toString(event, debug);
        }
        return "unequip " + this.entities.toString(event, debug) + "'s equipment";
    }

    static {
        CARPET = new ItemType((Tag<Material>)Tag.WOOL_CARPETS);
        HORSE_ARMOR = new ItemType(Material.LEATHER_HORSE_ARMOR, Material.IRON_HORSE_ARMOR, Material.GOLDEN_HORSE_ARMOR, Material.DIAMOND_HORSE_ARMOR);
        SADDLE = new ItemType(Material.SADDLE);
        CHEST = new ItemType(Material.CHEST);
        WOLF_ARMOR = Skript.fieldExists(Material.class, "WOLF_ARMOR") ? new ItemType(Material.WOLF_ARMOR) : new ItemType();
        if (Skript.fieldExists(Tag.class, "ITEM_CHEST_ARMOR")) {
            CHESTPLATE = new ItemType((Tag<Material>)Tag.ITEMS_CHEST_ARMOR);
            LEGGINGS = new ItemType((Tag<Material>)Tag.ITEMS_LEG_ARMOR);
            BOOTS = new ItemType((Tag<Material>)Tag.ITEMS_FOOT_ARMOR);
        } else {
            CHESTPLATE = new ItemType(Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE, Material.ELYTRA);
            LEGGINGS = new ItemType(Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS);
            BOOTS = new ItemType(Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.GOLDEN_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS);
        }
        if (Skript.fieldExists(Tag.class, "ITEMS_HARNESSES")) {
            HAPPY_GHAST_HARNESS = new ItemType((Tag<Material>)Tag.ITEMS_HARNESSES);
            try {
                HAPPY_GHAST_CLASS = Class.forName("org.bukkit.entity.HappyGhast");
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            HAPPY_GHAST_HARNESS = new ItemType();
            HAPPY_GHAST_CLASS = null;
        }
        ALL_EQUIPMENT = new ItemType[]{CHESTPLATE, LEGGINGS, BOOTS, HORSE_ARMOR, SADDLE, CHEST, CARPET, WOLF_ARMOR, HAPPY_GHAST_HARNESS};
        Skript.registerEffect(EffEquip.class, "equip [%livingentities%] with %itemtypes%", "make %livingentities% wear %itemtypes%", "unequip %itemtypes% [from %livingentities%]", "unequip %livingentities%'[s] (armo[u]r|equipment)");
    }
}

