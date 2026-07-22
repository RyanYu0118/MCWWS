/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;

@Name(value="Is Jumping")
@Description(value={"Checks whether a living entity is jumping. This condition does not work on players."})
@Example(value="on spawn of zombie:\n\twhile event-entity is not jumping:\n\t\twait 5 ticks\n\tpush event-entity upwards\n")
@Since(value={"2.8.0"})
public class CondIsJumping
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (HumanEntity.class.isAssignableFrom(exprs[0].getReturnType())) {
            Skript.error("The 'is jumping' condition only works on mobs.");
            return false;
        }
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        return livingEntity.isJumping();
    }

    @Override
    protected String getPropertyName() {
        return "jumping";
    }

    static {
        if (Skript.methodExists(LivingEntity.class, "isJumping", new Class[0])) {
            CondIsJumping.register(CondIsJumping.class, "jumping", "livingentities");
        }
    }
}

