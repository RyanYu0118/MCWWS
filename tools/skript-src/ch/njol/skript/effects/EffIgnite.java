/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityCombustEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

@Name(value="Ignite/Extinguish")
@Description(value={"Lights entities on fire or extinguishes them."})
@Example.Examples(value={@Example(value="ignite the player"), @Example(value="extinguish the player")})
@Since(value={"1.4"})
public class EffIgnite
extends Effect {
    private static final int DEFAULT_DURATION = 160;
    @Nullable
    private Expression<Timespan> duration;
    private Expression<Entity> entities;
    private boolean ignite;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        boolean bl = this.ignite = exprs.length > 1;
        if (this.ignite) {
            this.duration = exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        int duration;
        if (this.duration == null) {
            duration = this.ignite ? 160 : 0;
        } else {
            Timespan timespan = this.duration.getSingle(event);
            if (timespan == null) {
                return;
            }
            duration = (int)timespan.getAs(Timespan.TimePeriod.TICK);
        }
        for (final Entity entity : this.entities.getArray(event)) {
            if (event instanceof EntityDamageEvent && ((EntityDamageEvent)event).getEntity() == entity && !Delay.isDelayed(event)) {
                Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), new Runnable(){

                    @Override
                    public void run() {
                        entity.setFireTicks(duration);
                    }
                });
                continue;
            }
            if (event instanceof EntityCombustEvent && ((EntityCombustEvent)event).getEntity() == entity && !Delay.isDelayed(event)) {
                ((EntityCombustEvent)event).setCancelled(true);
            }
            entity.setFireTicks(duration);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.ignite) {
            return "set " + this.entities.toString(event, debug) + " on fire for " + (this.duration != null ? this.duration.toString(event, debug) : new Timespan(Timespan.TimePeriod.TICK, 160L).toString());
        }
        return "extinguish " + this.entities.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffIgnite.class, "(ignite|set fire to) %entities% [for %-timespan%]", "(set|light) %entities% on fire [for %-timespan%]", "extinguish %entities%");
    }
}

