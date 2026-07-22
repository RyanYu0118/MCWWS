/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.DyeColor
 *  org.bukkit.entity.TropicalFish
 *  org.bukkit.entity.TropicalFish$Pattern
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.DyeColor;
import org.bukkit.entity.TropicalFish;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TropicalFishData
extends EntityData<TropicalFish> {
    private static final Patterns<TropicalFish.Pattern> PATTERNS = new Patterns(new Object[][]{{"tropical fish", null}, {"kob", TropicalFish.Pattern.KOB}, {"sunstreak", TropicalFish.Pattern.SUNSTREAK}, {"snooper", TropicalFish.Pattern.SNOOPER}, {"dasher", TropicalFish.Pattern.DASHER}, {"brinely", TropicalFish.Pattern.BRINELY}, {"spotty", TropicalFish.Pattern.SPOTTY}, {"flopper", TropicalFish.Pattern.FLOPPER}, {"stripey", TropicalFish.Pattern.STRIPEY}, {"glitter", TropicalFish.Pattern.GLITTER}, {"blockfish", TropicalFish.Pattern.BLOCKFISH}, {"betty", TropicalFish.Pattern.BETTY}, {"clayfish", TropicalFish.Pattern.CLAYFISH}});
    private static final TropicalFish.Pattern[] FISH_PATTERNS = TropicalFish.Pattern.values();
    @Nullable
    private DyeColor bodyColor = null;
    @Nullable
    private DyeColor patternColor = null;
    @Nullable
    private TropicalFish.Pattern fishPattern = null;

    public TropicalFishData() {
    }

    public TropicalFishData(@Nullable TropicalFish.Pattern fishPattern, @Nullable DyeColor bodyColor, @Nullable DyeColor patternColor) {
        this.fishPattern = fishPattern;
        this.bodyColor = bodyColor;
        this.patternColor = patternColor;
        this.codeNameIndex = PATTERNS.getMatchedPattern(fishPattern, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.fishPattern = PATTERNS.getInfo(matchedCodeName);
        if (exprs.length == 0) {
            return true;
        }
        if (matchedPattern == 0) {
            if (exprs[0] != null) {
                this.bodyColor = ((Color)exprs[0].getSingle()).asDyeColor();
                if (exprs[1] != null) {
                    this.patternColor = ((Color)exprs[1].getSingle()).asDyeColor();
                }
            }
        } else if (exprs[0] != null) {
            this.patternColor = this.bodyColor = ((Color)exprs[0].getSingle()).asDyeColor();
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends TropicalFish> entityClass, @Nullable TropicalFish tropicalFish) {
        if (tropicalFish != null) {
            this.bodyColor = tropicalFish.getBodyColor();
            this.patternColor = tropicalFish.getPatternColor();
            this.fishPattern = tropicalFish.getPattern();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.fishPattern, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(TropicalFish tropicalFish) {
        TropicalFish.Pattern fishPattern = this.fishPattern;
        if (fishPattern == null) {
            fishPattern = CollectionUtils.getRandom(FISH_PATTERNS);
        }
        assert (fishPattern != null);
        tropicalFish.setPattern(fishPattern);
        if (this.bodyColor != null) {
            tropicalFish.setBodyColor(this.bodyColor);
        }
        if (this.patternColor != null) {
            tropicalFish.setPatternColor(this.patternColor);
        }
    }

    @Override
    protected boolean match(TropicalFish tropicalFish) {
        if (!this.dataMatch(this.bodyColor, tropicalFish.getBodyColor())) {
            return false;
        }
        if (!this.dataMatch(this.patternColor, tropicalFish.getPatternColor())) {
            return false;
        }
        return this.dataMatch(this.fishPattern, tropicalFish.getPattern());
    }

    @Override
    public Class<? extends TropicalFish> getType() {
        return TropicalFish.class;
    }

    @Override
    @NotNull
    public EntityData<TropicalFish> getSuperType() {
        return new TropicalFishData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hash(this.fishPattern, this.bodyColor, this.patternColor);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof TropicalFishData)) {
            return false;
        }
        TropicalFishData other = (TropicalFishData)entityData;
        return this.fishPattern == other.fishPattern && this.bodyColor == other.bodyColor && this.patternColor == other.patternColor;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof TropicalFishData)) {
            return false;
        }
        TropicalFishData other = (TropicalFishData)entityData;
        if (!this.dataMatch(this.bodyColor, other.bodyColor)) {
            return false;
        }
        if (!this.dataMatch(this.patternColor, other.patternColor)) {
            return false;
        }
        return this.dataMatch(this.fishPattern, other.fishPattern);
    }

    static {
        TropicalFishData.register(TropicalFishData.class, "tropical fish", TropicalFish.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(TropicalFish.Pattern.class, "TropicalFish.Pattern");
    }
}

