/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.attribute.Attributable
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.damage.DamageSource
 *  org.bukkit.damage.DamageType
 *  org.bukkit.entity.Damageable
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import ch.njol.util.Math2;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

public class HealthUtils {
    @Nullable
    private static final Constructor<EntityDamageEvent> OLD_DAMAGE_EVENT_CONSTRUCTOR;
    private static final Attribute MAX_HEALTH;

    public static double getHealth(Damageable damageable) {
        if (damageable.isDead()) {
            return 0.0;
        }
        return damageable.getHealth() / 2.0;
    }

    public static void setHealth(Damageable damageable, double health) {
        damageable.setHealth(Math2.fit(0.0, health, HealthUtils.getMaxHealth(damageable)) * 2.0);
    }

    public static double getMaxHealth(Damageable damageable) {
        AttributeInstance attributeInstance = ((Attributable)damageable).getAttribute(MAX_HEALTH);
        assert (attributeInstance != null);
        return attributeInstance.getValue() / 2.0;
    }

    public static void setMaxHealth(Damageable damageable, double health) {
        AttributeInstance attributeInstance = ((Attributable)damageable).getAttribute(MAX_HEALTH);
        assert (attributeInstance != null);
        attributeInstance.setBaseValue(health * 2.0);
    }

    public static void damage(Damageable damageable, double damage) {
        if (damage < 0.0) {
            HealthUtils.heal(damageable, -damage);
            return;
        }
        damageable.damage(damage * 2.0);
    }

    public static void damage(Damageable damageable, double damage, DamageSource cause) {
        if (damage < 0.0) {
            HealthUtils.heal(damageable, -damage);
            return;
        }
        damageable.damage(damage * 2.0, cause);
    }

    public static void heal(Damageable damageable, double health) {
        if (health < 0.0) {
            HealthUtils.damage(damageable, -health);
            return;
        }
        HealthUtils.setHealth(damageable, HealthUtils.getHealth(damageable) + health);
    }

    public static double getDamage(EntityDamageEvent event) {
        return event.getDamage() / 2.0;
    }

    public static double getFinalDamage(EntityDamageEvent event) {
        return event.getFinalDamage() / 2.0;
    }

    public static void setDamage(EntityDamageEvent event, double damage) {
        event.setDamage(damage * 2.0);
        if (event.getEntity() instanceof LivingEntity) {
            ((LivingEntity)event.getEntity()).setLastDamage(damage * 2.0);
        }
    }

    public static void setDamageCause(Damageable damageable, EntityDamageEvent.DamageCause cause) {
        if (OLD_DAMAGE_EVENT_CONSTRUCTOR != null) {
            try {
                damageable.setLastDamageCause(OLD_DAMAGE_EVENT_CONSTRUCTOR.newInstance(damageable, cause, 0));
            }
            catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                Skript.exception("Failed to set last damage cause");
            }
        } else {
            damageable.setLastDamageCause(new EntityDamageEvent((Entity)damageable, cause, DamageSource.builder((DamageType)DamageType.GENERIC).build(), 0.0));
        }
    }

    static {
        Constructor constructor = null;
        try {
            constructor = EntityDamageEvent.class.getConstructor(Damageable.class, EntityDamageEvent.DamageCause.class, Double.TYPE);
        }
        catch (NoSuchMethodException noSuchMethodException) {
            // empty catch block
        }
        OLD_DAMAGE_EVENT_CONSTRUCTOR = constructor;
        MAX_HEALTH = Skript.isRunningMinecraft(1, 21, 3) ? (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)"max_health")) : Enum.valueOf(Attribute.class, "GENERIC_MAX_HEALTH");
    }
}

