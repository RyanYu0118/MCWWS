/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.HashBiMap
 *  org.bukkit.Location
 *  org.bukkit.entity.Ageable
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Piglin
 *  org.bukkit.entity.Zoglin
 *  org.bukkit.entity.Zombie
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Zombie;
import org.jetbrains.annotations.Nullable;

public class EntityUtils {
    private static final boolean HAS_PIGLINS = Skript.classExists("org.bukkit.entity.Piglin");
    private static final BiMap<EntityData<?>, EntityType> SPAWNER_TYPES = HashBiMap.create();
    private static final Map<Class<? extends Entity>, EntityType> CLASS_ENTITY_TYPE_MAP = new HashMap<Class<? extends Entity>, EntityType>();

    public static boolean isAgeable(Entity entity) {
        if (entity instanceof Ageable || entity instanceof Zombie) {
            return true;
        }
        return HAS_PIGLINS && (entity instanceof Piglin || entity instanceof Zoglin);
    }

    public static int getAge(Entity entity) {
        if (entity instanceof Ageable) {
            return ((Ageable)entity).getAge();
        }
        if (entity instanceof Zombie) {
            return ((Zombie)entity).isBaby() ? -1 : 0;
        }
        if (HAS_PIGLINS) {
            if (entity instanceof Piglin) {
                return ((Piglin)entity).isBaby() ? -1 : 0;
            }
            if (entity instanceof Zoglin) {
                return ((Zoglin)entity).isBaby() ? -1 : 0;
            }
        }
        return 0;
    }

    public static void setAge(Entity entity, int age) {
        if (entity instanceof Ageable) {
            ((Ageable)entity).setAge(age);
        } else if (entity instanceof Zombie) {
            ((Zombie)entity).setBaby(age < 0);
        } else if (HAS_PIGLINS) {
            if (entity instanceof Piglin) {
                ((Piglin)entity).setBaby(age < 0);
            } else if (entity instanceof Zoglin) {
                ((Zoglin)entity).setBaby(age < 0);
            }
        }
    }

    public static void setBaby(Entity entity) {
        EntityUtils.setAge(entity, -24000);
    }

    public static void setAdult(Entity entity) {
        EntityUtils.setAge(entity, 0);
    }

    public static boolean isAdult(Entity entity) {
        return EntityUtils.getAge(entity) >= 0;
    }

    private static void loadSpawnerTypes() {
        for (EntityType e : EntityType.values()) {
            Class c = e.getEntityClass();
            if (c == null) continue;
            SPAWNER_TYPES.put(EntityData.fromClass(c), (Object)e);
        }
    }

    public static EntityType toBukkitEntityType(EntityData<?> e) {
        EntityData<?> entityData;
        if (SPAWNER_TYPES.isEmpty()) {
            EntityUtils.loadSpawnerTypes();
        }
        if (SPAWNER_TYPES.containsKey(entityData = EntityData.fromClass(e.getType()))) {
            return (EntityType)SPAWNER_TYPES.get(entityData);
        }
        return EntityUtils.toBukkitEntityType(e.getType());
    }

    @Nullable
    public static EntityType toBukkitEntityType(Class<? extends Entity> entityClass) {
        if (CLASS_ENTITY_TYPE_MAP.containsKey(entityClass)) {
            return CLASS_ENTITY_TYPE_MAP.get(entityClass);
        }
        EntityType closestEntityType = null;
        Class closestClass = null;
        for (EntityType entityType : EntityType.values()) {
            Class typeClass = entityType.getEntityClass();
            if (typeClass == null || !typeClass.isAssignableFrom(entityClass) || closestEntityType != null && !closestClass.isAssignableFrom(typeClass)) continue;
            closestEntityType = entityType;
            closestClass = typeClass;
            if (typeClass.equals(entityClass)) break;
        }
        CLASS_ENTITY_TYPE_MAP.put(entityClass, closestEntityType);
        return closestEntityType;
    }

    public static EntityData<?> toSkriptEntityData(EntityType e) {
        if (SPAWNER_TYPES.isEmpty()) {
            EntityUtils.loadSpawnerTypes();
        }
        return (EntityData)SPAWNER_TYPES.inverse().get((Object)e);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static void teleport(Entity entity, Location location) {
        if (location.getWorld() == null) {
            location = location.clone();
            location.setWorld(entity.getWorld());
        }
        entity.teleport(location);
    }

    static {
        for (EntityType entityType : EntityType.values()) {
            Class entityClass = entityType.getEntityClass();
            if (entityClass == null) continue;
            CLASS_ENTITY_TYPE_MAP.put(entityClass, entityType);
        }
    }
}

