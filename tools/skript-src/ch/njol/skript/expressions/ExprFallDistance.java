/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Fall Distance")
@Description(value={"The distance an entity has fallen for."})
@Example.Examples(value={@Example(value="set all entities' fall distance to 10"), @Example(value="on damage:\n\tsend \"%victim's fall distance%\" to victim\n")})
@Since(value={"2.5"})
public class ExprFallDistance
extends SimplePropertyExpression<Entity, Number> {
    @Override
    @Nullable
    public Number convert(Entity entity) {
        return Float.valueOf(entity.getFallDistance());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.REMOVE_ALL || mode == Changer.ChangeMode.DELETE ? null : CollectionUtils.array(Number.class);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta != null) {
            Entity[] entities = (Entity[])this.getExpr().getArray(e);
            if (entities.length < 1) {
                return;
            }
            Float number = Float.valueOf(((Number)delta[0]).floatValue());
            block5: for (Entity entity : entities) {
                Float fallDistance = Float.valueOf(entity.getFallDistance());
                switch (mode) {
                    case ADD: {
                        entity.setFallDistance(fallDistance.floatValue() + number.floatValue());
                        continue block5;
                    }
                    case SET: {
                        entity.setFallDistance(number.floatValue());
                        continue block5;
                    }
                    case REMOVE: {
                        entity.setFallDistance(fallDistance.floatValue() - number.floatValue());
                        continue block5;
                    }
                    default: {
                        assert (false);
                        continue block5;
                    }
                }
            }
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "fall distance";
    }

    static {
        ExprFallDistance.register(ExprFallDistance.class, Number.class, "fall[en] (distance|height)", "entities");
    }
}

