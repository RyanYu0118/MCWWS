/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.util.Vector
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
import org.bukkit.util.Vector;

@Name(value="Is Normalized")
@Description(value={"Checks whether a vector is normalized i.e. length of 1"})
@Example(value="vector of player's location is normalized")
@Since(value={"2.5.1"})
public class CondIsVectorNormalized
extends PropertyCondition<Vector> {
    @Override
    public boolean check(Vector vector) {
        return vector.isNormalized();
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
        return "normalized";
    }

    static {
        CondIsVectorNormalized.register(CondIsVectorNormalized.class, "normalized", "vectors");
    }
}

