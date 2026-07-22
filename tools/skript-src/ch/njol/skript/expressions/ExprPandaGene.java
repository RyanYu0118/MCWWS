/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Panda
 *  org.bukkit.entity.Panda$Gene
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Panda;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Panda Gene")
@Description(value={"The main or hidden gene of a panda."})
@Example(value="if the main gene of last spawned panda is lazy:\n\tset the main gene of last spawned panda to playful\n")
@Since(value={"2.11"})
public class ExprPandaGene
extends SimplePropertyExpression<LivingEntity, Panda.Gene> {
    private boolean mainGene;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.mainGene = parseResult.hasTag("main");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Panda.Gene convert(LivingEntity entity) {
        if (!(entity instanceof Panda)) {
            return null;
        }
        Panda panda = (Panda)entity;
        return this.mainGene ? panda.getMainGene() : panda.getHiddenGene();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Panda.Gene.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        Panda.Gene gene = (Panda.Gene)delta[0];
        for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (!(entity instanceof Panda)) continue;
            Panda panda = (Panda)entity;
            if (this.mainGene) {
                panda.setMainGene(gene);
                continue;
            }
            panda.setHiddenGene(gene);
        }
    }

    @Override
    public Class<? extends Panda.Gene> getReturnType() {
        return Panda.Gene.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.mainGene ? "main" : "hidden") + "gene";
    }

    static {
        ExprPandaGene.register(ExprPandaGene.class, Panda.Gene.class, "(:main|hidden) gene[s]", "livingentities");
    }
}

