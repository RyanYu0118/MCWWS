/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
 *  org.bukkit.entity.Pig
 *  org.bukkit.entity.Pig$Variant
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.BukkitUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.Iterators;
import java.util.Objects;
import org.bukkit.entity.Pig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PigData
extends EntityData<Pig> {
    private static final boolean VARIANTS_ENABLED;
    private static final Object[] VARIANTS;
    private static final Patterns<Kleenean> PATTERNS;
    private Kleenean saddled = Kleenean.UNKNOWN;
    @Nullable
    private Object variant = null;

    public PigData() {
    }

    public PigData(@Nullable Kleenean saddled, @Nullable Object variant) {
        this.saddled = saddled != null ? saddled : Kleenean.UNKNOWN;
        this.variant = variant;
        this.codeNameIndex = PATTERNS.getMatchedPattern(this.saddled, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.saddled = PATTERNS.getInfo(matchedCodeName);
        if (VARIANTS_ENABLED && exprs[0] != null) {
            this.variant = exprs[0].getSingle();
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Pig> entityClass, @Nullable Pig pig) {
        if (pig != null) {
            this.saddled = Kleenean.get(pig.hasSaddle());
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.saddled, 0).orElse(0);
            if (VARIANTS_ENABLED) {
                this.variant = pig.getVariant();
            }
        }
        return true;
    }

    @Override
    public void set(Pig pig) {
        pig.setSaddle(this.saddled.isTrue());
        if (VARIANTS_ENABLED) {
            Object finalVariant;
            Object object = finalVariant = this.variant != null ? this.variant : CollectionUtils.getRandom(VARIANTS);
            assert (finalVariant != null);
            pig.setVariant((Pig.Variant)finalVariant);
        }
    }

    @Override
    protected boolean match(Pig pig) {
        if (!this.kleeneanMatch(this.saddled, pig.hasSaddle())) {
            return false;
        }
        return this.variant == null || this.variant == pig.getVariant();
    }

    @Override
    public Class<? extends Pig> getType() {
        return Pig.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new PigData();
    }

    @Override
    protected int hashCode_i() {
        return this.saddled.ordinal() + Objects.hashCode(this.variant);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof PigData)) {
            return false;
        }
        PigData other = (PigData)entityData;
        if (this.saddled != other.saddled) {
            return false;
        }
        return this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof PigData)) {
            return false;
        }
        PigData other = (PigData)entityData;
        if (!this.kleeneanMatch(this.saddled, other.saddled)) {
            return false;
        }
        return this.variant == null || this.variant == other.variant;
    }

    static {
        PATTERNS = new Patterns(new Object[][]{{"pig", Kleenean.UNKNOWN}, {"saddled pig", Kleenean.TRUE}, {"unsaddled pig", Kleenean.FALSE}});
        ClassInfo pigVariantClassInfo = BukkitUtils.getRegistryClassInfo("org.bukkit.entity.Pig$Variant", "PIG_VARIANT", "pigvariant", "pig variants");
        if (pigVariantClassInfo == null) {
            pigVariantClassInfo = new ClassInfo<PigVariantDummy>(PigVariantDummy.class, "pigvariant");
        }
        Classes.registerClass(pigVariantClassInfo.user("pig ?variants?").name("Pig Variant").description("Represents the variant of a pig entity.", "NOTE: Minecraft namespaces are supported, ex: 'minecraft:warm'.").since("2.12").requiredPlugins("Minecraft 1.21.5+").documentationId("PigVariant"));
        PigData.register(PigData.class, "pig", Pig.class, 0, PATTERNS.getPatterns());
        if (Skript.classExists("org.bukkit.entity.Pig$Variant")) {
            VARIANTS_ENABLED = true;
            VARIANTS = Iterators.toArray(Classes.getExactClassInfo(Pig.Variant.class).getSupplier().get(), Pig.Variant.class);
        } else {
            VARIANTS_ENABLED = false;
            VARIANTS = null;
        }
    }

    public static class PigVariantDummy {
    }
}

