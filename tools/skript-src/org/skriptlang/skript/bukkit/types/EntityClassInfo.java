/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.types;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.PlayerUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.util.Experience;
import ch.njol.skript.util.Utils;
import ch.njol.util.coll.CollectionUtils;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Internal
public class EntityClassInfo
extends ClassInfo<Entity> {
    public EntityClassInfo() {
        super(Entity.class, "entity");
        this.user("entit(y|ies)").name("Entity").description("An entity is something in a <a href='#world'>world</a> that's not a <a href='#block'>block</a>, e.g. a <a href='#player'>player</a>, a skeleton, or a zombie, but also <a href='#projectile'>projectiles</a> like arrows, fireballs or thrown potions, or special entities like dropped items, falling blocks or paintings.").usage("player, op, wolf, tamed ocelot, powered creeper, zombie, unsaddled pig, fireball, arrow, dropped item, item frame, etc.").examples("entity is a zombie or creeper", "player is an op", "projectile is an arrow", "shoot a fireball from the player").since("1.0").defaultExpression(new EventValueExpression<Entity>(Entity.class)).parser(new EntityParser()).changer(new EntityChanger()).property(Property.NAME, "The entity's name, if it has one, as text.Note that the regular name cannot be changed, meaning the entity's custom (display) name will be changed instead.", Skript.instance(), EntityNameHandler.name()).property(Property.DISPLAY_NAME, "The entity's custom name, if it has one, as text. Can be set or reset.", Skript.instance(), EntityNameHandler.displayName());
    }

    private static class EntityParser
    extends Parser<Entity> {
        private EntityParser() {
        }

        @Override
        @Nullable
        public Entity parse(String s, ParseContext context) {
            if (Utils.isValidUUID(s)) {
                return Bukkit.getEntity((UUID)UUID.fromString(s));
            }
            return null;
        }

        @Override
        public boolean canParse(ParseContext context) {
            return context == ParseContext.COMMAND || context == ParseContext.PARSE;
        }

        @Override
        public String toVariableNameString(Entity entity) {
            return "entity:" + entity.getUniqueId().toString().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public String toString(Entity e, int flags) {
            return EntityData.toString(e, flags);
        }
    }

    public static class EntityChanger
    implements Changer<Entity> {
        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            return switch (mode) {
                default -> throw new MatchException(null, null);
                case Changer.ChangeMode.ADD -> CollectionUtils.array(ItemType[].class, Inventory.class, Experience[].class);
                case Changer.ChangeMode.DELETE -> CollectionUtils.array(new Class[0]);
                case Changer.ChangeMode.REMOVE -> CollectionUtils.array(PotionEffectType[].class, ItemType[].class, Inventory.class);
                case Changer.ChangeMode.REMOVE_ALL -> CollectionUtils.array(PotionEffectType[].class, ItemType[].class);
                case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> null;
            };
        }

        public void change(Entity[] entities, Object @Nullable [] delta, Changer.ChangeMode mode) {
            if (delta == null) {
                for (Entity entity : entities) {
                    if (entity instanceof Player) continue;
                    entity.remove();
                }
                return;
            }
            boolean hasItem = false;
            for (Entity entity : entities) {
                for (Object deltaObj : delta) {
                    if (deltaObj instanceof PotionEffectType) {
                        PotionEffectType potionEffectType = (PotionEffectType)deltaObj;
                        assert (mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.REMOVE_ALL);
                        if (!(entity instanceof LivingEntity)) continue;
                        LivingEntity livingEntity = (LivingEntity)entity;
                        livingEntity.removePotionEffect(potionEffectType);
                        continue;
                    }
                    if (!(entity instanceof Player)) continue;
                    Player player = (Player)entity;
                    if (deltaObj instanceof Experience) {
                        Experience experience = (Experience)deltaObj;
                        player.giveExp(experience.getXP());
                        continue;
                    }
                    if (deltaObj instanceof Inventory) {
                        Inventory itemStacks = (Inventory)deltaObj;
                        PlayerInventory inventory = player.getInventory();
                        for (ItemStack itemStack : itemStacks) {
                            if (itemStack == null) continue;
                            if (mode == Changer.ChangeMode.ADD) {
                                inventory.addItem(new ItemStack[]{itemStack});
                                continue;
                            }
                            inventory.remove(itemStack);
                        }
                        continue;
                    }
                    if (!(deltaObj instanceof ItemType)) continue;
                    ItemType itemType = (ItemType)deltaObj;
                    hasItem = true;
                    PlayerInventory invi = player.getInventory();
                    if (mode == Changer.ChangeMode.ADD) {
                        itemType.addTo((Inventory)invi);
                        continue;
                    }
                    if (mode == Changer.ChangeMode.REMOVE) {
                        itemType.removeFrom((Inventory)invi);
                        continue;
                    }
                    itemType.removeAll((Inventory)invi);
                }
                if (!(entity instanceof Player)) continue;
                Player player = (Player)entity;
                if (!hasItem) continue;
                PlayerUtils.updateInventory(player);
            }
        }
    }

    private static class EntityNameHandler
    implements ExpressionPropertyHandler<Entity, Component> {
        private final boolean isDisplayName;

        public static EntityNameHandler name() {
            return new EntityNameHandler(false);
        }

        public static EntityNameHandler displayName() {
            return new EntityNameHandler(true);
        }

        private EntityNameHandler(boolean isDisplayName) {
            this.isDisplayName = isDisplayName;
        }

        @Override
        public Component convert(Entity entity) {
            if (this.isDisplayName) {
                return entity.customName();
            }
            return entity.name();
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            Class[] classArray;
            switch (mode) {
                case DELETE: 
                case SET: 
                case RESET: {
                    Class[] classArray2 = new Class[1];
                    classArray = classArray2;
                    classArray2[0] = Component.class;
                    break;
                }
                default: {
                    classArray = null;
                }
            }
            return classArray;
        }

        @Override
        public void change(Entity entity, Object @Nullable [] delta, Changer.ChangeMode mode) {
            Component name = delta == null ? null : (Component)delta[0];
            entity.customName(name);
            if (this.isDisplayName || mode == Changer.ChangeMode.RESET) {
                entity.setCustomNameVisible(name != null);
            }
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                living.setRemoveWhenFarAway(name == null);
            }
        }

        @Override
        @NotNull
        public Class<Component> returnType() {
            return Component.class;
        }
    }
}

