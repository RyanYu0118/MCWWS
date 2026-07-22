/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.loot.LootContext
 *  org.bukkit.loot.LootContext$Builder
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.loottables;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LootContextWrapper {
    @NotNull
    private Location location;
    @Nullable
    private transient LootContext cachedLootContext;
    @Nullable
    private Player killer;
    @Nullable
    private Entity entity;
    private float luck;

    public LootContextWrapper(@NotNull Location location) {
        this.location = location;
    }

    public LootContext getContext() {
        if (this.cachedLootContext == null) {
            this.cachedLootContext = new LootContext.Builder(this.location).killer((HumanEntity)this.killer).lootedEntity(this.entity).luck(this.luck).build();
        }
        return this.cachedLootContext;
    }

    public void setLocation(@NotNull Location location) {
        this.location = location;
        this.cachedLootContext = null;
    }

    public void setKiller(@Nullable Player killer) {
        this.killer = killer;
        this.cachedLootContext = null;
    }

    public void setEntity(@Nullable Entity entity) {
        this.entity = entity;
        this.cachedLootContext = null;
    }

    public void setLuck(float luck) {
        this.luck = luck;
        this.cachedLootContext = null;
    }

    public Location getLocation() {
        return this.location;
    }

    @Nullable
    public Player getKiller() {
        return this.killer;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }

    public float getLuck() {
        return this.luck;
    }
}

