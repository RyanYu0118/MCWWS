/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Will Consume Boosting Firework")
@Description(value={"Checks to see if the firework used in an 'elytra boost' event will be consumed."})
@Example(value="on elytra boost:\n\tif the used firework will be consumed:\n\t\tprevent the used firework from being consumed\n")
@Since(value={"2.10"})
public class CondElytraBoostConsume
extends Condition
implements EventRestrictedSyntax {
    private boolean checkConsume;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.checkConsume = matchedPattern == 0;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerElytraBoostEvent.class);
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof PlayerElytraBoostEvent)) {
            return false;
        }
        PlayerElytraBoostEvent boostEvent = (PlayerElytraBoostEvent)event;
        return boostEvent.shouldConsume() == this.checkConsume;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the boosting firework will " + (this.checkConsume ? "" : "not") + " be consumed";
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.player.PlayerElytraBoostEvent")) {
            Skript.registerCondition(CondElytraBoostConsume.class, "[the] (boosting|used) firework will be consumed", "[the] (boosting|used) firework (will not|won't) be consumed");
        }
    }
}

