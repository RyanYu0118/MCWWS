/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Statistic
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Time Played")
@Description(value={"The amount of time a player has played for on the server. This info is stored in the player's statistics in the main world's data folder. Changing this will also change the player's stats which can be views in the client's statistics menu.", "Using this expression on offline players on Minecraft 1.14 and below will return nothing <code>&lt;none&gt;</code>."})
@Example.Examples(value={@Example(value="set {_t} to time played of player"), @Example(value="if player's time played is greater than 10 minutes:\n\tgive player a diamond sword\n"), @Example(value="set player's time played to 0 seconds")})
@RequiredPlugins(value={"MC 1.15+ (offline players)"})
@Since(value={"2.5, 2.7 (offline players)"})
public class ExprTimePlayed
extends SimplePropertyExpression<OfflinePlayer, Timespan> {
    private static final boolean IS_OFFLINE_SUPPORTED = Skript.methodExists(OfflinePlayer.class, "getStatistic", Statistic.class);

    @Override
    @Nullable
    public Timespan convert(OfflinePlayer offlinePlayer) {
        return this.getTimePlayed(offlinePlayer);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) {
            return CollectionUtils.array(Timespan.class);
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        long ticks = ((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        for (OfflinePlayer offlinePlayer : (OfflinePlayer[])this.getExpr().getArray(event)) {
            Timespan playerTimespan;
            if (!IS_OFFLINE_SUPPORTED && !offlinePlayer.isOnline() || (playerTimespan = this.getTimePlayed(offlinePlayer)) == null) continue;
            long playerTicks = playerTimespan.getAs(Timespan.TimePeriod.TICK);
            switch (mode) {
                case ADD: {
                    ticks = playerTicks + ticks;
                    break;
                }
                case REMOVE: {
                    ticks = playerTicks - ticks;
                }
            }
            if (IS_OFFLINE_SUPPORTED) {
                offlinePlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, (int)ticks);
                continue;
            }
            if (!offlinePlayer.isOnline()) continue;
            offlinePlayer.getPlayer().setStatistic(Statistic.PLAY_ONE_MINUTE, (int)ticks);
        }
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "time played";
    }

    @Nullable
    private Timespan getTimePlayed(OfflinePlayer offlinePlayer) {
        if (IS_OFFLINE_SUPPORTED) {
            return new Timespan(Timespan.TimePeriod.TICK, offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE));
        }
        if (offlinePlayer.isOnline()) {
            return new Timespan(Timespan.TimePeriod.TICK, offlinePlayer.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE));
        }
        return null;
    }

    static {
        ExprTimePlayed.register(ExprTimePlayed.class, Timespan.class, "(time played|play[ ]time)", "offlineplayers");
    }
}

