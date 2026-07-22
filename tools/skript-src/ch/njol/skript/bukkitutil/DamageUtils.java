/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.damage.DamageSource
 *  org.bukkit.damage.DamageType
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.bukkitutil;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class DamageUtils {
    @NotNull
    public static DamageSource getDamageSourceFromCause(EntityDamageEvent.DamageCause cause) {
        return DamageSource.builder((DamageType)(switch (cause) {
            case EntityDamageEvent.DamageCause.KILL, EntityDamageEvent.DamageCause.SUICIDE -> DamageType.GENERIC_KILL;
            case EntityDamageEvent.DamageCause.WORLD_BORDER, EntityDamageEvent.DamageCause.VOID -> DamageType.OUT_OF_WORLD;
            case EntityDamageEvent.DamageCause.CONTACT -> DamageType.CACTUS;
            case EntityDamageEvent.DamageCause.SUFFOCATION -> DamageType.IN_WALL;
            case EntityDamageEvent.DamageCause.FALL -> DamageType.FALL;
            case EntityDamageEvent.DamageCause.FIRE -> DamageType.ON_FIRE;
            case EntityDamageEvent.DamageCause.FIRE_TICK -> DamageType.IN_FIRE;
            case EntityDamageEvent.DamageCause.LAVA -> DamageType.LAVA;
            case EntityDamageEvent.DamageCause.DROWNING -> DamageType.DROWN;
            case EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION -> DamageType.EXPLOSION;
            case EntityDamageEvent.DamageCause.LIGHTNING -> DamageType.LIGHTNING_BOLT;
            case EntityDamageEvent.DamageCause.STARVATION -> DamageType.STARVE;
            case EntityDamageEvent.DamageCause.MAGIC, EntityDamageEvent.DamageCause.POISON -> DamageType.MAGIC;
            case EntityDamageEvent.DamageCause.WITHER -> DamageType.WITHER;
            case EntityDamageEvent.DamageCause.FALLING_BLOCK -> DamageType.FALLING_BLOCK;
            case EntityDamageEvent.DamageCause.THORNS -> DamageType.THORNS;
            case EntityDamageEvent.DamageCause.DRAGON_BREATH -> DamageType.DRAGON_BREATH;
            case EntityDamageEvent.DamageCause.FLY_INTO_WALL -> DamageType.FLY_INTO_WALL;
            case EntityDamageEvent.DamageCause.HOT_FLOOR -> DamageType.HOT_FLOOR;
            case EntityDamageEvent.DamageCause.CAMPFIRE -> DamageType.CAMPFIRE;
            case EntityDamageEvent.DamageCause.CRAMMING -> DamageType.CRAMMING;
            case EntityDamageEvent.DamageCause.DRYOUT -> DamageType.DRY_OUT;
            case EntityDamageEvent.DamageCause.FREEZE -> DamageType.FREEZE;
            case EntityDamageEvent.DamageCause.SONIC_BOOM -> DamageType.SONIC_BOOM;
            default -> DamageType.GENERIC;
        })).build();
    }
}

