/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.ExperienceOrb
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.ArgsMessage;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XpOrbData
extends EntityData<ExperienceOrb> {
    private static final ArgsMessage FORMAT = new ArgsMessage("entities.xp-orb.format");
    private int xp = -1;

    public XpOrbData() {
    }

    public XpOrbData(int xp) {
        this.xp = xp;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends ExperienceOrb> entityClass, @Nullable ExperienceOrb orb) {
        if (orb != null) {
            this.xp = orb.getExperience();
        }
        return true;
    }

    @Override
    public void set(ExperienceOrb orb) {
        if (this.xp != -1) {
            orb.setExperience(this.xp + orb.getExperience());
        }
    }

    @Override
    protected boolean match(ExperienceOrb orb) {
        return this.xp == -1 || orb.getExperience() == this.xp;
    }

    @Override
    public Class<? extends ExperienceOrb> getType() {
        return ExperienceOrb.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new XpOrbData();
    }

    @Override
    protected int hashCode_i() {
        return this.xp;
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof XpOrbData)) {
            return false;
        }
        XpOrbData other = (XpOrbData)entityData;
        return this.xp == other.xp;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof XpOrbData)) {
            return false;
        }
        XpOrbData other = (XpOrbData)entityData;
        return this.xp == -1 || other.xp == this.xp;
    }

    @Override
    @Nullable
    public ExperienceOrb spawn(Location loc, @Nullable Consumer<ExperienceOrb> consumer) {
        ExperienceOrb orb = super.spawn(loc, consumer);
        if (orb == null) {
            return null;
        }
        if (this.xp == -1) {
            orb.setExperience(1 + orb.getExperience());
        }
        return orb;
    }

    @Override
    public String toString(int flags) {
        return this.xp == -1 ? super.toString(flags) : FORMAT.toString(super.toString(flags), this.xp);
    }

    public int getExperience() {
        return this.xp == -1 ? 1 : this.xp;
    }

    public int getInternalExperience() {
        return this.xp;
    }

    static {
        EntityData.register(XpOrbData.class, "xporb", ExperienceOrb.class, "xp-orb");
    }
}

