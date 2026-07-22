/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Creeper
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Firework
 *  org.bukkit.entity.TNTPrimed
 *  org.bukkit.entity.WindCharge
 *  org.bukkit.entity.minecart.ExplosiveMinecart
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
import ch.njol.util.Kleenean;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Detonate Entities")
@Description(value={"Immediately detonates an entity. Accepted entities are fireworks, TNT minecarts, primed TNT, wind charges and creepers."})
@Example(value="detonate last launched firework")
@Since(value={"2.10"})
public class EffDetonate
extends Effect {
    private static final boolean HAS_WINDCHARGE = Skript.classExists("org.bukkit.entity.WindCharge");
    private Expression<Entity> entities;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Entity entity : this.entities.getArray(event)) {
            if (entity instanceof Firework) {
                Firework firework = (Firework)entity;
                firework.detonate();
                continue;
            }
            if (HAS_WINDCHARGE && entity instanceof WindCharge) {
                WindCharge windCharge = (WindCharge)entity;
                windCharge.explode();
                continue;
            }
            if (entity instanceof ExplosiveMinecart) {
                ExplosiveMinecart explosiveMinecart = (ExplosiveMinecart)entity;
                explosiveMinecart.explode();
                continue;
            }
            if (entity instanceof Creeper) {
                Creeper creeper = (Creeper)entity;
                creeper.explode();
                continue;
            }
            if (!(entity instanceof TNTPrimed)) continue;
            TNTPrimed tntPrimed = (TNTPrimed)entity;
            tntPrimed.setFuseTicks(0);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "detonate " + this.entities.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffDetonate.class, "detonate %entities%");
    }
}

