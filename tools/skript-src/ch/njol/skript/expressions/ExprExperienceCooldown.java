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
import ch.njol.skript.util.Timespan;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Experience Pickup Cooldown")
@Description(value={"The experience cooldown of a player.", "Experience cooldown is how long until a player can pick up another orb of experience.", "The cooldown of a player must be 0 to pick up another orb of experience."})
@Example.Examples(value={@Example(value="send experience cooldown of player"), @Example(value="set the xp pickup cooldown of player to 1 hour"), @Example(value="if exp collection cooldown of player >= 10 minutes:\n\tclear the experience pickup cooldown of player\n")})
@Since(value={"2.10"})
public class ExprExperienceCooldown
extends SimplePropertyExpression<Player, Timespan> {
    private static final int maxTicks = Integer.MAX_VALUE;

    @Override
    public Timespan convert(Player player) {
        return new Timespan(Timespan.TimePeriod.TICK, player.getExpCooldown());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int providedTime = 0;
        if (delta[0] != null) {
            providedTime = (int)((Timespan)delta[0]).get(Timespan.TimePeriod.TICK);
        }
        switch (mode) {
            case ADD: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExpCooldown(Math2.fit(-1, player.getExpCooldown() + providedTime, Integer.MAX_VALUE));
                }
                break;
            }
            case REMOVE: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExpCooldown(Math2.fit(-1, player.getExpCooldown() - providedTime, Integer.MAX_VALUE));
                }
                break;
            }
            case SET: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExpCooldown(Math2.fit(-1, providedTime, Integer.MAX_VALUE));
                }
                break;
            }
            case RESET: 
            case DELETE: {
                for (Player player : (Player[])this.getExpr().getArray(event)) {
                    player.setExpCooldown(0);
                }
                break;
            }
        }
    }

    @Override
    protected String getPropertyName() {
        return "experience cooldown";
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    static {
        ExprExperienceCooldown.register(ExprExperienceCooldown.class, Timespan.class, "(experience|[e]xp) [pickup|collection] cooldown", "players");
    }
}

