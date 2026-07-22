/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Villager$Profession
 *  org.bukkit.entity.ZombieVillager
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZombieVillagerData
extends EntityData<ZombieVillager> {
    private static final Villager.Profession[] PROFESSIONS;
    private static final Patterns<Villager.Profession> PATTERNS;
    @Nullable
    private Villager.Profession profession = null;

    public ZombieVillagerData() {
    }

    public ZombieVillagerData(@Nullable Villager.Profession profession) {
        this.profession = profession;
        this.codeNameIndex = PATTERNS.getMatchedPattern(profession, 0).orElse(0);
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.profession = PATTERNS.getInfo(matchedCodeName);
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends ZombieVillager> entityClass, @Nullable ZombieVillager zombieVillager) {
        if (zombieVillager != null) {
            this.profession = zombieVillager.getVillagerProfession();
            this.codeNameIndex = PATTERNS.getMatchedPattern(this.profession, 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(ZombieVillager zombieVillager) {
        Villager.Profession profession = this.profession;
        if (profession == null) {
            profession = CollectionUtils.getRandom(PROFESSIONS);
        }
        assert (profession != null);
        zombieVillager.setVillagerProfession(profession);
    }

    @Override
    protected boolean match(ZombieVillager zombieVillager) {
        return this.dataMatch(this.profession, zombieVillager.getVillagerProfession());
    }

    @Override
    public Class<? extends ZombieVillager> getType() {
        return ZombieVillager.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new ZombieVillagerData();
    }

    @Override
    protected int hashCode_i() {
        return Objects.hashCode(this.profession);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof ZombieVillagerData)) {
            return false;
        }
        ZombieVillagerData other = (ZombieVillagerData)entityData;
        return this.profession == other.profession;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof ZombieVillagerData)) {
            return false;
        }
        ZombieVillagerData other = (ZombieVillagerData)entityData;
        return this.dataMatch(this.profession, other.profession);
    }

    static {
        PATTERNS = new Patterns(new Object[][]{{"zombie villager", null}, {"zombie normal", Villager.Profession.NONE}, {"zombie armorer", Villager.Profession.ARMORER}, {"zombie butcher", Villager.Profession.BUTCHER}, {"zombie cartographer", Villager.Profession.CARTOGRAPHER}, {"zombie cleric", Villager.Profession.CLERIC}, {"zombie farmer", Villager.Profession.FARMER}, {"zombie fisherman", Villager.Profession.FISHERMAN}, {"zombie fletcher", Villager.Profession.FLETCHER}, {"zombie leatherworker", Villager.Profession.LEATHERWORKER}, {"zombie librarian", Villager.Profession.LIBRARIAN}, {"zombie mason", Villager.Profession.MASON}, {"zombie nitwit", Villager.Profession.NITWIT}, {"zombie shepherd", Villager.Profession.SHEPHERD}, {"zombie toolsmith", Villager.Profession.TOOLSMITH}, {"zombie weaponsmith", Villager.Profession.WEAPONSMITH}});
        EntityData.register(ZombieVillagerData.class, "zombie villager", ZombieVillager.class, 0, PATTERNS.getPatterns());
        PROFESSIONS = new Villager.Profession[]{Villager.Profession.NONE, Villager.Profession.ARMORER, Villager.Profession.BUTCHER, Villager.Profession.CARTOGRAPHER, Villager.Profession.CLERIC, Villager.Profession.FARMER, Villager.Profession.FISHERMAN, Villager.Profession.FLETCHER, Villager.Profession.LEATHERWORKER, Villager.Profession.LIBRARIAN, Villager.Profession.MASON, Villager.Profession.NITWIT, Villager.Profession.SHEPHERD, Villager.Profession.TOOLSMITH, Villager.Profession.WEAPONSMITH};
    }
}

