/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
 *  org.bukkit.entity.Chicken
 *  org.bukkit.entity.Chicken$Variant
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
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.Iterators;
import java.util.Objects;
import org.bukkit.entity.Chicken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChickenData
extends EntityData<Chicken> {
    private static final boolean VARIANTS_ENABLED;
    private static final Object[] VARIANTS;
    @Nullable
    private Object variant = null;

    public ChickenData() {
    }

    public ChickenData(@Nullable Object variant) {
        this.variant = variant;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (VARIANTS_ENABLED && exprs[0] != null) {
            this.variant = exprs[0].getSingle();
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Chicken> entityClass, @Nullable Chicken chicken) {
        if (chicken != null && VARIANTS_ENABLED) {
            this.variant = chicken.getVariant();
        }
        return true;
    }

    @Override
    public void set(Chicken chicken) {
        if (VARIANTS_ENABLED) {
            Chicken.Variant variant = (Chicken.Variant)this.variant;
            if (variant == null) {
                variant = (Chicken.Variant)CollectionUtils.getRandom(VARIANTS);
            }
            assert (variant != null);
            chicken.setVariant(variant);
        }
    }

    @Override
    protected boolean match(Chicken chicken) {
        return this.variant == null || this.variant == chicken.getVariant();
    }

    @Override
    public Class<? extends Chicken> getType() {
        return Chicken.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new ChickenData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.variant);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof ChickenData)) {
            return false;
        }
        ChickenData other = (ChickenData)entityData;
        return this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof ChickenData)) {
            return false;
        }
        ChickenData other = (ChickenData)entityData;
        return this.dataMatch(this.variant, other.variant);
    }

    static {
        ClassInfo chickenVariantClassInfo = BukkitUtils.getRegistryClassInfo("org.bukkit.entity.Chicken$Variant", "CHICKEN_VARIANT", "chickenvariant", "chicken variants");
        if (chickenVariantClassInfo == null) {
            chickenVariantClassInfo = new ClassInfo<ChickenVariantDummy>(ChickenVariantDummy.class, "chickenvariant");
        }
        Classes.registerClass(chickenVariantClassInfo.user("chicken ?variants?").name("Chicken Variant").description("Represents the variant of a chicken entity.", "NOTE: Minecraft namespaces are supported, ex: 'minecraft:warm'.").since("2.12").requiredPlugins("Minecraft 1.21.5+").documentationId("ChickenVariant"));
        ChickenData.register(ChickenData.class, "chicken", Chicken.class, "chicken");
        if (Skript.classExists("org.bukkit.entity.Chicken$Variant")) {
            VARIANTS_ENABLED = true;
            VARIANTS = Iterators.toArray(Classes.getExactClassInfo(Chicken.Variant.class).getSupplier().get(), Chicken.Variant.class);
        } else {
            VARIANTS_ENABLED = false;
            VARIANTS = null;
        }
    }

    public static class ChickenVariantDummy {
    }
}

