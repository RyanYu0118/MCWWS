/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.ClientOption
 *  com.destroystokyo.paper.ClientOption$ChatVisibility
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.destroystokyo.paper.ClientOption;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Can See Messages")
@Description(value={"Checks whether a player can see specific message types in chat."})
@Example.Examples(value={@Example(value="if player can see all messages:\n\tsend \"You can see all messages.\"\n"), @Example(value="if player can only see commands:\n\tsend \"This game doesn't work with commands-only chat.\"\n"), @Example(value="if player can't see any messages:\n\tsend action bar \"Server shutting down in 5 minutes!\"\n")})
@Since(value={"2.10"})
public class CondChatVisibility
extends Condition {
    private int pattern = 0;
    private Expression<Player> player;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.pattern = matchedPattern;
        this.player = expressions[0];
        this.setNegated(matchedPattern > 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Player player = this.player.getSingle(event);
        if (player == null) {
            return false;
        }
        ClientOption.ChatVisibility current = (ClientOption.ChatVisibility)player.getClientOption(ClientOption.CHAT_VISIBILITY);
        return switch (this.pattern) {
            case 0 -> {
                if (current == ClientOption.ChatVisibility.FULL) {
                    yield true;
                }
                yield false;
            }
            case 1 -> {
                if (current == ClientOption.ChatVisibility.SYSTEM) {
                    yield true;
                }
                yield false;
            }
            case 2 -> {
                if (current == ClientOption.ChatVisibility.HIDDEN) {
                    yield true;
                }
                yield false;
            }
            case 3 -> {
                if (current != ClientOption.ChatVisibility.FULL) {
                    yield true;
                }
                yield false;
            }
            case 4 -> {
                if (current != ClientOption.ChatVisibility.SYSTEM) {
                    yield true;
                }
                yield false;
            }
            default -> throw new IllegalStateException("Unexpected value: " + this.pattern);
        };
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (this.pattern) {
            case 0 -> this.player.toString(event, debug) + " can see all messages";
            case 1 -> this.player.toString(event, debug) + " can only see commands";
            case 2 -> this.player.toString(event, debug) + " can't see any messages";
            case 3 -> this.player.toString(event, debug) + " can't see all messages";
            case 4 -> this.player.toString(event, debug) + " can't only see commands";
            default -> throw new IllegalStateException("Unexpected value: " + this.pattern);
        };
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.ClientOption$ChatVisibility")) {
            Skript.registerCondition(CondChatVisibility.class, "%player% can see all messages [in chat]", "%player% can only see (commands|system messages) [in chat]", "%player% can('t|[ ]not) see any (command[s]|message[s]) [in chat]", "%player% can('t|[ ]not) see all messages [in chat]", "%player% can('t|[ ]not) only see (commands|system messages) [in chat]");
        }
    }
}

