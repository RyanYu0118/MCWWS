/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.fishing.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.fishing.elements.effects.EffFishingLure;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Pull In Hooked Entity")
@Description(value={"Pull the hooked entity to the player."})
@Example(value="on fishing state of caught entity:\n\tpull in hooked entity\n")
@Events(value={"Fishing"})
@Since(value={"2.10"})
public class EffPullHookedEntity
extends Effect {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffFishingLure.class).addPatterns("(reel|pull) in [the] hook[ed] entity").supplier(EffFishingLure::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerFishEvent.class)) {
            Skript.error("The 'pull in hooked entity' effect can only be used in the fishing event.");
            return false;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (!(event instanceof PlayerFishEvent)) {
            return;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        fishEvent.getHook().pullHookedEntity();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "pull in hooked entity";
    }
}

