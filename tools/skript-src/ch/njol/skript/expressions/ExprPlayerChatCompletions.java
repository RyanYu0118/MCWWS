/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Player Chat Completions")
@Description(value={"The custom chat completion suggestions. You can add, set, remove, and clear them. Removing the names of online players with this expression is ineffective.", "This expression will not return anything due to Bukkit limitations."})
@Example.Examples(value={@Example(value="add \"Skript\" and \"Njol\" to chat completions of all players"), @Example(value="remove \"text\" from {_p}'s chat completions"), @Example(value="clear player's chat completions")})
@RequiredPlugins(value={"Spigot 1.19+"})
@Since(value={"2.10"})
public class ExprPlayerChatCompletions
extends SimplePropertyExpression<Player, String> {
    @Override
    @Nullable
    public String convert(Player player) {
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.REMOVE, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(String[].class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        List<Object> completions;
        Player[] players;
        block12: {
            block13: {
                players = (Player[])this.getExpr().getArray(event);
                if (players.length == 0) {
                    return;
                }
                completions = new ArrayList();
                if (delta == null) break block12;
                if (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) break block13;
                if (mode != Changer.ChangeMode.SET) break block12;
            }
            completions = Arrays.stream(delta).filter(String.class::isInstance).map(String.class::cast).collect(Collectors.toList());
        }
        switch (mode) {
            case SET: 
            case DELETE: 
            case RESET: {
                for (Player player : players) {
                    player.setCustomChatCompletions(completions);
                }
                break;
            }
            case ADD: {
                for (Player player : players) {
                    player.addCustomChatCompletions(completions);
                }
                break;
            }
            case REMOVE: {
                for (Player player : players) {
                    player.removeCustomChatCompletions(completions);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "custom chat completions";
    }

    static {
        if (Skript.methodExists(Player.class, "addCustomChatCompletions", Collection.class)) {
            ExprPlayerChatCompletions.register(ExprPlayerChatCompletions.class, String.class, "[custom] chat completion[s]", "players");
        }
    }
}

