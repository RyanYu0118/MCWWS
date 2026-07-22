/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Sheep
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.SkriptColor;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Arrays;
import org.bukkit.entity.Sheep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SheepData
extends EntityData<Sheep> {
    private static final Patterns<Kleenean> PATTERNS = new Patterns(new Object[][]{{"sheep", Kleenean.UNKNOWN}, {"sheared sheep", Kleenean.TRUE}, {"unsheared sheep", Kleenean.FALSE}});
    private Color @Nullable [] colors = null;
    private Kleenean sheared = Kleenean.UNKNOWN;
    private Adjective @Nullable [] adjectives = null;

    public SheepData() {
    }

    public SheepData(@Nullable Kleenean sheared, Color @Nullable [] colors) {
        this.sheared = sheared != null ? sheared : Kleenean.UNKNOWN;
        this.colors = colors;
        this.codeNameIndex = PATTERNS.getMatchedPattern(this.sheared, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.sheared = PATTERNS.getInfo(matchedCodeName);
        if (exprs[0] != null) {
            this.colors = (Color[])exprs[0].getAll();
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Sheep> entityClass, @Nullable Sheep sheep) {
        if (sheep != null) {
            this.sheared = Kleenean.get(sheep.isSheared());
            this.colors = CollectionUtils.array(SkriptColor.fromDyeColor(sheep.getColor()));
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.sheared, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Sheep sheep) {
        if (this.colors != null) {
            Color color = CollectionUtils.getRandom(this.colors);
            assert (color != null);
            sheep.setColor(color.asDyeColor());
        }
        sheep.setSheared(this.sheared.isTrue());
    }

    @Override
    public boolean match(Sheep sheep) {
        if (!this.kleeneanMatch(this.sheared, sheep.isSheared())) {
            return false;
        }
        return this.colors == null || SimpleExpression.check(this.colors, c -> sheep.getColor() == c.asDyeColor(), false, false);
    }

    @Override
    public Class<Sheep> getType() {
        return Sheep.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new SheepData();
    }

    @Override
    protected int hashCode_i() {
        int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.colors);
        result = prime * result + this.sheared.hashCode();
        return result;
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof SheepData)) {
            return false;
        }
        SheepData other = (SheepData)entityData;
        if (!Arrays.equals(this.colors, other.colors)) {
            return false;
        }
        return this.sheared == other.sheared;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof SheepData)) {
            return false;
        }
        SheepData other = (SheepData)entityData;
        if (!this.kleeneanMatch(this.sheared, other.sheared)) {
            return false;
        }
        return this.colors == null || CollectionUtils.isSubset(this.colors, other.colors);
    }

    @Override
    public String toString(int flags) {
        Color[] colors = this.colors;
        if (colors == null) {
            return super.toString(flags);
        }
        Adjective[] adjectives = this.adjectives;
        if (adjectives == null) {
            adjectives = new Adjective[colors.length];
            this.adjectives = adjectives;
            for (int i = 0; i < colors.length; ++i) {
                Color color = colors[i];
                if (!(color instanceof SkriptColor)) continue;
                SkriptColor skriptColor = (SkriptColor)color;
                adjectives[i] = skriptColor.getAdjective();
            }
        }
        Noun name = this.getName();
        Adjective age = this.getAgeAdjective();
        return name.getArticleWithSpace(flags) + (String)(age == null ? "" : age.toString(name.getGender(), flags) + " ") + Adjective.toString(adjectives, name.getGender(), flags, false) + " " + name.toString(flags & 0xFFFFFFF9);
    }

    static {
        EntityData.register(SheepData.class, "sheep", Sheep.class, 0, PATTERNS.getPatterns());
    }
}

