/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Explosive
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.ExplosionPrimeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Incendiary")
@Description(value={"Checks if an entity will create fire when it explodes. This condition is also usable in an explosion prime event."})
@Example(value="on explosion prime:\n\tif the explosion is fiery:\n\t\tbroadcast \"A fiery explosive has been ignited!\"\n")
@Since(value={"2.5"})
public class CondIncendiary
extends Condition {
    private Expression<Entity> entities;
    private boolean isEvent;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean bl = this.isEvent = matchedPattern == 2;
        if (this.isEvent && !this.getParser().isCurrentEvent((Class<? extends Event>)ExplosionPrimeEvent.class)) {
            Skript.error("Checking if 'the explosion' is fiery is only possible in an explosion prime event", ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        if (!this.isEvent) {
            this.entities = exprs[0];
        }
        this.setNegated(matchedPattern == 1 || parseResult.mark == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (this.isEvent) {
            if (!(e instanceof ExplosionPrimeEvent)) {
                return this.isNegated();
            }
            return ((ExplosionPrimeEvent)e).getFire() ^ this.isNegated();
        }
        return this.entities.check(e, entity -> entity instanceof Explosive && ((Explosive)entity).isIncendiary(), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (this.isEvent) {
            return "the event-explosion " + (!this.isNegated() ? "is" : "is not") + " incendiary";
        }
        if (this.entities.isSingle()) {
            return this.entities.toString(e, debug) + (!this.isNegated() ? " is" : " is not") + " incendiary";
        }
        return this.entities.toString(e, debug) + (!this.isNegated() ? " are" : " are not") + " incendiary";
    }

    static {
        Skript.registerCondition(CondIncendiary.class, "%entities% ((is|are) incendiary|cause[s] a[n] (incendiary|fiery) explosion)", "%entities% ((is not|are not|isn't|aren't) incendiary|(does not|do not|doesn't|don't) cause[s] a[n] (incendiary|fiery) explosion)", "the [event(-| )]explosion (is|1\u00a6(is not|isn't)) (incendiary|fiery)");
    }
}

