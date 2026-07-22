/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Parrot
 *  org.bukkit.entity.Parrot$Variant
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
import org.bukkit.entity.Parrot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParrotData
extends EntityData<Parrot> {
    private static final Parrot.Variant[] VARIANTS = Parrot.Variant.values();
    private static final Patterns<Parrot.Variant> PATTERNS = new Patterns(new Object[][]{{"parrot", null}, {"red parrot", Parrot.Variant.RED}, {"blue parrot", Parrot.Variant.BLUE}, {"green parrot", Parrot.Variant.GREEN}, {"cyan parrot", Parrot.Variant.CYAN}, {"gray parrot", Parrot.Variant.GRAY}});
    @Nullable
    private Parrot.Variant variant = null;

    public ParrotData() {
    }

    public ParrotData(@Nullable Parrot.Variant variant) {
        this.variant = variant;
        this.codeNameIndex = PATTERNS.getMatchedPattern(variant, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.variant = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Parrot> entityClass, @Nullable Parrot parrot) {
        if (parrot != null) {
            this.variant = parrot.getVariant();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.variant, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Parrot parrot) {
        Parrot.Variant variant = this.variant;
        if (variant == null) {
            variant = CollectionUtils.getRandom(VARIANTS);
        }
        assert (variant != null);
        parrot.setVariant(variant);
    }

    @Override
    protected boolean match(Parrot parrot) {
        return this.dataMatch(this.variant, parrot.getVariant());
    }

    @Override
    public Class<? extends Parrot> getType() {
        return Parrot.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new ParrotData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.variant);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof ParrotData)) {
            return false;
        }
        ParrotData other = (ParrotData)entityData;
        return this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof ParrotData)) {
            return false;
        }
        ParrotData other = (ParrotData)entityData;
        return this.dataMatch(this.variant, other.variant);
    }

    static {
        EntityData.register(ParrotData.class, "parrot", Parrot.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(Parrot.Variant.class, "Parrot.Variant");
    }
}

