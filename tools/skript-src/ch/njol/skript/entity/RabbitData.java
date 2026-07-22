/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Rabbit
 *  org.bukkit.entity.Rabbit$Type
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.entity.Rabbit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RabbitData
extends EntityData<Rabbit> {
    private static final Rabbit.Type[] TYPES = Rabbit.Type.values();
    private static final Patterns<Rabbit.Type> PATTERNS = new Patterns(new Object[][]{{"rabbit", null}, {"white rabbit", Rabbit.Type.WHITE}, {"black rabbit", Rabbit.Type.BLACK}, {"black and white rabbit", Rabbit.Type.BLACK_AND_WHITE}, {"brown rabbit", Rabbit.Type.BROWN}, {"gold rabbit", Rabbit.Type.GOLD}, {"salt and pepper rabbit", Rabbit.Type.SALT_AND_PEPPER}, {"killer rabbit", Rabbit.Type.THE_KILLER_BUNNY}});
    @Nullable
    private Rabbit.Type type = null;

    public RabbitData() {
    }

    public RabbitData(@Nullable Rabbit.Type type) {
        this.type = type;
        this.codeNameIndex = PATTERNS.getMatchedPattern(type, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.type = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Rabbit> entityClass, @Nullable Rabbit rabbit) {
        if (rabbit != null) {
            this.type = rabbit.getRabbitType();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.type, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Rabbit rabbit) {
        Rabbit.Type type = this.type;
        if (type == null) {
            type = CollectionUtils.getRandom(TYPES);
        }
        assert (type != null);
        rabbit.setRabbitType(type);
    }

    @Override
    protected boolean match(Rabbit rabbit) {
        return this.dataMatch(this.type, rabbit.getRabbitType());
    }

    @Override
    public Class<? extends Rabbit> getType() {
        return Rabbit.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new RabbitData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.type);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof RabbitData)) {
            return false;
        }
        RabbitData other = (RabbitData)entityData;
        return this.type == other.type;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof RabbitData)) {
            return false;
        }
        RabbitData other = (RabbitData)entityData;
        return this.dataMatch(this.type, other.type);
    }

    static {
        EntityData.register(RabbitData.class, "rabbit", Rabbit.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(Rabbit.Type.class, "Rabbit.Type");
    }
}

