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
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Apply Fishing Lure")
@Description(value={"Sets whether the lure enchantment should be applied, which reduces the wait time."})
@Example(value="on fishing line cast:\n\tapply lure enchantment bonus\n")
@Events(value={"Fishing"})
@Since(value={"2.10"})
public class EffFishingLure
extends Effect {
    private boolean remove;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffFishingLure.class).addPatterns("apply [the] lure enchantment bonus", "remove [the] lure enchantment bonus").supplier(EffFishingLure::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerFishEvent.class)) {
            Skript.error("The 'fishing hook lure' effect can only be used in a fishing event.");
            return false;
        }
        this.remove = matchedPattern == 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (!(event instanceof PlayerFishEvent)) {
            return;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        fishEvent.getHook().setApplyLure(!this.remove);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.remove ? "remove" : "apply") + " the lure enchantment bonus";
    }
}

