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
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Level Progress")
@Description(value={"The player's progress in reaching the next level, this represents the experience bar in the game. Please note that this value is between 0 and 1 (e.g. 0.5 = half experience bar).", "Changing this value can cause the player's level to change if the resulting level progess is negative or larger than 1, e.g. <code>increase the player's level progress by 0.5</code> will make the player gain a level if their progress was more than 50%."})
@Example(value="# use the exp bar as mana\non rightclick with a blaze rod:\n\tplayer's level progress is larger than 0.2\n\tshoot a fireball from the player\n\treduce the player's level progress by 0.2\nevery 2 seconds:\n\tloop all players:\n\t\tlevel progress of loop-player is smaller than 0.9:\n\t\t\tincrease level progress of the loop-player by 0.1\n\t\telse:\n\t\t\tset level progress of the loop-player to 0.99\non xp spawn:\n\tcancel event\n")
@Since(value={"2.0"})
@Events(value={"level change"})
public class ExprLevelProgress
extends SimplePropertyExpression<Player, Number> {
    @Override
    public Number convert(Player p) {
        return Float.valueOf(p.getExp());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL) {
            return null;
        }
        return new Class[]{Number.class};
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        assert (mode != Changer.ChangeMode.REMOVE_ALL);
        float d = delta == null ? 0.0f : ((Number)delta[0]).floatValue();
        for (Player p : (Player[])this.getExpr().getArray(e)) {
            float c;
            switch (mode) {
                case SET: {
                    c = d;
                    break;
                }
                case ADD: {
                    c = p.getExp() + d;
                    break;
                }
                case REMOVE: {
                    c = p.getExp() - d;
                    break;
                }
                case DELETE: 
                case RESET: {
                    c = 0.0f;
                    break;
                }
                default: {
                    assert (false);
                    return;
                }
            }
            p.setLevel(Math.max(0, p.getLevel() + (int)Math.floor(c)));
            p.setExp(Math2.mod(Math2.safe(c), 1.0f));
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "level progress";
    }

    static {
        ExprLevelProgress.register(ExprLevelProgress.class, Number.class, "level progress", "players");
    }
}

