/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.registry.RegistryKey
 *  io.papermc.paper.registry.set.RegistryKeySet
 *  io.papermc.paper.registry.set.RegistrySet
 *  org.bukkit.Keyed
 *  org.bukkit.Registry
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.jetbrains.annotations.Nullable;

public class ComponentUtils {
    public static <T extends Keyed> Collection<T> registryKeySetToCollection(@Nullable RegistryKeySet<T> registryKeySet, Registry<T> registry) {
        if (registryKeySet == null || registryKeySet.isEmpty()) {
            return Collections.emptyList();
        }
        return registryKeySet.resolve(registry);
    }

    public static <T extends Keyed> RegistryKeySet<T> collectionToRegistryKeySet(Collection<T> collection, RegistryKey<T> registryKey) {
        return RegistrySet.keySetFromValues(registryKey, collection);
    }
}

