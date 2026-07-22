/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.registry.RegistryAccess
 *  io.papermc.paper.registry.RegistryKey
 *  org.bukkit.Registry
 *  org.bukkit.entity.ZombieNautilus
 *  org.bukkit.entity.ZombieNautilus$Variant
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.entitydata;

import ch.njol.skript.classes.registry.RegistryClassInfo;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.Objects;
import org.bukkit.Registry;
import org.bukkit.entity.ZombieNautilus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZombieNautilusData
extends EntityData<ZombieNautilus> {
    private static ZombieNautilus.Variant[] VARIANTS;
    private Kleenean isTamed = Kleenean.UNKNOWN;
    @Nullable
    private ZombieNautilus.Variant variant = null;

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public static void register() {
        EntityData.register(ZombieNautilusData.class, "zombie nautilus", ZombieNautilus.class, 0, "zombie nautilus");
        Variables.yggdrasil.registerSingleClass(ZombieNautilus.Variant.class, "ZombieNautilus.Variant");
        @NotNull Registry variantRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ZOMBIE_NAUTILUS_VARIANT);
        VARIANTS = (ZombieNautilus.Variant[])variantRegistry.stream().toArray(ZombieNautilus.Variant[]::new);
        Classes.registerClass(new RegistryClassInfo<ZombieNautilus.Variant>(ZombieNautilus.Variant.class, variantRegistry, "zombienautilusvariant", "zombie nautilus variants").user("zombie ?nautilus ?variants?").name("Zombie Nautilus Variant").description("Represents the variant of a zombie nautilus.").since("2.14").documentationId("ZombieNautilusVariant"));
    }

    public ZombieNautilusData() {
    }

    public ZombieNautilusData(@Nullable ZombieNautilus.Variant variant) {
        this.variant = variant;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (parseResult.hasTag("tamed")) {
            this.isTamed = Kleenean.TRUE;
        }
        if (exprs[0] != null) {
            this.variant = (ZombieNautilus.Variant)exprs[0].getSingle();
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends ZombieNautilus> entityClass, @Nullable ZombieNautilus zombieNautilus) {
        if (zombieNautilus != null) {
            this.isTamed = Kleenean.get(zombieNautilus.isTamed());
            this.variant = zombieNautilus.getVariant();
        }
        return true;
    }

    @Override
    public void set(ZombieNautilus zombieNautilus) {
        zombieNautilus.setTamed(this.isTamed.isTrue());
        ZombieNautilus.Variant variant = this.variant;
        if (variant == null) {
            variant = CollectionUtils.getRandom(VARIANTS);
        }
        assert (variant != null);
        zombieNautilus.setVariant(variant);
    }

    @Override
    protected boolean match(ZombieNautilus zombieNautilus) {
        return this.kleeneanMatch(this.isTamed, zombieNautilus.isTamed()) && this.dataMatch(this.variant, zombieNautilus.getVariant());
    }

    @Override
    public Class<? extends ZombieNautilus> getType() {
        return ZombieNautilus.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new ZombieNautilusData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hash(new Object[]{this.isTamed, this.variant});
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof ZombieNautilusData)) {
            return false;
        }
        ZombieNautilusData other = (ZombieNautilusData)entityData;
        return this.isTamed == other.isTamed && this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof ZombieNautilusData)) {
            return false;
        }
        ZombieNautilusData other = (ZombieNautilusData)entityData;
        return this.kleeneanMatch(this.isTamed, other.isTamed) && this.dataMatch(this.variant, other.variant);
    }
}

