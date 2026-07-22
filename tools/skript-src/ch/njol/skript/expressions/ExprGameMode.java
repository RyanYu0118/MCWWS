/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerGameModeChangeEvent
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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Game Mode")
@Description(value={"The gamemode of a player. (<a href=\"#gamemode\">Gamemodes</a>)"})
@Example.Examples(value={@Example(value="player's gamemode is survival"), @Example(value="set the player's gamemode to creative")})
@Since(value={"1.0"})
public class ExprGameMode
extends PropertyExpression<Player, GameMode> {
    @Override
    public boolean init(Expression<?>[] vars, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(vars[0]);
        return true;
    }

    protected GameMode[] get(Event e, Player[] source) {
        return this.get(source, player -> {
            if (this.getTime() >= 0 && e instanceof PlayerGameModeChangeEvent && ((PlayerGameModeChangeEvent)e).getPlayer() == player && !Delay.isDelayed(e)) {
                return ((PlayerGameModeChangeEvent)e).getNewGameMode();
            }
            return player.getGameMode();
        });
    }

    @Override
    public Class<GameMode> getReturnType() {
        return GameMode.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the gamemode of " + this.getExpr().toString(e, debug);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
            return CollectionUtils.array(GameMode.class);
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        GameMode m = delta == null ? Bukkit.getDefaultGameMode() : (GameMode)delta[0];
        for (Player p : (Player[])this.getExpr().getArray(e)) {
            if (this.getTime() >= 0 && e instanceof PlayerGameModeChangeEvent && ((PlayerGameModeChangeEvent)e).getPlayer() == p && !Delay.isDelayed(e) && ((PlayerGameModeChangeEvent)e).getNewGameMode() != m) {
                ((PlayerGameModeChangeEvent)e).setCancelled(true);
            }
            p.setGameMode(m);
        }
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, (Class<? extends Event>)PlayerGameModeChangeEvent.class);
    }

    static {
        Skript.registerExpression(ExprGameMode.class, GameMode.class, ExpressionType.PROPERTY, "[the] game[ ]mode of %players%", "%players%'[s] game[ ]mode");
    }
}

