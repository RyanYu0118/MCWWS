/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
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
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="No Damage Ticks")
@Description(value={"The number of ticks that an entity is invulnerable to damage for."})
@Example(value="on damage:\n\tset victim's invulnerability ticks to 20 #Victim will not take damage for the next second\n")
@Since(value={"2.5, 2.11 (deprecated)"})
@Deprecated(since="2.11.0", forRemoval=true)
public class ExprNoDamageTicks
extends SimplePropertyExpression<LivingEntity, Long> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ScriptWarning.printDeprecationWarning("This expression is deprecated. Please use 'invulnerability time' instead of 'invulnerability ticks'.");
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public Long convert(LivingEntity entity) {
        return entity.getNoDamageTicks();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Object object;
        int providedTicks = 0;
        if (delta != null && (object = delta[0]) instanceof Number) {
            Number number = (Number)object;
            providedTicks = number.intValue();
        }
        block5: for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: 
                case DELETE: 
                case RESET: {
                    entity.setNoDamageTicks(providedTicks);
                    continue block5;
                }
                case ADD: {
                    int current = entity.getNoDamageTicks();
                    int value = Math2.fit(0, current + providedTicks, Integer.MAX_VALUE);
                    entity.setNoDamageTicks(value);
                    continue block5;
                }
                case REMOVE: {
                    int current = entity.getNoDamageTicks();
                    int value = Math2.fit(0, current - providedTicks, Integer.MAX_VALUE);
                    entity.setNoDamageTicks(value);
                }
            }
        }
    }

    @Override
    protected String getPropertyName() {
        return "no damage ticks";
    }

    @Override
    public Class<Long> getReturnType() {
        return Long.class;
    }

    static {
        ExprNoDamageTicks.registerDefault(ExprNoDamageTicks.class, Long.class, "(invulnerability|invincibility|no damage) tick[s]", "livingentities");
    }
}

