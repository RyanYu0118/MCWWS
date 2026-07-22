/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerLevelChangeEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
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
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Level")
@Description(value={"The experience level of a player."})
@Example.Examples(value={@Example(value="reduce the victim's level by 1"), @Example(value="set the player's level to 0"), @Example(value="on level change:\n\tset {_diff} to future xp level - past exp level\n\tbroadcast \"%player%'s level changed by %{_diff}%!\"\n")})
@Since(value={"unknown (before 2.1), 2.13.2 (allow player default)"})
@Events(value={"level change"})
public class ExprLevel
extends SimplePropertyExpression<Player, Long> {
    protected Long[] get(Event event, Player[] source) {
        return super.get(source, player -> {
            PlayerLevelChangeEvent playerLevelChangeEvent;
            if (event instanceof PlayerLevelChangeEvent && (playerLevelChangeEvent = (PlayerLevelChangeEvent)event).getPlayer() == player && !Delay.isDelayed(event)) {
                return this.getTime() < 0 ? playerLevelChangeEvent.getOldLevel() : playerLevelChangeEvent.getNewLevel();
            }
            return player.getLevel();
        });
    }

    @Override
    @Nullable
    public Long convert(Player player) {
        assert (false);
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL) {
            return null;
        }
        if (this.getParser().isCurrentEvent((Class<? extends Event>)PlayerRespawnEvent.class) && !this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("Cannot change a player's level in a respawn event. Add a delay of 1 tick or change the 'new level' in a death event.");
            return null;
        }
        if (this.getParser().isCurrentEvent((Class<? extends Event>)EntityDeathEvent.class) && this.getTime() == 0 && this.getExpr().isDefault() && !this.getParser().getHasDelayBefore().isTrue()) {
            Skript.warning("Changing the player's level in a death event will change the player's level before they die. Use either 'past level of player' or 'new level of player' to clearly state whether to change the level before or after they die.");
        }
        if (this.getTime() == -1 && !this.getParser().isCurrentEvent((Class<? extends Event>)EntityDeathEvent.class)) {
            return null;
        }
        if (this.getTime() != 0 && this.getParser().isCurrentEvent((Class<? extends Event>)PlayerLevelChangeEvent.class)) {
            Skript.error("Changing the past or future level in a level change event has no effect.");
            return null;
        }
        return new Class[]{Number.class};
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (mode != Changer.ChangeMode.REMOVE_ALL);
        int deltaAmount = delta == null ? 0 : ((Number)delta[0]).intValue();
        for (Player player : (Player[])this.getExpr().getArray(event)) {
            PlayerDeathEvent playerDeathEvent;
            int level = this.getTime() > 0 && event instanceof PlayerDeathEvent && (playerDeathEvent = (PlayerDeathEvent)event).getEntity() == player && !Delay.isDelayed(event) ? playerDeathEvent.getNewLevel() : player.getLevel();
            switch (mode) {
                case SET: {
                    level = deltaAmount;
                    break;
                }
                case ADD: {
                    level += deltaAmount;
                    break;
                }
                case REMOVE: {
                    level -= deltaAmount;
                    break;
                }
                case DELETE: 
                case RESET: {
                    level = 0;
                }
            }
            if (level < 0) {
                level = 0;
            }
            if (this.getTime() > 0 && event instanceof PlayerDeathEvent && (playerDeathEvent = (PlayerDeathEvent)event).getEntity() == player && !Delay.isDelayed(event)) {
                playerDeathEvent.setNewLevel(level);
                continue;
            }
            player.setLevel(level);
        }
    }

    @Override
    public Class<Long> getReturnType() {
        return Long.class;
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, this.getExpr(), PlayerLevelChangeEvent.class, PlayerDeathEvent.class);
    }

    @Override
    protected String getPropertyName() {
        return "level";
    }

    static {
        ExprLevel.registerDefault(ExprLevel.class, Long.class, "[xp|exp[erience]] level", "players");
    }
}

