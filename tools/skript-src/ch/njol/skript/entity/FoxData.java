/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Fox
 *  org.bukkit.entity.Fox$Type
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
import org.bukkit.entity.Fox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FoxData
extends EntityData<Fox> {
    private static final Patterns<Fox.Type> PATTERNS = new Patterns(new Object[][]{{"fox", null}, {"red fox", Fox.Type.RED}, {"snow fox", Fox.Type.SNOW}});
    private static final Fox.Type[] TYPES = Fox.Type.values();
    @Nullable
    private Fox.Type type = null;

    public FoxData() {
    }

    public FoxData(@Nullable Fox.Type type) {
        this.type = type;
        this.codeNameIndex = PATTERNS.getMatchedPattern(type, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.type = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Fox> entityClass, @Nullable Fox fox) {
        if (fox != null) {
            this.type = fox.getFoxType();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.type, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Fox fox) {
        Fox.Type type = this.type;
        if (type == null) {
            type = CollectionUtils.getRandom(TYPES);
        }
        assert (type != null);
        fox.setFoxType(type);
    }

    @Override
    protected boolean match(Fox fox) {
        return this.dataMatch(this.type, fox.getFoxType());
    }

    @Override
    public Class<? extends Fox> getType() {
        return Fox.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new FoxData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.type);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof FoxData)) {
            return false;
        }
        FoxData other = (FoxData)entityData;
        return this.type == other.type;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof FoxData)) {
            return false;
        }
        FoxData other = (FoxData)entityData;
        return this.dataMatch(this.type, other.type);
    }

    static {
        EntityData.register(FoxData.class, "fox", Fox.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(Fox.Type.class, "Fox.Type");
    }
}

