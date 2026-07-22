/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
 *  com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.types.EntityClassInfo;

@Name(value="Spectator Target")
@Description(value={"Grabs the spectator target entity of the players."})
@Example.Examples(value={@Example(value="on player start spectating of player:\n\tmessage \"&c%spectator target% currently has %{game::kills::%spectator target%}% kills!\" to the player\n"), @Example(value="on player stop spectating:\n\tpast spectator target was a zombie\n\tset spectator target to the nearest skeleton\n")})
@Since(value={"2.4-alpha4, 2.7 (Paper Spectator Event)"})
public class ExprSpectatorTarget
extends SimpleExpression<Entity> {
    private static final boolean EVENT_SUPPORT = Skript.classExists("com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent");
    private Expression<Player> players;
    private static final Changer<Entity> ENTITY_CHANGER;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = expressions[0];
        if (this.players == null && !EVENT_SUPPORT) {
            Skript.error("Your server platform does not support using 'spectator target' without players defined.'spectator target of event-player'");
            return false;
        }
        if (this.players == null && !this.getParser().isCurrentEvent(PlayerStartSpectatingEntityEvent.class, PlayerStopSpectatingEntityEvent.class)) {
            Skript.error("The expression 'spectator target' may only be used in a start/stop/swap spectating target event");
            return false;
        }
        return true;
    }

    @Nullable
    protected Entity[] get(Event event) {
        if (EVENT_SUPPORT && this.players == null && !Delay.isDelayed(event)) {
            if (event instanceof PlayerStartSpectatingEntityEvent) {
                if (this.getTime() == EventValues.TIME_PAST) {
                    return CollectionUtils.array(((PlayerStartSpectatingEntityEvent)event).getCurrentSpectatorTarget());
                }
                return CollectionUtils.array(((PlayerStartSpectatingEntityEvent)event).getNewSpectatorTarget());
            }
            if (event instanceof PlayerStopSpectatingEntityEvent) {
                if (this.getTime() == EventValues.TIME_FUTURE) {
                    return new Entity[0];
                }
                return CollectionUtils.array(((PlayerStopSpectatingEntityEvent)event).getSpectatorTarget());
            }
        }
        if (this.players == null) {
            return new Entity[0];
        }
        return (Entity[])this.players.stream(event).map(Player::getSpectatorTarget).toArray(Entity[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.players == null) {
            return ENTITY_CHANGER.acceptChange(mode);
        }
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.DELETE) {
            return CollectionUtils.array(Entity.class);
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (this.players == null) {
            Entity[] entities = this.get(event);
            if (entities.length == 0) {
                return;
            }
            ENTITY_CHANGER.change((Entity[])entities, delta, mode);
            return;
        }
        switch (mode) {
            case SET: {
                assert (delta != null);
                for (Player player : this.players.getArray(event)) {
                    if (player.getGameMode() != GameMode.SPECTATOR) continue;
                    player.setSpectatorTarget((Entity)delta[0]);
                }
                break;
            }
            case RESET: 
            case DELETE: {
                for (Player player : this.players.getArray(event)) {
                    if (player.getGameMode() != GameMode.SPECTATOR) continue;
                    player.setSpectatorTarget(null);
                }
                break;
            }
        }
    }

    @Override
    public boolean setTime(int time) {
        if (!EVENT_SUPPORT) {
            return false;
        }
        if (this.players == null) {
            return super.setTime(time, PlayerStartSpectatingEntityEvent.class, PlayerStopSpectatingEntityEvent.class);
        }
        return super.setTime(time, this.players, PlayerStartSpectatingEntityEvent.class, PlayerStopSpectatingEntityEvent.class);
    }

    @Override
    public boolean isSingle() {
        return this.players == null || this.players.isSingle();
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return Entity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "spectator target" + (String)(this.players != null ? " of " + this.players.toString(event, debug) : "");
    }

    static {
        Skript.registerExpression(ExprSpectatorTarget.class, Entity.class, ExpressionType.PROPERTY, "spectator target [of %-players%]", "%players%'[s] spectator target");
        ENTITY_CHANGER = new EntityClassInfo.EntityChanger();
    }
}

