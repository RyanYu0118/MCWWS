/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

@Name(value="Maximum Freeze Time")
@Description(value={"The maximum amount of time an entity can spend in powdered snow before taking damage."})
@Example(value="difference between player's freeze time and player's max freeze time is less than 1 second:\n\tsend \"you're about to freeze!\" to the player\n")
@Since(value={"2.7"})
public class ExprMaxFreezeTicks
extends SimplePropertyExpression<Entity, Timespan> {
    @Override
    @Nullable
    public Timespan convert(Entity entity) {
        return new Timespan(Timespan.TimePeriod.TICK, entity.getMaxFreezeTicks());
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "maximum freeze time";
    }

    static {
        if (Skript.methodExists(Entity.class, "getMaxFreezeTicks", new Class[0])) {
            ExprMaxFreezeTicks.register(ExprMaxFreezeTicks.class, Timespan.class, "max[imum] freeze time", "entities");
        }
    }
}

