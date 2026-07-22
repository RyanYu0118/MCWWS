/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ExperienceOrb
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.bukkitutil.PlayerUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Total Experience")
@Description(value={"The total experience, in points, of players or experience orbs.", "Adding to a player's experience will trigger Mending, but setting their experience will not."})
@Example.Examples(value={@Example(value="set total experience of player to 100"), @Example(value="add 100 to player's experience"), @Example(value="if player's total experience is greater than 100:\n\tset player's total experience to 0\n\tgive player 1 diamond\n")})
@Since(value={"2.7"})
public class ExprTotalExperience
extends SimplePropertyExpression<Entity, Integer> {
    @Override
    @Nullable
    public Integer convert(Entity entity) {
        if (entity instanceof ExperienceOrb) {
            return ((ExperienceOrb)entity).getExperience();
        }
        if (entity instanceof Player) {
            return PlayerUtils.getTotalXP(((Player)entity).getLevel(), ((Player)entity).getExp());
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD: 
            case REMOVE: 
            case SET: 
            case DELETE: 
            case RESET: {
                return new Class[]{Number.class};
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int change = delta == null ? 0 : ((Number)delta[0]).intValue();
        switch (mode) {
            case SET: 
            case DELETE: 
            case RESET: {
                if (change < 0) {
                    change = 0;
                }
                for (Entity entity : (Entity[])this.getExpr().getArray(event)) {
                    if (entity instanceof ExperienceOrb) {
                        ((ExperienceOrb)entity).setExperience(change);
                        continue;
                    }
                    if (!(entity instanceof Player)) continue;
                    PlayerUtils.setTotalXP((Player)entity, change);
                }
                break;
            }
            case REMOVE: {
                change = -change;
            }
            case ADD: {
                for (Entity entity : (Entity[])this.getExpr().getArray(event)) {
                    int xp;
                    if (entity instanceof ExperienceOrb) {
                        xp = ((ExperienceOrb)entity).getExperience() + change;
                        ((ExperienceOrb)entity).setExperience(Math.max(xp, 0));
                        continue;
                    }
                    if (!(entity instanceof Player)) continue;
                    if (change < 0) {
                        xp = PlayerUtils.getTotalXP((Player)entity) + change;
                        PlayerUtils.setTotalXP((Player)entity, Math.max(xp, 0));
                        continue;
                    }
                    ((Player)entity).giveExp(change);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "total experience";
    }

    static {
        ExprTotalExperience.register(ExprTotalExperience.class, Integer.class, "[total] experience", "entities");
    }
}

