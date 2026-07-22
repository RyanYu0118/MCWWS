/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Axolotl
 *  org.bukkit.entity.Axolotl$Variant
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
import org.bukkit.entity.Axolotl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AxolotlData
extends EntityData<Axolotl> {
    private static final Patterns<Axolotl.Variant> PATTERNS = new Patterns(new Object[][]{{"axolotl", null}, {"lucy axolotl", Axolotl.Variant.LUCY}, {"wild axolotl", Axolotl.Variant.WILD}, {"gold axolotl", Axolotl.Variant.GOLD}, {"cyan axolotl", Axolotl.Variant.CYAN}, {"blue axolotl", Axolotl.Variant.BLUE}});
    private static final Axolotl.Variant[] VARIANTS = Axolotl.Variant.values();
    @Nullable
    private Axolotl.Variant variant = null;

    public AxolotlData() {
    }

    public AxolotlData(@Nullable Axolotl.Variant variant) {
        this.variant = variant;
        this.codeNameIndex = PATTERNS.getMatchedPattern(variant, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.variant = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Axolotl> entityClass, @Nullable Axolotl axolotl) {
        if (axolotl != null) {
            this.variant = axolotl.getVariant();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.variant, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Axolotl axolotl) {
        Axolotl.Variant variant = this.variant;
        if (variant == null) {
            variant = CollectionUtils.getRandom(VARIANTS);
        }
        assert (variant != null);
        axolotl.setVariant(variant);
    }

    @Override
    protected boolean match(Axolotl axolotl) {
        return this.dataMatch(this.variant, axolotl.getVariant());
    }

    @Override
    public Class<? extends Axolotl> getType() {
        return Axolotl.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new AxolotlData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.variant);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof AxolotlData)) {
            return false;
        }
        AxolotlData other = (AxolotlData)entityData;
        return this.variant == other.variant;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof AxolotlData)) {
            return false;
        }
        AxolotlData other = (AxolotlData)entityData;
        return this.dataMatch(this.variant, other.variant);
    }

    static {
        EntityData.register(AxolotlData.class, "axolotl", Axolotl.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(Axolotl.Variant.class, "Axolotl.Variant");
    }
}

