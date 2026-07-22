/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Nautilus
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.entitydata;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.Objects;
import org.bukkit.entity.Nautilus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NautilusData
extends EntityData<Nautilus> {
    private Kleenean isTamed = Kleenean.UNKNOWN;

    public static void register() {
        EntityData.register(NautilusData.class, "nautilus", Nautilus.class, 0, "nautilus");
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (parseResult.hasTag("tamed")) {
            this.isTamed = Kleenean.TRUE;
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Nautilus> entityClass, @Nullable Nautilus nautilus) {
        if (nautilus != null) {
            this.isTamed = Kleenean.get(nautilus.isTamed());
        }
        return true;
    }

    @Override
    public void set(Nautilus nautilus) {
        nautilus.setTamed(this.isTamed.isTrue());
    }

    @Override
    protected boolean match(Nautilus nautilus) {
        return this.kleeneanMatch(this.isTamed, nautilus.isTamed());
    }

    @Override
    public Class<? extends Nautilus> getType() {
        return Nautilus.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new NautilusData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode((Object)this.isTamed);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof NautilusData)) {
            return false;
        }
        NautilusData other = (NautilusData)entityData;
        return this.isTamed == other.isTamed;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof NautilusData)) {
            return false;
        }
        NautilusData other = (NautilusData)entityData;
        return this.kleeneanMatch(this.isTamed, other.isTamed);
    }
}

