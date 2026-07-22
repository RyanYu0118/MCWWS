/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.HashBiMap
 *  org.bukkit.Keyed
 *  org.bukkit.Registry
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.registry.RegistryClassInfo;
import ch.njol.skript.util.PaperUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class BukkitUtils {
    private static final BiMap<EquipmentSlot, Integer> BUKKIT_EQUIPMENT_SLOT_INDICES = HashBiMap.create();

    public static boolean registryExists(String registry) {
        return Skript.classExists("org.bukkit.Registry") && Skript.fieldExists(Registry.class, registry);
    }

    @Deprecated(since="2.14", forRemoval=true)
    @Nullable
    public static Registry<PotionEffectType> getPotionEffectTypeRegistry() {
        if (BukkitUtils.registryExists("MOB_EFFECT")) {
            return Registry.MOB_EFFECT;
        }
        if (BukkitUtils.registryExists("EFFECT")) {
            return Registry.EFFECT;
        }
        return null;
    }

    public static Integer getEquipmentSlotIndex(EquipmentSlot equipmentSlot) {
        return (Integer)BUKKIT_EQUIPMENT_SLOT_INDICES.get((Object)equipmentSlot);
    }

    public static EquipmentSlot getEquipmentSlotFromIndex(int slotIndex) {
        return (EquipmentSlot)BUKKIT_EQUIPMENT_SLOT_INDICES.inverse().get((Object)slotIndex);
    }

    @Nullable
    public static <R extends Keyed> RegistryClassInfo<?> getRegistryClassInfo(String classPath, String registryName, String codeName, String languageNode) {
        if (!Skript.classExists(classPath)) {
            return null;
        }
        Registry registry = null;
        if (BukkitUtils.registryExists(registryName)) {
            try {
                registry = (Registry)Registry.class.getField(registryName).get(null);
            }
            catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        } else if (PaperUtils.registryExists(registryName)) {
            registry = PaperUtils.getBukkitRegistry(registryName);
        }
        if (registry != null) {
            Class<?> registryClass;
            try {
                registryClass = Class.forName(classPath);
            }
            catch (ClassNotFoundException e) {
                Skript.debug("Could not retrieve the class with the path: '" + classPath + "'.");
                throw new RuntimeException(e);
            }
            return new RegistryClassInfo(registryClass, registry, codeName, languageNode);
        }
        Skript.debug("There were no registries found for '" + registryName + "'.");
        return null;
    }

    static {
        BUKKIT_EQUIPMENT_SLOT_INDICES.put((Object)EquipmentSlot.FEET, (Object)36);
        BUKKIT_EQUIPMENT_SLOT_INDICES.put((Object)EquipmentSlot.LEGS, (Object)37);
        BUKKIT_EQUIPMENT_SLOT_INDICES.put((Object)EquipmentSlot.CHEST, (Object)38);
        BUKKIT_EQUIPMENT_SLOT_INDICES.put((Object)EquipmentSlot.HEAD, (Object)39);
        BUKKIT_EQUIPMENT_SLOT_INDICES.put((Object)EquipmentSlot.OFF_HAND, (Object)40);
    }
}

