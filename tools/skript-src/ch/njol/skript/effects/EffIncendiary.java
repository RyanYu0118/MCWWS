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
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Incendiary")
@Description(value={"Sets if an entity's explosion will leave behind fire. This effect is also usable in an explosion prime event."})
@Example(value="on explosion prime:\n\tmake the explosion fiery\n")
@Since(value={"2.5"})
public class EffIncendiary
extends Effect {
    private Expression<Entity> entities;
    private boolean causeFire;
    private boolean isEvent;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean bl = this.isEvent = matchedPattern == 2;
        if (this.isEvent && !this.getParser().isCurrentEvent((Class<? extends Event>)ExplosionPrimeEvent.class)) {
            Skript.error("Making 'the explosion' fiery is only usable in an explosion prime event", ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        if (!this.isEvent) {
            this.entities = exprs[0];
        }
        this.causeFire = parseResult.mark != 1;
        return true;
    }

    @Override
    protected void execute(Event e) {
        if (this.isEvent) {
            if (!(e instanceof ExplosionPrimeEvent)) {
                return;
            }
            ((ExplosionPrimeEvent)e).setFire(this.causeFire);
        } else {
            for (Entity entity : this.entities.getArray(e)) {
                if (!(entity instanceof Explosive)) continue;
                ((Explosive)entity).setIsIncendiary(this.causeFire);
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (this.isEvent) {
            return "make the event-explosion " + (this.causeFire ? "" : "not") + " fiery";
        }
        return "make " + this.entities.toString(e, debug) + (this.causeFire ? "" : " not") + " incendiary";
    }

    static {
        Skript.registerEffect(EffIncendiary.class, "make %entities% [(1\u00a6not)] incendiary", "make %entities%'[s] explosion [(1\u00a6not)] (incendiary|fiery)", "make [the] [event(-| )]explosion [(1\u00a6not)] (incendiary|fiery)");
    }
}

