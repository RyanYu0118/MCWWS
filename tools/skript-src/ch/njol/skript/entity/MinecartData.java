/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Minecart
 *  org.bukkit.entity.minecart.CommandMinecart
 *  org.bukkit.entity.minecart.ExplosiveMinecart
 *  org.bukkit.entity.minecart.HopperMinecart
 *  org.bukkit.entity.minecart.PoweredMinecart
 *  org.bukkit.entity.minecart.RideableMinecart
 *  org.bukkit.entity.minecart.SpawnerMinecart
 *  org.bukkit.entity.minecart.StorageMinecart
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.variables.Variables;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinecartData
extends EntityData<Minecart> {
    private static final MinecartType[] TYPES = MinecartType.values();
    private static final Patterns<MinecartType> PATTERNS = new Patterns(new Object[][]{{"minecart", MinecartType.ANY}, {"regular minecart", MinecartType.NORMAL}, {"storage minecart", MinecartType.STORAGE}, {"powered minecart", MinecartType.POWERED}, {"hopper minecart", MinecartType.HOPPER}, {"explosive minecart", MinecartType.EXPLOSIVE}, {"spawner minecart", MinecartType.SPAWNER}, {"command minecart", MinecartType.COMMAND}});
    private MinecartType type = MinecartType.ANY;

    public MinecartData() {
    }

    public MinecartData(@Nullable MinecartType type) {
        this.type = type != null ? type : MinecartType.ANY;
        this.codeNameIndex = PATTERNS.getMatchedPattern(this.type, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.type = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Minecart> entityClass, @Nullable Minecart minecart) {
        for (MinecartType type : TYPES) {
            if (type == MinecartType.ANY) continue;
            Class<? extends Minecart> typeClass = type.entityClass;
            if (!(minecart == null ? typeClass.isAssignableFrom(entityClass) : typeClass.isInstance(minecart))) continue;
            this.type = type;
            break;
        }
        if (this.type == null) {
            this.type = MinecartType.ANY;
        }
        this.codeNameIndex = PATTERNS.getMatchedPattern(this.type, 0).orElse(0);
        return true;
    }

    @Override
    public void set(Minecart minecart) {
    }

    @Override
    public boolean match(Minecart minecart) {
        if (this.type == MinecartType.ANY) {
            return true;
        }
        return this.type.entityClass.isInstance(minecart);
    }

    @Override
    public Class<? extends Minecart> getType() {
        return this.type.entityClass;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new MinecartData();
    }

    @Override
    protected int hashCode_i() {
        return this.type.hashCode();
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof MinecartData)) {
            return false;
        }
        MinecartData other = (MinecartData)entityData;
        return this.type == other.type;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof MinecartData)) {
            return false;
        }
        MinecartData other = (MinecartData)entityData;
        return this.type == MinecartType.ANY || this.type == other.type;
    }

    static {
        EntityData.register(MinecartData.class, "minecart", Minecart.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(MinecartType.class, "MinecartType");
    }

    public static enum MinecartType {
        ANY(Minecart.class),
        NORMAL(RideableMinecart.class),
        STORAGE(StorageMinecart.class),
        POWERED(PoweredMinecart.class),
        HOPPER(HopperMinecart.class),
        EXPLOSIVE(ExplosiveMinecart.class),
        SPAWNER(SpawnerMinecart.class),
        COMMAND(CommandMinecart.class);

        private final Class<? extends Minecart> entityClass;

        private MinecartType(Class<? extends Minecart> entityClass) {
            this.entityClass = entityClass;
        }
    }
}

