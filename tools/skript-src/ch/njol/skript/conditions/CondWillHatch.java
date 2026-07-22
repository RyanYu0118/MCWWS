/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerEggThrowEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Egg Will Hatch")
@Description(value={"Whether the egg will hatch in a Player Egg Throw event."})
@Example(value="on player egg throw:\n\tif an entity won't hatch:\n\t\tsend \"Better luck next time!\" to the player\n")
@Events(value={"Egg Throw"})
@Since(value={"2.7"})
public class CondWillHatch
extends Condition
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(!parseResult.hasTag("will"));
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerEggThrowEvent.class);
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof PlayerEggThrowEvent)) {
            return false;
        }
        return ((PlayerEggThrowEvent)event).isHatching() ^ this.isNegated();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the egg " + (this.isNegated() ? "will" : "will not") + " hatch";
    }

    static {
        Skript.registerCondition(CondWillHatch.class, "[the] egg (:will|will not|won't) hatch");
    }
}

