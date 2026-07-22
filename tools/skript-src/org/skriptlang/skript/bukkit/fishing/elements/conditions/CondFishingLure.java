/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.fishing.elements.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Fishing Lure Applied")
@Description(value={"Checks if the lure enchantment is applied to the current fishing event."})
@Example(value="on fishing line cast:\n\tif lure enchantment bonus is applied:\n\t\tcancel event\n")
@Events(value={"Fishing"})
@Since(value={"2.10"})
public class CondFishingLure
extends Condition {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(CondFishingLure.class).addPatterns("lure enchantment bonus is (applied|active)", "lure enchantment bonus is(n't| not) (applied|active)").supplier(CondFishingLure::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerFishEvent.class)) {
            Skript.error("The 'lure enchantment' condition can only be used in a fishing event.");
            return false;
        }
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof PlayerFishEvent)) {
            return false;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        return fishEvent.getHook().getApplyLure() ^ this.isNegated();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "lure enchantment bonus " + (this.isNegated() ? "is" : "isn't") + " applied";
    }
}

