/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Exhaustion")
@Description(value={"The exhaustion of a player. This is mainly used to determine the rate of hunger depletion."})
@Example(value="set exhaustion of all players to 1")
@Since(value={"2.2-dev35"})
public class ExprExhaustion
extends SimplePropertyExpression<Player, Number> {
    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "exhaustion";
    }

    @Override
    @Nullable
    public Number convert(Player player) {
        return Float.valueOf(player.getExhaustion());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return CollectionUtils.array(Number.class);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        float exhaustion = ((Number)delta[0]).floatValue();
        switch (mode) {
            case ADD: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExhaustion(player.getExhaustion() + exhaustion);
                }
                break;
            }
            case REMOVE: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExhaustion(player.getExhaustion() - exhaustion);
                }
                break;
            }
            case SET: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExhaustion(((Number)delta[0]).floatValue());
                }
                break;
            }
            case DELETE: 
            case REMOVE_ALL: 
            case RESET: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExhaustion(0.0f);
                }
                break;
            }
        }
    }

    static {
        ExprExhaustion.register(ExprExhaustion.class, Number.class, "exhaustion", "players");
    }
}

