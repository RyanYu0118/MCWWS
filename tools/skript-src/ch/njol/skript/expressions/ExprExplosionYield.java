/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.ExplosionPrimeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Explosion Yield")
@Description(value={"The yield of the explosion in an explosion prime event. This is how big the explosion is.", " When changing the yield, values less than 0 will be ignored.", " Read <a href='https://minecraft.wiki/w/Explosion'>this wiki page</a> for more information"})
@Example(value="on explosion prime:\n\tset the yield of the explosion to 10\n")
@Events(value={"explosion prime"})
@Since(value={"2.5"})
public class ExprExplosionYield
extends SimpleExpression<Number>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(ExplosionPrimeEvent.class);
    }

    @Nullable
    protected Number[] get(Event e) {
        if (!(e instanceof ExplosionPrimeEvent)) {
            return null;
        }
        return new Number[]{Float.valueOf(((ExplosionPrimeEvent)e).getRadius())};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: 
            case DELETE: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        float f;
        float f2 = f = delta == null ? 0.0f : ((Number)delta[0]).floatValue();
        if (f < 0.0f || !(event instanceof ExplosionPrimeEvent)) {
            return;
        }
        ExplosionPrimeEvent e = (ExplosionPrimeEvent)event;
        switch (mode) {
            case SET: {
                e.setRadius(f);
                break;
            }
            case ADD: {
                float add = e.getRadius() + f;
                if (add < 0.0f) {
                    return;
                }
                e.setRadius(add);
                break;
            }
            case REMOVE: {
                float subtract = e.getRadius() - f;
                if (subtract < 0.0f) {
                    return;
                }
                e.setRadius(subtract);
                break;
            }
            case DELETE: {
                e.setRadius(0.0f);
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the yield of the explosion";
    }

    static {
        Skript.registerExpression(ExprExplosionYield.class, Number.class, ExpressionType.SIMPLE, "[the] explosion (yield|radius|size)", "[the] (yield|radius|size) of [the] explosion");
    }
}

