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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Speed")
@Description(value={"A player's walking or flying speed. Both can be changed, but values must be between -1 and 1 (excessive values will be changed to -1 or 1 respectively). Negative values reverse directions.", "Please note that changing a player's speed will change their FOV just like potions do."})
@Example.Examples(value={@Example(value="set the player's walk speed to 1"), @Example(value="increase the argument's fly speed by 0.1")})
@Since(value={"unknown (before 2.1)"})
public class ExprSpeed
extends SimplePropertyExpression<Player, Number> {
    private boolean walk;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        super.init(exprs, matchedPattern, isDelayed, parseResult);
        this.walk = parseResult.mark == 0;
        return true;
    }

    @Override
    public Number convert(Player p) {
        return Float.valueOf(this.walk ? p.getWalkSpeed() : p.getFlySpeed());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) {
            return new Class[]{Number.class};
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        float input = delta == null ? 0.0f : ((Number)delta[0]).floatValue();
        for (Player p : (Player[])this.getExpr().getArray(e)) {
            float oldSpeed = this.walk ? p.getWalkSpeed() : p.getFlySpeed();
            float d = Math2.fit(-1.0f, switch (mode) {
                case Changer.ChangeMode.SET -> input;
                case Changer.ChangeMode.ADD -> oldSpeed + input;
                case Changer.ChangeMode.REMOVE -> oldSpeed - input;
                default -> this.walk ? 0.2f : 0.1f;
            }, 1.0f);
            if (this.walk) {
                p.setWalkSpeed(d);
                continue;
            }
            p.setFlySpeed(d);
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return this.walk ? "walk speed" : "fly speed";
    }

    static {
        ExprSpeed.register(ExprSpeed.class, Number.class, "(0\u00a6walk[ing]|1\u00a6fl(y[ing]|ight))[( |-)]speed", "players");
    }
}

