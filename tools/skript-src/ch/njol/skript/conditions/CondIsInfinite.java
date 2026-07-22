/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.potion.PotionEffect
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.util.Timespan;
import org.bukkit.potion.PotionEffect;

@Name(value="Is Infinite")
@Description(value={"Checks whether potion effects or timespans are infinite."})
@Example.Examples(value={@Example(value="all of the active potion effects of the player are infinite"), @Example(value="if timespan argument is infinite:")})
@Since(value={"2.7"})
public class CondIsInfinite
extends PropertyCondition<Object> {
    @Override
    public boolean check(Object object) {
        if (object instanceof PotionEffect) {
            PotionEffect potionEffect = (PotionEffect)object;
            return potionEffect.isInfinite();
        }
        if (object instanceof Timespan) {
            Timespan timespan = (Timespan)object;
            return timespan.isInfinite();
        }
        return false;
    }

    @Override
    public Condition simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return "infinite";
    }

    static {
        CondIsInfinite.register(CondIsInfinite.class, "infinite", "potioneffects/timespans");
    }
}

