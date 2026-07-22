/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityUnleashEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Allow / Prevent Leash Drop")
@Description(value={"Allows or prevents the leash from being dropped in an unleash event."})
@Example(value="on unleash:\n\tif player is not set:\n\t\tprevent the leash from dropping\n\telse if player is op:\n\t\tallow the leash to drop\n")
@Keywords(value={"lead"})
@Events(value={"Leash / Unleash"})
@Since(value={"2.10"})
public class EffDropLeash
extends Effect
implements EventRestrictedSyntax {
    private boolean allowLeashDrop;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.allowLeashDrop = matchedPattern == 0;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityUnleashEvent.class);
    }

    @Override
    protected void execute(Event event) {
        if (!(event instanceof EntityUnleashEvent)) {
            return;
        }
        EntityUnleashEvent unleashEvent = (EntityUnleashEvent)event;
        unleashEvent.setDropLeash(this.allowLeashDrop);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.allowLeashDrop ? "allow the leash to drop" : "prevent the leash from dropping";
    }

    static {
        Skript.registerEffect(EffDropLeash.class, "(force|allow) [the] (lead|leash) [item] to drop", "(block|disallow|prevent) [the] (lead|leash) [item] from dropping");
    }
}

