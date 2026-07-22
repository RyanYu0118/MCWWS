/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Allay
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Piglin
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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Dance")
@Description(value={"Make an allay or piglin start or stop dancing.", "Providing a location only applies to allays. They will check to see if the the block at the location is a jukebox and playing music. If it isn't, they will stop dancing. If no location is provided, the allay will dance indefinitely.", "Providing a timespan only applies for piglins. It determines the length of time they will dance for. If no timespan is provided, they will dance indefinitely."})
@Example.Examples(value={@Example(value="if last spawned allay is not dancing:\n\tmake last spawned allay start dancing\n"), @Example(value="if block at location(0, 0, 0) is a jukebox:\n\tmake all allays dance at location(0, 0, 0)\n"), @Example(value="make last spawned piglin start dancing"), @Example(value="make all piglins dance for 5 hours")})
@Since(value={"2.11"})
public class EffDancing
extends Effect {
    private static final boolean SUPPORTS_PIGLINS = Skript.methodExists(Piglin.class, "setDancing", Boolean.TYPE);
    private Expression<LivingEntity> entities;
    private boolean start;
    @Nullable
    private Expression<Location> location;
    @Nullable
    private Expression<Timespan> timespan;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        boolean bl = this.start = matchedPattern == 0;
        if (this.start && exprs[1] != null) {
            this.location = Direction.combine(exprs[1], exprs[2]);
        }
        if (parseResult.hasTag("timespan")) {
            this.timespan = exprs[3];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Timespan timespan1;
        Location location = null;
        long time = 0L;
        if (this.location != null) {
            location = this.location.getSingle(event);
        }
        if (this.timespan != null && (timespan1 = this.timespan.getSingle(event)) != null) {
            time = timespan1.getAs(Timespan.TimePeriod.TICK);
        }
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (entity instanceof Allay) {
                Allay allay = (Allay)entity;
                if (!this.start) {
                    allay.stopDancing();
                    continue;
                }
                if (location != null) {
                    allay.startDancing(location);
                    continue;
                }
                allay.startDancing();
                continue;
            }
            if (!SUPPORTS_PIGLINS || !(entity instanceof Piglin)) continue;
            Piglin piglin = (Piglin)entity;
            if (!this.start) {
                piglin.setDancing(false);
                continue;
            }
            if (time > 0L) {
                piglin.setDancing(time);
                continue;
            }
            piglin.setDancing(true);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.entities);
        if (this.start) {
            builder.append((Object)"start");
        } else {
            builder.append((Object)"stop");
        }
        builder.append((Object)"dancing");
        if (this.location != null) {
            builder.append((Object)this.location);
        }
        if (this.timespan != null) {
            builder.append("for", this.timespan);
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffDancing.class, "make %livingentities% (start dancing|dance) [%-direction% %-location%] [timespan:for %-timespan%]", "make %livingentities% (stop dancing|not dance)");
    }
}

