/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerEggThrowEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Egg Hatch")
@Description(value={"Makes the egg hatch in a Player Egg Throw event."})
@Example(value="on player egg throw:\n\t# EGGS FOR DAYZ!\n\tmake the egg hatch\n")
@Events(value={"Egg Throw"})
@Since(value={"2.7"})
public class EffMakeEggHatch
extends Effect
implements EventRestrictedSyntax {
    private boolean not;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.not = parseResult.hasTag("not");
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerEggThrowEvent.class);
    }

    @Override
    protected void execute(Event e) {
        if (e instanceof PlayerEggThrowEvent) {
            PlayerEggThrowEvent event = (PlayerEggThrowEvent)e;
            event.setHatching(!this.not);
            if (!this.not && event.getNumHatches() == 0) {
                event.setNumHatches((byte)1);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make the egg " + (this.not ? "not " : "") + "hatch";
    }

    static {
        Skript.registerEffect(EffMakeEggHatch.class, "make [the] egg [:not] hatch");
    }
}

