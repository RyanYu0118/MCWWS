/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableCollection
 *  com.google.common.collect.ImmutableList
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerCommandSendEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.jetbrains.annotations.Nullable;

public class EvtPlayerCommandSend
extends SkriptEvent {
    private final Collection<String> originalCommands = new ArrayList<String>();

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        this.originalCommands.clear();
        this.originalCommands.addAll(((PlayerCommandSendEvent)event).getCommands());
        return true;
    }

    public ImmutableCollection<String> getOriginalCommands() {
        return ImmutableList.copyOf(this.originalCommands);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "sending of the server command list";
    }

    static {
        Skript.registerEvent("Send Command List", EvtPlayerCommandSend.class, PlayerCommandSendEvent.class, "send[ing] [of [the]] [server] command[s] list", "[server] command list send").description("Called when the server sends a list of commands to the player. This usually happens on join. The sent commands can be modified via the <a href='#ExprSentCommands'>sent commands expression</a>.", "Modifications will affect what commands show up for the player to tab complete. They will not affect what commands the player can actually run.", "Adding new commands to the list is illegal behavior and will be ignored.").examples("on send command list:", "\tset command list to command list where [input does not contain \":\"]", "\tremove \"help\" from command list").since("2.8.0");
    }
}

