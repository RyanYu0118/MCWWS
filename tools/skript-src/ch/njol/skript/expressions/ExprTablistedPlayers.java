/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Tablisted Players")
@Description(value={"The players shown in the tab lists of the specified players.", "`delete` will remove all the online players from the tab list.", "`reset` will reset the tab list to the default state, which makes all players visible again."})
@Example(value="tablist players of player")
@Since(value={"2.13"})
@Keywords(value={"tablist"})
public class ExprTablistedPlayers
extends PropertyExpression<Player, Player> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(expressions[0]);
        return true;
    }

    protected Player[] get(Event event, Player[] source) {
        return (Player[])Arrays.stream(source).flatMap(viewer -> Bukkit.getOnlinePlayers().stream().filter(arg_0 -> ((Player)viewer).isListed(arg_0))).distinct().toArray(Player[]::new);
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return CollectionUtils.array(Player[].class);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Player[] recipients = (Player[])delta;
        Player[] viewers = (Player[])this.getExpr().getArray(event);
        switch (mode) {
            case DELETE: {
                recipients = (Player[])Bukkit.getOnlinePlayers().toArray(Player[]::new);
            }
            case REMOVE: {
                for (Player viewer : viewers) {
                    for (Player player : recipients) {
                        viewer.unlistPlayer(player);
                    }
                }
                break;
            }
            case SET: {
                for (Player viewer : viewers) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!Arrays.stream(recipients).noneMatch(recipient -> recipient.equals((Object)player))) continue;
                        viewer.unlistPlayer(player);
                    }
                }
            }
            case ADD: {
                assert (recipients != null);
                for (Player viewer : viewers) {
                    for (Player player : recipients) {
                        if (!viewer.canSee(player)) continue;
                        viewer.listPlayer(player);
                    }
                }
                break;
            }
            case RESET: {
                for (Player viewer : viewers) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (viewer.isListed(player) || !viewer.canSee(player)) continue;
                        viewer.listPlayer(player);
                    }
                }
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tablisted players of " + this.getExpr().toString(event, debug);
    }

    static {
        ExprTablistedPlayers.registerDefault(ExprTablistedPlayers.class, Player.class, "(tablist[ed]|listed) players", "players");
    }
}

