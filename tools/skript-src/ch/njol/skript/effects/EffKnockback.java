/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Knockback")
@Description(value={"Apply the same velocity as a knockback to living entities in a direction. Mechanics such as knockback resistance will be factored in."})
@Example.Examples(value={@Example(value="knockback player north"), @Example(value="knock victim (vector from attacker to victim) with strength 10")})
@Since(value={"2.7"})
public class EffKnockback
extends Effect {
    private Expression<LivingEntity> entities;
    private Expression<Direction> direction;
    @Nullable
    private Expression<Number> strength;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.direction = exprs[1];
        this.strength = exprs[2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Direction direction = this.direction.getSingle(event);
        if (direction == null) {
            return;
        }
        double strength = this.strength != null ? this.strength.getOptionalSingle(event).orElse(1).doubleValue() : 1.0;
        for (LivingEntity livingEntity : this.entities.getArray(event)) {
            Vector directionVector = direction.getDirection((Entity)livingEntity);
            directionVector.multiply(-1);
            livingEntity.knockback(strength, directionVector.getX(), directionVector.getZ());
            livingEntity.setVelocity(livingEntity.getVelocity());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "knockback " + this.entities.toString(event, debug) + " " + this.direction.toString(event, debug) + " with strength " + (this.strength != null ? this.strength.toString(event, debug) : "1");
    }

    static {
        if (Skript.methodExists(LivingEntity.class, "knockback", Double.TYPE, Double.TYPE, Double.TYPE)) {
            Skript.registerEffect(EffKnockback.class, "(apply knockback to|knock[back]) %livingentities% [%direction%] [with (strength|force) %-number%]");
        }
    }
}

