/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
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
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Explosion")
@Description(value={"Creates an explosion of a given force. The Minecraft Wiki has an <a href='https://www.minecraft.wiki/w/Explosion'>article on explosions</a> which lists the explosion forces of TNT, creepers, etc.", "Hint: use a force of 0 to create a fake explosion that does no damage whatsoever, or use the explosion effect introduced in Skript 2.0.", "Starting with Bukkit 1.4.5 and Skript 2.0 you can use safe explosions which will damage entities but won't destroy any blocks."})
@Example.Examples(value={@Example(value="create an explosion of force 10 at the player"), @Example(value="create an explosion of force 0 at the victim")})
@Since(value={"1.0"})
public class EffExplosion
extends Effect {
    @Nullable
    private Expression<Number> force;
    private Expression<Location> locations;
    private boolean blockDamage;
    private boolean setFire;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.force = matchedPattern <= 1 ? exprs[0] : null;
        this.blockDamage = matchedPattern != 1;
        this.setFire = parser.mark == 1;
        this.locations = Direction.combine(exprs[exprs.length - 2], exprs[exprs.length - 1]);
        return true;
    }

    @Override
    public void execute(Event e) {
        Integer power;
        Number number = power = this.force != null ? (Number)this.force.getSingle(e) : (Number)0;
        if (power == null) {
            return;
        }
        for (Location location : this.locations.getArray(e)) {
            if (location.getWorld() == null) continue;
            if (!this.blockDamage) {
                location.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), ((Number)power).floatValue(), false, false);
                continue;
            }
            location.getWorld().createExplosion(location, ((Number)power).floatValue(), this.setFire);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (this.force != null) {
            return "create explosion of force " + this.force.toString(e, debug) + " " + this.locations.toString(e, debug);
        }
        return "create explosion effect " + this.locations.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffExplosion.class, "[(create|make)] [an] explosion (of|with) (force|strength|power) %number% [%directions% %locations%] [(1\u00a6with fire)]", "[(create|make)] [a] safe explosion (of|with) (force|strength|power) %number% [%directions% %locations%]", "[(create|make)] [a] fake explosion [%directions% %locations%]", "[(create|make)] [an] explosion[ ]effect [%directions% %locations%]");
    }
}

