/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Frog
 *  org.bukkit.entity.Frog$Variant
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.bukkitutil.BukkitUtils;
import ch.njol.skript.classes.registry.RegistryClassInfo;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Patterns;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.entity.Frog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrogData
extends EntityData<Frog> {
    private static final Patterns<Frog.Variant> PATTERNS = new Patterns(new Object[][]{{"frog", null}, {"temperate frog", Frog.Variant.TEMPERATE}, {"warm frog", Frog.Variant.WARM}, {"cold frog", Frog.Variant.COLD}});
    private static final Frog.Variant[] VARIANTS;
    @Nullable
    private Frog.Variant variant = null;

    public FrogData() {
    }

    public FrogData(@Nullable Frog.Variant variant) {
        this.variant = variant;
        this.codeNameIndex = PATTERNS.getMatchedPattern(variant, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.variant = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Frog> entityClass, @Nullable Frog frog) {
        if (frog != null) {
            this.variant = frog.getVariant();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.variant, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Frog frog) {
        Frog.Variant variant = this.variant;
        if (variant == null) {
            variant = CollectionUtils.getRandom(VARIANTS);
        }
        assert (variant != null);
        frog.setVariant(variant);
    }

    @Override
    protected boolean match(Frog frog) {
        return this.dataMatch(this.variant, frog.getVariant());
    }

    @Override
    public Class<? extends Frog> getType() {
        return Frog.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new FrogData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.variant);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof FrogData)) {
            return false;
        }
        FrogData other = (FrogData)entityData;
        return this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof FrogData)) {
            return false;
        }
        FrogData other = (FrogData)entityData;
        return this.dataMatch(this.variant, other.variant);
    }

    static {
        EntityData.register(FrogData.class, "frog", Frog.class, 0, PATTERNS.getPatterns());
        VARIANTS = new Frog.Variant[]{Frog.Variant.TEMPERATE, Frog.Variant.WARM, Frog.Variant.COLD};
        RegistryClassInfo<?> frogVariantClassInfo = BukkitUtils.getRegistryClassInfo("org.bukkit.entity.Frog$Variant", "FROG_VARIANT", "frogvariant", "frog variants");
        assert (frogVariantClassInfo != null);
        Classes.registerClass(frogVariantClassInfo.user("frog ?variants?").name("Frog Variant").description("Represents the variant of a frog entity.", "NOTE: Minecraft namespaces are supported, ex: 'minecraft:warm'.").since("2.13").documentationId("FrogVariant"));
    }
}

