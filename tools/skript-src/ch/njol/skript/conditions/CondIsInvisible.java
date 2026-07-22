/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;

@Name(value="Is Invisible")
@Description(value={"Checks whether a living entity is invisible."})
@Example(value="target entity is invisible")
@Since(value={"2.7"})
public class CondIsInvisible
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(matchedPattern == 1 ^ parseResult.hasTag("visible"));
        return true;
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        return livingEntity.isInvisible();
    }

    @Override
    protected String getPropertyName() {
        return "invisible";
    }

    static {
        CondIsInvisible.register(CondIsInvisible.class, "(invisible|:visible)", "livingentities");
    }
}

