/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Food Level")
@Description(value={"The food level of a player from 0 to 10. Has several aliases: food/hunger level/meter/bar. "})
@Example(value="set the player's food level to 10")
@Since(value={"1.0"})
public class ExprFoodLevel
extends PropertyExpression<Player, Number> {
    @Override
    public boolean init(Expression<?>[] vars, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(vars[0]);
        return true;
    }

    protected Number[] get(Event event, Player[] source) {
        return this.get(source, player -> {
            FoodLevelChangeEvent foodLevelChangeEvent;
            if (this.getTime() >= 0 && event instanceof FoodLevelChangeEvent && player.equals((Object)(foodLevelChangeEvent = (FoodLevelChangeEvent)event).getEntity()) && !Delay.isDelayed(event)) {
                return Float.valueOf(0.5f * (float)foodLevelChangeEvent.getFoodLevel());
            }
            return Float.valueOf(0.5f * (float)player.getFoodLevel());
        });
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the food level of " + this.getExpr().toString(e, debug);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL) {
            return null;
        }
        return CollectionUtils.array(Number.class);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        assert (mode != Changer.ChangeMode.REMOVE_ALL);
        int s = delta == null ? 0 : Math.round(((Number)delta[0]).floatValue() * 2.0f);
        for (Player player : (Player[])this.getExpr().getArray(e)) {
            boolean event = this.getTime() >= 0 && e instanceof FoodLevelChangeEvent && ((FoodLevelChangeEvent)e).getEntity() == player && !Delay.isDelayed(e);
            int food = event ? ((FoodLevelChangeEvent)e).getFoodLevel() : player.getFoodLevel();
            switch (mode) {
                case SET: 
                case DELETE: {
                    food = Math2.fit(0, s, 20);
                    break;
                }
                case ADD: {
                    food = Math2.fit(0, food + s, 20);
                    break;
                }
                case REMOVE: {
                    food = Math2.fit(0, food - s, 20);
                    break;
                }
                case RESET: {
                    food = 20;
                    break;
                }
                case REMOVE_ALL: {
                    assert (false);
                    break;
                }
            }
            if (event) {
                ((FoodLevelChangeEvent)e).setFoodLevel(food);
                continue;
            }
            player.setFoodLevel(food);
        }
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, FoodLevelChangeEvent.class, this.getExpr());
    }

    static {
        Skript.registerExpression(ExprFoodLevel.class, Number.class, ExpressionType.PROPERTY, "[the] (food|hunger)[[ ](level|met(er|re)|bar)] [of %players%]", "%players%'[s] (food|hunger)[[ ](level|met(er|re)|bar)]");
    }
}

