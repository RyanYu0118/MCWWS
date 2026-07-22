/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.FishHook
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.bukkit.event.player.PlayerFishEvent$State
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.fishing.elements.events;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import java.util.ArrayList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EvtFish
extends SkriptEvent {
    private State state;

    public static void register(SyntaxRegistry registry) {
        ArrayList<String> patterns = new ArrayList<String>();
        for (State state : State.values()) {
            if (state.state == null && state != State.STATE_CHANGE) continue;
            patterns.add(state.pattern);
        }
        registry.register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(EvtFish.class, "Fishing").addPatterns(patterns)).addEvent(PlayerFishEvent.class).addDescription("Called when a player triggers a fishing event.", "An entity hooked event is triggered when an entity gets caught by a fishing rod.", "A fish escape event is called when the player fails to click on time, and the fish escapes.", "A fish approaching event is when the bobber is waiting to be hooked, and a fish is approaching.", "A fishing state change event is triggered whenever the fishing state changes.").addExample("on fishing line cast:\n\tsend \"You caught a fish!\" to player\n").addExample("on entity caught:\n\tpush event-entity vector from entity to player\n").addExample("on fishing state change:\n\tif event-fishing state is fish caught:\n\t\tbroadcast \"A fish has been caught!\"\n").addSince("2.10", "2.11 (state change)").supplier(EvtFish::new)).build());
        EventValues.registerEventValue(PlayerFishEvent.class, Entity.class, PlayerFishEvent::getCaught);
        EventValues.registerEventValue(PlayerFishEvent.class, PlayerFishEvent.State.class, PlayerFishEvent::getState);
        EventValues.registerEventValue(PlayerFishEvent.class, FishHook.class, PlayerFishEvent::getHook);
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.state = State.values()[matchedPattern];
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof PlayerFishEvent)) {
            return false;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        return this.state == State.STATE_CHANGE || this.state.state == fishEvent.getState();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.state.toString;
    }

    private static enum State {
        FISHING(PlayerFishEvent.State.FISHING, "[fishing] (line|rod) cast", "fishing line cast"),
        CAUGHT_FISH(PlayerFishEvent.State.CAUGHT_FISH, "fish (caught|catch)", "fish caught"),
        CAUGHT_ENTITY(PlayerFishEvent.State.CAUGHT_ENTITY, "entity (hook[ed]|caught|catch)", "entity hooked"),
        IN_GROUND(PlayerFishEvent.State.IN_GROUND, "(bobber|hook) (in|hit) ground", "bobber hit ground"),
        FISH_ESCAPE(PlayerFishEvent.State.FAILED_ATTEMPT, "fish (escape|get away)", "fish escape"),
        REEL_IN(PlayerFishEvent.State.REEL_IN, "[fishing] (rod|line) reel in", "fishing rod reel in"),
        BITE(PlayerFishEvent.State.BITE, "fish bit(e|ing)", "fish bite"),
        LURED(State.getOptional("LURED"), "(fish approach[ing]|(bobber|hook) lure[d])", "fish approaching"),
        STATE_CHANGE(null, "fishing state change[d]", "fishing state changed");

        @Nullable
        private final PlayerFishEvent.State state;
        private final String pattern;
        private final String toString;

        private State(@NotNull PlayerFishEvent.State state, String pattern, String toString) {
            this.state = state;
            this.pattern = pattern;
            this.toString = toString;
        }

        private static PlayerFishEvent.State getOptional(String name) {
            try {
                return PlayerFishEvent.State.valueOf((String)name);
            }
            catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }
}

