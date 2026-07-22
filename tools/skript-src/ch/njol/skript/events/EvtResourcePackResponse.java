/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent$Status
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

public class EvtResourcePackResponse
extends SkriptEvent {
    @Nullable
    private Literal<PlayerResourcePackStatusEvent.Status> states;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        if (matchedPattern == 1) {
            this.states = args[0];
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (this.states != null) {
            PlayerResourcePackStatusEvent.Status state = ((PlayerResourcePackStatusEvent)e).getStatus();
            return this.states.check(e, arg_0 -> state.equals(arg_0));
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.states != null ? "resource pack " + this.states.toString(e, debug) : "resource pack request response";
    }

    static {
        Skript.registerEvent("Resource Pack Request Response", EvtResourcePackResponse.class, PlayerResourcePackStatusEvent.class, "resource pack [request] response", "resource pack [request] %resourcepackstates%").description("Called when a player takes action on a resource pack request sent via the ", "<a href='#EffSendResourcePack'>send resource pack</a> effect. ", "The <a href='#CondResourcePack'>resource pack</a> condition can be used ", "to check the resource pack state.", "", "This event will be triggered once when the player accepts or declines the resource pack request, ", "and once when the resource pack is successfully installed or failed to download.").examples("on resource pack request response:", "\tif the resource pack was declined or failed to download:", "", "on resource pack deny:", "\tkick the player due to \"You have to install the resource pack to play in this server!\"").since("2.4");
    }
}

