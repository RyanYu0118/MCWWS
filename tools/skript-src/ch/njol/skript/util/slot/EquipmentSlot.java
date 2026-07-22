/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.bukkitutil.BukkitUtils;
import ch.njol.skript.bukkitutil.PlayerUtils;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.SlotWithIndex;
import com.google.common.base.Preconditions;
import java.util.Locale;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EquipmentSlot
extends SlotWithIndex {
    private final EntityEquipment entityEquipment;
    private EquipSlot skriptSlot;
    private final int slotIndex;
    private final boolean slotToString;
    private org.bukkit.inventory.EquipmentSlot bukkitSlot;

    @Deprecated(since="2.11.0", forRemoval=true)
    public EquipmentSlot(@NotNull EntityEquipment entityEquipment, @NotNull EquipSlot skriptSlot, boolean slotToString) {
        Entity holder;
        Preconditions.checkNotNull((Object)entityEquipment, (Object)"entityEquipment cannot be null");
        Preconditions.checkNotNull((Object)((Object)skriptSlot), (Object)"skriptSlot cannot be null");
        this.entityEquipment = entityEquipment;
        int slotIndex = -1;
        if (skriptSlot == EquipSlot.TOOL && (holder = entityEquipment.getHolder()) instanceof Player) {
            Player player = (Player)holder;
            slotIndex = player.getInventory().getHeldItemSlot();
        }
        this.slotIndex = slotIndex;
        this.skriptSlot = skriptSlot;
        this.slotToString = slotToString;
    }

    @Deprecated(since="2.11.0", forRemoval=true)
    public EquipmentSlot(@NotNull EntityEquipment entityEquipment, @NotNull EquipSlot skriptSlot) {
        this(entityEquipment, skriptSlot, false);
    }

    public EquipmentSlot(@NotNull EntityEquipment entityEquipment, @NotNull org.bukkit.inventory.EquipmentSlot bukkitSlot, boolean slotToString) {
        Entity holder;
        Preconditions.checkNotNull((Object)entityEquipment, (Object)"entityEquipment cannot be null");
        Preconditions.checkNotNull((Object)bukkitSlot, (Object)"bukkitSlot cannot be null");
        this.entityEquipment = entityEquipment;
        int slotIndex = -1;
        if (bukkitSlot == org.bukkit.inventory.EquipmentSlot.HAND && (holder = entityEquipment.getHolder()) instanceof Player) {
            Player player = (Player)holder;
            slotIndex = player.getInventory().getHeldItemSlot();
        }
        this.slotIndex = slotIndex;
        this.bukkitSlot = bukkitSlot;
        this.slotToString = slotToString;
    }

    public EquipmentSlot(@NotNull EntityEquipment equipment, @NotNull org.bukkit.inventory.EquipmentSlot bukkitSlot) {
        this(equipment, bukkitSlot, false);
    }

    public EquipmentSlot(@NotNull HumanEntity holder, int index) {
        this(holder.getEquipment(), BukkitUtils.getEquipmentSlotFromIndex(index), true);
    }

    public EquipmentSlot(@NotNull HumanEntity holder, @NotNull org.bukkit.inventory.EquipmentSlot bukkitSlot) {
        this(holder.getEquipment(), bukkitSlot, true);
    }

    @Override
    @Nullable
    public ItemStack getItem() {
        if (this.skriptSlot != null) {
            return this.skriptSlot.get(this.entityEquipment);
        }
        return this.entityEquipment.getItem(this.bukkitSlot);
    }

    @Override
    public void setItem(@Nullable ItemStack item) {
        if (this.skriptSlot != null) {
            this.skriptSlot.set(this.entityEquipment, item);
        } else {
            this.entityEquipment.setItem(this.bukkitSlot, item);
        }
        Entity entity = this.entityEquipment.getHolder();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            PlayerUtils.updateInventory(player);
        }
    }

    @Override
    public int getAmount() {
        ItemStack item = this.getItem();
        return item != null ? item.getAmount() : 0;
    }

    @Override
    public void setAmount(int amount) {
        ItemStack item = this.getItem();
        if (item != null) {
            item.setAmount(amount);
        }
        this.setItem(item);
    }

    @Deprecated(since="2.11.0", forRemoval=true)
    public EquipSlot getEquipSlot() {
        return this.skriptSlot;
    }

    public org.bukkit.inventory.EquipmentSlot getEquipmentSlot() {
        return this.bukkitSlot;
    }

    @Override
    public int getIndex() {
        if (this.slotIndex != -1) {
            return this.slotIndex;
        }
        if (this.skriptSlot != null) {
            return this.skriptSlot.slotNumber;
        }
        if (BukkitUtils.getEquipmentSlotIndex(this.bukkitSlot) != null) {
            return BukkitUtils.getEquipmentSlotIndex(this.bukkitSlot);
        }
        return -1;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.slotToString) {
            SyntaxStringBuilder syntaxBuilder = new SyntaxStringBuilder(event, debug);
            syntaxBuilder.append((Object)"the ");
            if (this.skriptSlot != null) {
                syntaxBuilder.append((Object)this.skriptSlot.name().toLowerCase(Locale.ENGLISH));
            } else {
                syntaxBuilder.append((Object)this.bukkitSlot.name().replace('_', ' ').toLowerCase(Locale.ENGLISH));
            }
            syntaxBuilder.append((Object)" of ").append((Object)Classes.toString(this.entityEquipment.getHolder()));
            return syntaxBuilder.toString();
        }
        return Classes.toString(this.getItem());
    }

    @Deprecated(since="2.11.0", forRemoval=true)
    public static enum EquipSlot {
        TOOL{

            @Override
            public ItemStack get(EntityEquipment entityEquipment) {
                return entityEquipment.getItemInMainHand();
            }

            @Override
            public void set(EntityEquipment entityEquipment, @Nullable ItemStack item) {
                entityEquipment.setItemInMainHand(item);
            }
        }
        ,
        OFF_HAND(40){

            @Override
            public ItemStack get(EntityEquipment entityEquipment) {
                return entityEquipment.getItemInOffHand();
            }

            @Override
            public void set(EntityEquipment entityEquipment, @Nullable ItemStack item) {
                entityEquipment.setItemInOffHand(item);
            }
        }
        ,
        HELMET(39){

            @Override
            @Nullable
            public ItemStack get(EntityEquipment entityEquipment) {
                return entityEquipment.getHelmet();
            }

            @Override
            public void set(EntityEquipment entityEquipment, @Nullable ItemStack item) {
                entityEquipment.setHelmet(item);
            }
        }
        ,
        CHESTPLATE(38){

            @Override
            @Nullable
            public ItemStack get(EntityEquipment entityEquipment) {
                return entityEquipment.getChestplate();
            }

            @Override
            public void set(EntityEquipment entityEquipment, @Nullable ItemStack item) {
                entityEquipment.setChestplate(item);
            }
        }
        ,
        LEGGINGS(37){

            @Override
            @Nullable
            public ItemStack get(EntityEquipment entityEquipment) {
                return entityEquipment.getLeggings();
            }

            @Override
            public void set(EntityEquipment entityEquipment, @Nullable ItemStack item) {
                entityEquipment.setLeggings(item);
            }
        }
        ,
        BOOTS(36){

            @Override
            @Nullable
            public ItemStack get(EntityEquipment entityEquipment) {
                return entityEquipment.getBoots();
            }

            @Override
            public void set(EntityEquipment entityEquipment, @Nullable ItemStack item) {
                entityEquipment.setBoots(item);
            }
        }
        ,
        BODY{

            @Override
            public ItemStack get(EntityEquipment entityEquipment) {
                return entityEquipment.getItem(org.bukkit.inventory.EquipmentSlot.BODY);
            }

            @Override
            public void set(EntityEquipment entityEquipment, @Nullable ItemStack item) {
                entityEquipment.setItem(org.bukkit.inventory.EquipmentSlot.BODY, item);
            }
        };

        public final int slotNumber;

        private EquipSlot() {
            this.slotNumber = -1;
        }

        private EquipSlot(int number) {
            this.slotNumber = number;
        }

        @Nullable
        public abstract ItemStack get(EntityEquipment var1);

        public abstract void set(EntityEquipment var1, @Nullable ItemStack var2);
    }
}

