/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityExplodeEvent
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
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Explosion Block Yield")
@Description(value={"The percentage of exploded blocks dropped in an explosion event.", "When changing the yield, a value greater than 1 will function the same as using 1.", "Attempting to change the yield to a value less than 0 will have no effect."})
@Example(value="on explode:\n\tset the explosion's block yield to 10%\n")
@Events(value={"explode"})
@Since(value={"2.5"})
public class ExprExplosionBlockYield
extends SimpleExpression<Number>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityExplodeEvent.class);
    }

    @Nullable
    protected Number[] get(Event e) {
        if (!(e instanceof EntityExplodeEvent)) {
            return null;
        }
        return new Number[]{Float.valueOf(((EntityExplodeEvent)e).getYield())};
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
        float n;
        float f = n = delta == null ? 0.0f : ((Number)delta[0]).floatValue();
        if (n < 0.0f || !(event instanceof EntityExplodeEvent)) {
            return;
        }
        EntityExplodeEvent e = (EntityExplodeEvent)event;
        switch (mode) {
            case SET: {
                e.setYield(n);
                break;
            }
            case ADD: {
                float add = e.getYield() + n;
                if (add < 0.0f) {
                    return;
                }
                e.setYield(add);
                break;
            }
            case REMOVE: {
                float subtract = e.getYield() - n;
                if (subtract < 0.0f) {
                    return;
                }
                e.setYield(subtract);
                break;
            }
            case DELETE: {
                e.setYield(0.0f);
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
        return "the explosion's block yield";
    }

    static {
        Skript.registerExpression(ExprExplosionBlockYield.class, Number.class, ExpressionType.PROPERTY, "[the] [explosion['s]] block (yield|amount)", "[the] percentage of blocks dropped");
    }
}

