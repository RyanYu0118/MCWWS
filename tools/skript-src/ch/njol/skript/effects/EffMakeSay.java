/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Say")
@Description(value={"Forces a player to send a message to the chat. If the message starts with a slash it will force the player to use command."})
@Example.Examples(value={@Example(value="make the player say \"Hello.\""), @Example(value="force all players to send the message \"I love this server\"")})
@Since(value={"2.3"})
public class EffMakeSay
extends Effect {
    private Expression<Player> players;
    private Expression<String> messages;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.messages = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        for (Player player : this.players.getArray(e)) {
            for (String message : this.messages.getArray(e)) {
                player.chat(message);
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "make " + this.players.toString(e, debug) + " say " + this.messages.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffMakeSay.class, "make %players% (say|send [the] message[s]) %strings%", "force %players% to (say|send [the] message[s]) %strings%");
    }
}

