/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.GameRule
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Nameable
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Server
 *  org.bukkit.World
 *  org.bukkit.WorldType
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.DoubleChest
 *  org.bukkit.command.BlockCommandSender
 *  org.bukkit.command.CommandSender
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.enchantments.EnchantmentOffer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntitySnapshot
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scoreboard.Objective
 *  org.bukkit.scoreboard.Team
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes.data;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.entity.EntityType;
import ch.njol.skript.entity.XpOrbData;
import ch.njol.skript.lang.util.common.AnyAmount;
import ch.njol.skript.lang.util.common.AnyNamed;
import ch.njol.skript.util.BlockInventoryHolder;
import ch.njol.skript.util.BlockUtils;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.skript.util.Experience;
import ch.njol.skript.util.slot.Slot;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.script.Script;

public class DefaultConverters {
    static {
        Converters.registerConverter(Number.class, Byte.class, Number::byteValue);
        Converters.registerConverter(Number.class, Double.class, Number::doubleValue);
        Converters.registerConverter(Number.class, Float.class, Number::floatValue);
        Converters.registerConverter(Number.class, Integer.class, Number::intValue);
        Converters.registerConverter(Number.class, Long.class, Number::longValue);
        Converters.registerConverter(Number.class, Short.class, Number::shortValue);
        Converters.registerConverter(OfflinePlayer.class, PlayerInventory.class, p -> {
            if (!p.isOnline()) {
                return null;
            }
            Player online = p.getPlayer();
            assert (online != null);
            return online.getInventory();
        }, 8);
        Converters.registerConverter(OfflinePlayer.class, Player.class, OfflinePlayer::getPlayer, 8);
        Converters.registerConverter(CommandSender.class, Player.class, s -> {
            if (s instanceof Player) {
                return (Player)s;
            }
            return null;
        });
        Converters.registerConverter(BlockCommandSender.class, Block.class, BlockCommandSender::getBlock);
        Converters.registerConverter(Experience.class, Number.class, Experience::getXP);
        Converters.registerConverter(Entity.class, Player.class, e -> {
            if (e instanceof Player) {
                return (Player)e;
            }
            return null;
        });
        Converters.registerConverter(Entity.class, LivingEntity.class, e -> {
            if (e instanceof LivingEntity) {
                return (LivingEntity)e;
            }
            return null;
        });
        Converters.registerConverter(Block.class, Inventory.class, b -> {
            if (b.getState() instanceof InventoryHolder) {
                return ((InventoryHolder)b.getState()).getInventory();
            }
            return null;
        }, 8);
        Converters.registerConverter(Entity.class, Inventory.class, e -> {
            if (e instanceof InventoryHolder) {
                return ((InventoryHolder)e).getInventory();
            }
            return null;
        }, 8);
        Converters.registerConverter(Block.class, ItemType.class, ItemType::new, 9);
        Converters.registerConverter(Block.class, Location.class, BlockUtils::getLocation, 8);
        Converters.registerConverter(Entity.class, Location.class, Entity::getLocation, 8);
        Converters.registerConverter(Entity.class, EntityData.class, EntityData::fromEntity, 10);
        Converters.registerConverter(EntityData.class, EntityType.class, data -> new EntityType((EntityData<?>)data, -1));
        Converters.registerConverter(ItemType.class, ItemStack.class, ItemType::getRandom);
        Converters.registerConverter(ItemStack.class, ItemType.class, ItemType::new);
        Converters.registerConverter(Experience.class, XpOrbData.class, e -> new XpOrbData(e.getXP()));
        Converters.registerConverter(XpOrbData.class, Experience.class, e -> new Experience(e.getExperience()));
        Converters.registerConverter(Slot.class, ItemType.class, s -> {
            ItemStack i = s.getItem();
            return new ItemType(i != null ? i : new ItemStack(Material.AIR, 1));
        });
        Converters.registerConverter(Block.class, InventoryHolder.class, b -> {
            BlockState s = b.getState();
            if (s instanceof InventoryHolder) {
                return (InventoryHolder)s;
            }
            return null;
        }, 10);
        Converters.registerConverter(InventoryHolder.class, Block.class, holder -> {
            if (holder instanceof BlockState) {
                return new BlockInventoryHolder((BlockState)holder);
            }
            if (holder instanceof DoubleChest) {
                return holder.getInventory().getLocation().getBlock();
            }
            return null;
        }, 3);
        Converters.registerConverter(InventoryHolder.class, Entity.class, holder -> {
            if (holder instanceof Entity) {
                Entity entity = (Entity)holder;
                return entity;
            }
            return null;
        }, 3);
        Converters.registerConverter(Entity.class, InventoryHolder.class, entity -> {
            if (entity instanceof InventoryHolder) {
                InventoryHolder inventoryHolder = (InventoryHolder)entity;
                return inventoryHolder;
            }
            return null;
        }, 2);
        if (!SkriptConfig.useTypeProperties.value().booleanValue()) {
            Converters.registerConverter(OfflinePlayer.class, AnyNamed.class, player -> () -> ((OfflinePlayer)player).getName(), 2);
            if (Skript.classExists("org.bukkit.generator.WorldInfo")) {
                Converters.registerConverter(World.class, AnyNamed.class, world -> () -> ((World)world).getName(), 2);
            } else {
                Converters.registerConverter(World.class, AnyNamed.class, world -> () -> world.getName(), 2);
            }
            Converters.registerConverter(GameRule.class, AnyNamed.class, rule -> () -> ((GameRule)rule).getName(), 2);
            Converters.registerConverter(Server.class, AnyNamed.class, server -> () -> ((Server)server).getName(), 2);
            Converters.registerConverter(Plugin.class, AnyNamed.class, plugin -> () -> ((Plugin)plugin).getName(), 2);
            Converters.registerConverter(WorldType.class, AnyNamed.class, type -> () -> ((WorldType)type).getName(), 2);
            Converters.registerConverter(Team.class, AnyNamed.class, team -> () -> ((Team)team).getName(), 2);
            Converters.registerConverter(Objective.class, AnyNamed.class, objective -> () -> ((Objective)objective).getName(), 2);
            Converters.registerConverter(Nameable.class, AnyNamed.class, nameable -> new AnyNamed((Nameable)nameable){
                final /* synthetic */ Nameable val$nameable;
                {
                    this.val$nameable = nameable;
                }

                @Override
                @Nullable
                public String name() {
                    return this.val$nameable.getCustomName();
                }

                @Override
                @Nullable
                public Component nameComponent() {
                    return this.val$nameable.customName();
                }

                @Override
                public boolean supportsNameChange() {
                    return true;
                }

                @Override
                public void setName(String name) {
                    this.val$nameable.setCustomName(name);
                }

                @Override
                public void setName(Component name) {
                    this.val$nameable.customName(name);
                }
            }, 2);
            Converters.registerConverter(Block.class, AnyNamed.class, block -> new AnyNamed((Block)block){
                final /* synthetic */ Block val$block;
                {
                    this.val$block = block;
                }

                @Override
                @Nullable
                public String name() {
                    BlockState state = this.val$block.getState();
                    if (state instanceof Nameable) {
                        Nameable nameable = (Nameable)state;
                        return nameable.getCustomName();
                    }
                    return null;
                }

                @Override
                @Nullable
                public Component nameComponent() {
                    BlockState state = this.val$block.getState();
                    if (state instanceof Nameable) {
                        Nameable nameable = (Nameable)state;
                        return nameable.customName();
                    }
                    return null;
                }

                @Override
                public boolean supportsNameChange() {
                    return true;
                }

                @Override
                public void setName(String name) {
                    BlockState state = this.val$block.getState();
                    if (state instanceof Nameable) {
                        Nameable nameable = (Nameable)state;
                        nameable.setCustomName(name);
                        state.update(true, false);
                    }
                }

                @Override
                public void setName(Component name) {
                    BlockState state = this.val$block.getState();
                    if (state instanceof Nameable) {
                        Nameable nameable = (Nameable)state;
                        nameable.customName(name);
                        state.update(true, false);
                    }
                }
            }, 2);
            Converters.registerConverter(CommandSender.class, AnyNamed.class, thing -> () -> ((CommandSender)thing).getName(), 2);
            Converters.registerConverter(ItemStack.class, AnyAmount.class, item -> new AnyAmount((ItemStack)item){
                final /* synthetic */ ItemStack val$item;
                {
                    this.val$item = itemStack;
                }

                @Override
                @NotNull
                public Number amount() {
                    return this.val$item.getAmount();
                }

                @Override
                public boolean supportsAmountChange() {
                    return true;
                }

                @Override
                public void setAmount(Number amount) {
                    this.val$item.setAmount(amount != null ? amount.intValue() : 0);
                }
            }, 2);
        }
        Converters.registerConverter(InventoryHolder.class, Location.class, holder -> {
            if (holder instanceof Entity) {
                Entity entity = (Entity)holder;
                return entity.getLocation();
            }
            if (holder instanceof Block) {
                Block block = (Block)holder;
                return block.getLocation();
            }
            if (holder instanceof BlockState) {
                BlockState state = (BlockState)holder;
                return BlockUtils.getLocation(state.getBlock());
            }
            if (holder instanceof DoubleChest) {
                DoubleChest doubleChest = (DoubleChest)holder;
                if (doubleChest.getLeftSide() != null) {
                    return BlockUtils.getLocation(((BlockState)doubleChest.getLeftSide()).getBlock());
                }
                if (doubleChest.getRightSide() != null) {
                    return BlockUtils.getLocation(((BlockState)doubleChest.getRightSide()).getBlock());
                }
            }
            return null;
        });
        Converters.registerConverter(Enchantment.class, EnchantmentType.class, e -> new EnchantmentType((Enchantment)e, -1));
        Converters.registerConverter(Vector.class, Direction.class, Direction::new);
        Converters.registerConverter(EnchantmentOffer.class, EnchantmentType.class, eo -> new EnchantmentType(eo.getEnchantment(), eo.getEnchantmentLevel()));
        Converters.registerConverter(String.class, World.class, Bukkit::getWorld);
        if (Skript.classExists("org.bukkit.entity.EntitySnapshot")) {
            Converters.registerConverter(EntitySnapshot.class, EntityData.class, snapshot -> EntityUtils.toSkriptEntityData(snapshot.getEntityType()));
        }
        Converters.registerConverter(Script.class, Config.class, Script::getConfig);
        Converters.registerConverter(Config.class, Node.class, Config::getMainNode);
        Converters.registerConverter(UUID.class, String.class, UUID::toString);
    }
}

