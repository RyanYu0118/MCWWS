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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Fire Burn Duration")
@Description(value={"How much time an entity will be burning for."})
@Example.Examples(value={@Example(value="send \"You will stop burning in %fire time of player%\""), @Example(value="send the max burn time of target")})
@Since(value={"2.7, 2.10 (maximum)"})
public class ExprFireTicks
extends SimplePropertyExpression<Entity, Timespan> {
    private boolean max;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.max = parseResult.hasTag("max");
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Timespan convert(Entity entity) {
        return new Timespan(Timespan.TimePeriod.TICK, this.max ? entity.getMaxFireTicks() : Math.max(entity.getFireTicks(), 0));
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.max) {
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Entity[] entities = (Entity[])this.getExpr().getArray(event);
        int change = delta == null ? 0 : (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        switch (mode) {
            case REMOVE: {
                change = -change;
            }
            case ADD: {
                for (Entity entity : entities) {
                    entity.setFireTicks(entity.getFireTicks() + change);
                }
                break;
            }
            case SET: 
            case RESET: 
            case DELETE: {
                for (Entity entity : entities) {
                    entity.setFireTicks(change);
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public Class<Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "fire time";
    }

    static {
        ExprFireTicks.register(ExprFireTicks.class, Timespan.class, "[:max[imum]] (burn[ing]|fire) (time|duration)", "entities");
    }
}

