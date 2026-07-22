/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Goat
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Goat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoatData
extends EntityData<Goat> {
    private static final Patterns<Kleenean> PATTERNS = new Patterns(new Object[][]{{"goat", Kleenean.UNKNOWN}, {"screaming goat", Kleenean.TRUE}, {"quiet goat", Kleenean.FALSE}});
    private Kleenean screaming = Kleenean.UNKNOWN;

    public GoatData() {
    }

    public GoatData(@Nullable Kleenean screaming) {
        this.screaming = screaming != null ? screaming : Kleenean.UNKNOWN;
        this.codeNameIndex = PATTERNS.getMatchedPattern(this.screaming, 0).orElseThrow();
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.screaming = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Goat> entityClass, @Nullable Goat goat) {
        if (goat != null) {
            this.screaming = Kleenean.get(goat.isScreaming());
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.screaming, 0).orElseThrow();
        }
        return true;
    }

    @Override
    public void set(Goat goat) {
        goat.setScreaming(this.screaming.isTrue());
    }

    @Override
    protected boolean match(Goat goat) {
        return this.kleeneanMatch(this.screaming, goat.isScreaming());
    }

    @Override
    public Class<? extends Goat> getType() {
        return Goat.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new GoatData();
    }

    @Override
    protected int hashCode_i() {
        return this.screaming.hashCode();
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof GoatData)) {
            return false;
        }
        GoatData other = (GoatData)entityData;
        return this.screaming == other.screaming;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof GoatData)) {
            return false;
        }
        GoatData other = (GoatData)entityData;
        return this.kleeneanMatch(this.screaming, other.screaming);
    }

    static {
        EntityData.register(GoatData.class, "goat", Goat.class, 0, PATTERNS.getPatterns());
    }
}

