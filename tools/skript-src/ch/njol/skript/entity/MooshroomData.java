/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.MushroomCow
 *  org.bukkit.entity.MushroomCow$Variant
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
import org.bukkit.entity.MushroomCow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MooshroomData
extends EntityData<MushroomCow> {
    private static final Patterns<MushroomCow.Variant> PATTERNS = new Patterns(new Object[][]{{"mooshroom", null}, {"red mooshroom", MushroomCow.Variant.RED}, {"brown mooshroom", MushroomCow.Variant.BROWN}});
    private static final MushroomCow.Variant[] VARIANTS = MushroomCow.Variant.values();
    @Nullable
    private MushroomCow.Variant variant = null;

    public MooshroomData() {
    }

    public MooshroomData(@Nullable MushroomCow.Variant variant) {
        this.variant = variant;
        this.codeNameIndex = PATTERNS.getMatchedPattern(variant, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.variant = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends MushroomCow> entityClass, @Nullable MushroomCow mushroomCow) {
        if (mushroomCow != null) {
            this.variant = mushroomCow.getVariant();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.variant, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(MushroomCow mushroomCow) {
        MushroomCow.Variant variant = this.variant;
        if (variant == null) {
            variant = CollectionUtils.getRandom(VARIANTS);
        }
        assert (variant != null);
        mushroomCow.setVariant(variant);
    }

    @Override
    protected boolean match(MushroomCow mushroomCow) {
        return this.dataMatch(this.variant, mushroomCow.getVariant());
    }

    @Override
    public Class<? extends MushroomCow> getType() {
        return MushroomCow.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new MooshroomData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.variant);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof MooshroomData)) {
            return false;
        }
        MooshroomData other = (MooshroomData)entityData;
        return this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof MooshroomData)) {
            return false;
        }
        MooshroomData other = (MooshroomData)entityData;
        return this.dataMatch(this.variant, other.variant);
    }

    static {
        EntityData.register(MooshroomData.class, "mooshroom", MushroomCow.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(MushroomCow.Variant.class, "MushroomCow.Variant");
    }
}

