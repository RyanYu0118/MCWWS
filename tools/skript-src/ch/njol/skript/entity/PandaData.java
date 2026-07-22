/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Panda
 *  org.bukkit.entity.Panda$Gene
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.classes.EnumClassInfo;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.entity.Panda;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PandaData
extends EntityData<Panda> {
    private static final Panda.Gene[] GENES = Panda.Gene.values();
    @Nullable
    private Panda.Gene mainGene = null;
    @Nullable
    private Panda.Gene hiddenGene = null;

    public PandaData() {
    }

    public PandaData(@Nullable Panda.Gene mainGene, @Nullable Panda.Gene hiddenGene) {
        this.mainGene = mainGene;
        this.hiddenGene = hiddenGene;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (exprs[0] != null) {
            this.mainGene = (Panda.Gene)exprs[0].getSingle();
            if (exprs[1] != null) {
                this.hiddenGene = (Panda.Gene)exprs[1].getSingle();
            }
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Panda> entityClass, @Nullable Panda panda) {
        if (panda != null) {
            this.mainGene = panda.getMainGene();
            this.hiddenGene = panda.getHiddenGene();
        }
        return true;
    }

    @Override
    public void set(Panda panda) {
        Panda.Gene gene = this.mainGene;
        if (gene == null) {
            gene = CollectionUtils.getRandom(GENES);
        }
        assert (gene != null);
        panda.setMainGene(gene);
        panda.setHiddenGene(this.hiddenGene != null ? this.hiddenGene : gene);
    }

    @Override
    protected boolean match(Panda panda) {
        if (!this.dataMatch(this.mainGene, panda.getMainGene())) {
            return false;
        }
        return this.dataMatch(this.hiddenGene, panda.getHiddenGene());
    }

    @Override
    public Class<? extends Panda> getType() {
        return Panda.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new PandaData();
    }

    @Override
    protected int hashCode_i() {
        int prime = 7;
        int result = 0;
        result = result * prime + Objects.hashCode(this.mainGene);
        result = result * prime + Objects.hashCode(this.hiddenGene);
        return result;
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof PandaData)) {
            return false;
        }
        PandaData other = (PandaData)entityData;
        return other.mainGene == this.mainGene && other.hiddenGene == this.hiddenGene;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof PandaData)) {
            return false;
        }
        PandaData other = (PandaData)entityData;
        if (!this.dataMatch(this.mainGene, other.mainGene)) {
            return false;
        }
        return this.dataMatch(this.hiddenGene, other.hiddenGene);
    }

    @Override
    public String toString(int flags) {
        StringBuilder builder = new StringBuilder();
        if (this.mainGene != null) {
            builder.append(Language.getList("genes." + this.mainGene.name())[0]).append(" ");
        }
        if (this.hiddenGene != null && this.hiddenGene != this.mainGene) {
            builder.append(Language.getList("genes." + this.hiddenGene.name())[0]).append(" ");
        }
        builder.append(Language.get("panda"));
        return builder.toString();
    }

    static {
        EntityData.register(PandaData.class, "panda", Panda.class, "panda");
        Classes.registerClass(new EnumClassInfo<Panda.Gene>(Panda.Gene.class, "gene", "genes").user("(panda )?genes?").name("Gene").description("Represents a Panda's main or hidden gene. See <a href='https://minecraft.wiki/w/Panda#Genetics'>genetics</a> for more info.").since("2.4").requiredPlugins("Minecraft 1.14 or newer"));
    }
}

