/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Creeper
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;

@Name(value="Ignition Process")
@Description(value={"Checks if a creeper is going to explode."})
@Example(value="if the last spawned creeper is going to explode:\n\tloop all players in radius 3 of the last spawned creeper\n\t\tsend \"RUN!!!\" to the loop-player\n")
@Since(value={"2.5"})
public class CondIgnitionProcess
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(parseResult.mark == 1);
        return true;
    }

    @Override
    public boolean check(LivingEntity entity) {
        Creeper creeper;
        return entity instanceof Creeper && (creeper = (Creeper)entity).isIgnited();
    }

    @Override
    protected String getPropertyName() {
        return "going to explode";
    }

    static {
        if (Skript.methodExists(Creeper.class, "isIgnited", new Class[0])) {
            Skript.registerCondition(CondIgnitionProcess.class, "[creeper[s]] %livingentities% ((is|are)|1\u00a6(isn't|is not|aren't|are not)) going to explode", "[creeper[s]] %livingentities% ((is|are)|1\u00a6(isn't|is not|aren't|are not)) in the (ignition|explosion) process", "creeper[s] %livingentities% ((is|are)|1\u00a6(isn't|is not|aren't|are not)) ignited");
        }
    }
}

