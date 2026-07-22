/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Salmon
 *  org.bukkit.entity.Salmon$Variant
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.entity.Salmon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SalmonData
extends EntityData<Salmon> {
    private static final boolean SUPPORT_SALMON_VARIANTS = Skript.classExists("org.bukkit.entity.Salmon$Variant");
    private static final Object[] VARIANTS;
    private static final Patterns<Object> PATTERNS;
    @Nullable
    private Object variant = null;

    public SalmonData() {
    }

    public SalmonData(@Nullable Object variant) {
        this.variant = variant;
        this.codeNameIndex = PATTERNS.getMatchedPattern(variant, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.variant = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Salmon> entityClass, @Nullable Salmon salmon) {
        if (salmon != null && SUPPORT_SALMON_VARIANTS) {
            this.variant = salmon.getVariant();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.variant, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Salmon entity) {
        if (SUPPORT_SALMON_VARIANTS) {
            Salmon.Variant variant = (Salmon.Variant)this.variant;
            if (variant == null) {
                variant = (Salmon.Variant)CollectionUtils.getRandom(VARIANTS);
            }
            assert (variant != null);
            entity.setVariant(variant);
        }
    }

    @Override
    protected boolean match(Salmon entity) {
        return this.variant == null || this.variant == entity.getVariant();
    }

    @Override
    public Class<? extends Salmon> getType() {
        return Salmon.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new SalmonData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.variant);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof SalmonData)) {
            return false;
        }
        SalmonData other = (SalmonData)entityData;
        return this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof SalmonData)) {
            return false;
        }
        SalmonData other = (SalmonData)entityData;
        return this.dataMatch(this.variant, other.variant);
    }

    static {
        if (SUPPORT_SALMON_VARIANTS) {
            VARIANTS = Salmon.Variant.values();
            PATTERNS = new Patterns(new Object[][]{{"salmon", null}, {"any salmon", null}, {"small salmon", Salmon.Variant.SMALL}, {"medium salmon", Salmon.Variant.MEDIUM}, {"large salmon", Salmon.Variant.LARGE}});
            Variables.yggdrasil.registerSingleClass(Salmon.Variant.class, "Salmon.Variant");
        } else {
            VARIANTS = null;
            PATTERNS = new Patterns(new Object[][]{{"salmon", null}});
        }
        EntityData.register(SalmonData.class, "salmon", Salmon.class, 0, PATTERNS.getPatterns());
    }
}

