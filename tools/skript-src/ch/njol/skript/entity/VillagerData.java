/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.Villager$Profession
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
import java.util.Collections;
import java.util.Objects;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerData
extends EntityData<Villager> {
    private static final Villager.Profession[] PROFESSIONS;
    private static final Patterns<Villager.Profession> PATTERNS;
    @Nullable
    private Villager.Profession profession = null;

    public VillagerData() {
    }

    public VillagerData(@Nullable Villager.Profession profession) {
        this.profession = profession;
        this.codeNameIndex = PATTERNS.getMatchedPattern(profession, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.profession = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Villager> villagerClass, @Nullable Villager villager) {
        if (villager != null) {
            this.profession = villager.getProfession();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.profession, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Villager villager) {
        Villager.Profession profession = this.profession;
        if (profession == null) {
            profession = CollectionUtils.getRandom(PROFESSIONS);
        }
        assert (profession != null);
        villager.setProfession(profession);
        if (profession == Villager.Profession.NITWIT) {
            villager.setRecipes(Collections.emptyList());
        }
    }

    @Override
    protected boolean match(Villager villager) {
        return this.dataMatch(this.profession, villager.getProfession());
    }

    @Override
    public Class<? extends Villager> getType() {
        return Villager.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new VillagerData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.profession);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof VillagerData)) {
            return false;
        }
        VillagerData other = (VillagerData)entityData;
        return this.profession == other.profession;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof VillagerData)) {
            return false;
        }
        VillagerData other = (VillagerData)entityData;
        return this.dataMatch(this.profession, other.profession);
    }

    static {
        PATTERNS = new Patterns(new Object[][]{{"villager", null}, {"unemployed", Villager.Profession.NONE}, {"armorer", Villager.Profession.ARMORER}, {"butcher", Villager.Profession.BUTCHER}, {"cartographer", Villager.Profession.CARTOGRAPHER}, {"cleric", Villager.Profession.CLERIC}, {"farmer", Villager.Profession.FARMER}, {"fisherman", Villager.Profession.FISHERMAN}, {"fletcher", Villager.Profession.FLETCHER}, {"leatherworker", Villager.Profession.LEATHERWORKER}, {"librarian", Villager.Profession.LIBRARIAN}, {"mason", Villager.Profession.MASON}, {"nitwit", Villager.Profession.NITWIT}, {"shepherd", Villager.Profession.SHEPHERD}, {"toolsmith", Villager.Profession.TOOLSMITH}, {"weaponsmith", Villager.Profession.WEAPONSMITH}});
        Variables.yggdrasil.registerSingleClass(Villager.Profession.class, "Villager.Profession");
        EntityData.register(VillagerData.class, "villager", Villager.class, 0, PATTERNS.getPatterns());
        PROFESSIONS = new Villager.Profession[]{Villager.Profession.NONE, Villager.Profession.ARMORER, Villager.Profession.BUTCHER, Villager.Profession.CARTOGRAPHER, Villager.Profession.CLERIC, Villager.Profession.FARMER, Villager.Profession.FISHERMAN, Villager.Profession.FLETCHER, Villager.Profession.LEATHERWORKER, Villager.Profession.LIBRARIAN, Villager.Profession.MASON, Villager.Profession.NITWIT, Villager.Profession.SHEPHERD, Villager.Profession.TOOLSMITH, Villager.Profession.WEAPONSMITH};
    }
}

