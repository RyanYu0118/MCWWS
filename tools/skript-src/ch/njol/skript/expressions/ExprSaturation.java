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

@Name(value="Saturation")
@Description(value={"The saturation of a player. If used in a player event, it can be omitted and will default to event-player."})
@Example(value="set saturation of player to 20")
@Since(value={"2.2-Fixes-v10, 2.2-dev35 (fully modifiable), 2.6.2 (syntax pattern changed)"})
public class ExprSaturation
extends SimplePropertyExpression<Player, Number> {
    @Override
    @Nullable
    public Number convert(Player player) {
        return Float.valueOf(player.getSaturation());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode != Changer.ChangeMode.REMOVE_ALL ? CollectionUtils.array(Number.class) : null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Float value = delta != null ? Float.valueOf(((Number)delta[0]).floatValue()) : null;
        switch (mode) {
            case ADD: {
                for (Player player : (Player[])this.getExpr().getArray(e)) {
                    player.setSaturation(player.getSaturation() + value.floatValue());
                }
                break;
            }
            case REMOVE: {
                for (Player player : (Player[])this.getExpr().getArray(e)) {
                    player.setSaturation(player.getSaturation() - value.floatValue());
                }
                break;
            }
            case SET: {
                for (Player player : (Player[])this.getExpr().getArray(e)) {
                    player.setSaturation(value.floatValue());
                }
                break;
            }
            case DELETE: 
            case REMOVE_ALL: 
            case RESET: {
                for (Player player : (Player[])this.getExpr().getArray(e)) {
                    player.setSaturation(0.0f);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "saturation";
    }

    static {
        ExprSaturation.register(ExprSaturation.class, Number.class, "saturation", "players");
    }
}

