/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Consume Boosting Firework")
@Description(value={"Prevent the firework used in an 'elytra boost' event to be consumed."})
@Example(value="on elytra boost:\n\tif the used firework will be consumed:\n\t\tprevent the used firework from being consume\n")
@Since(value={"2.10"})
public class EffElytraBoostConsume
extends Effect
implements EventRestrictedSyntax {
    private boolean consume;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.consume = matchedPattern == 1;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerElytraBoostEvent.class);
    }

    @Override
    protected void execute(Event event) {
        if (!(event instanceof PlayerElytraBoostEvent)) {
            return;
        }
        PlayerElytraBoostEvent boostEvent = (PlayerElytraBoostEvent)event;
        boostEvent.setShouldConsume(this.consume);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.consume) {
            return "allow the boosting firework to be consumed";
        }
        return "prevent the boosting firework from being consumed";
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.player.PlayerElytraBoostEvent")) {
            Skript.registerEffect(EffElytraBoostConsume.class, "(prevent|disallow) [the] (boosting|used) firework from being consumed", "allow [the] (boosting|used) firework to be consumed");
        }
    }
}

