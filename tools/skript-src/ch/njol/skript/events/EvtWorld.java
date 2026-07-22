/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.bukkit.event.world.WorldEvent
 *  org.bukkit.event.world.WorldInitEvent
 *  org.bukkit.event.world.WorldLoadEvent
 *  org.bukkit.event.world.WorldSaveEvent
 *  org.bukkit.event.world.WorldUnloadEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.Nullable;

public class EvtWorld
extends SkriptEvent {
    private Literal<World> worlds;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.worlds = args[0];
        if (this.worlds instanceof LiteralList && this.worlds.getAnd()) {
            ((LiteralList)this.worlds).invertAnd();
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.worlds == null) {
            return true;
        }
        World evtWorld = ((WorldEvent)event).getWorld();
        return this.worlds.check(event, world -> world.equals((Object)evtWorld));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "world save/init/unload/load" + (String)(this.worlds == null ? "" : " of " + this.worlds.toString(event, debug));
    }

    static {
        Skript.registerEvent("World Save", EvtWorld.class, WorldSaveEvent.class, "world sav(e|ing) [of %-worlds%]").description("Called when a world is saved to disk. Usually all worlds are saved simultaneously, but world management plugins could change this.").examples("on world save of \"world\":", "\tbroadcast \"The world %event-world% has been saved\"").since("1.0, 2.8.0 (defining worlds)");
        Skript.registerEvent("World Init", EvtWorld.class, WorldInitEvent.class, "world init[ialization] [of %-worlds%]").description("Called when a world is initialized. As all default worlds are initialized before", "any scripts are loaded, this event is only called for newly created worlds.", "World management plugins might change the behaviour of this event though.").examples("on world init of \"world_the_end\":").since("1.0, 2.8.0 (defining worlds)");
        Skript.registerEvent("World Unload", EvtWorld.class, WorldUnloadEvent.class, "world unload[ing] [of %-worlds%]").description("Called when a world is unloaded. This event will never be called if you don't have a world management plugin.").examples("on world unload:", "\tbroadcast \"the %event-world% has been unloaded!\"").since("1.0, 2.8.0 (defining worlds)");
        Skript.registerEvent("World Load", EvtWorld.class, WorldLoadEvent.class, "world load[ing] [of %-worlds%]").description("Called when a world is loaded. As with the world init event, this event will not be called for the server's default world(s).").examples("on world load of \"world_nether\":", "\tbroadcast \"The world %event-world% has been loaded!\"").since("1.0, 2.8.0 (defining worlds)");
    }
}

