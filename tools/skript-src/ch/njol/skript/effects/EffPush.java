/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
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
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Push")
@Description(value={"Push entities in a given direction or towards a specific location."})
@Example.Examples(value={@Example(value="push the player upwards"), @Example(value="push the victim downwards at speed 0.5"), @Example(value="push player towards player's target at speed 2"), @Example(value="pull player along vector(1,1,1) at speed 1.5")})
@Since(value={"1.4.6", "2.12 (push towards)"})
public class EffPush
extends Effect {
    private Expression<Entity> entities;
    @Nullable
    private Expression<Direction> direction;
    @Nullable
    private Expression<Location> target;
    private boolean awayFrom = false;
    @Nullable
    private Expression<Number> speed = null;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        if (matchedPattern == 0) {
            this.direction = exprs[1];
        } else {
            this.target = exprs[1];
            this.awayFrom = parseResult.hasTag("away");
        }
        this.speed = exprs[2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Entity[] entities;
        Function<Entity, Vector> getDirection;
        Number speed;
        Number number = speed = this.speed != null ? (Number)this.speed.getSingle(event) : (Number)null;
        if (this.speed != null && speed == null) {
            return;
        }
        if (this.direction != null) {
            Direction direction = this.direction.getSingle(event);
            if (direction == null) {
                return;
            }
            getDirection = direction::getDirection;
        } else {
            assert (this.target != null);
            Location target = this.target.getSingle(event);
            if (target == null) {
                return;
            }
            Vector targetVector = target.toVector();
            getDirection = entity -> {
                Vector direction = targetVector.subtract(entity.getLocation().toVector());
                if (this.awayFrom) {
                    direction.multiply(-1);
                }
                return direction;
            };
        }
        for (Entity entity2 : entities = this.entities.getArray(event)) {
            Vector pushDirection = getDirection.apply(entity2);
            if (speed != null) {
                pushDirection.normalize().multiply(speed.doubleValue());
            }
            if (!(Double.isFinite(pushDirection.getX()) && Double.isFinite(pushDirection.getY()) && Double.isFinite(pushDirection.getZ()))) {
                return;
            }
            entity2.setVelocity(entity2.getVelocity().add(pushDirection));
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder ssb = new SyntaxStringBuilder(event, debug).append("push", this.entities);
        if (this.direction != null) {
            ssb.append((Object)this.direction);
        } else {
            assert (this.target != null);
            if (this.awayFrom) {
                ssb.append("away from", this.target);
            } else {
                ssb.append("towards", this.target);
            }
        }
        if (this.speed != null) {
            ssb.append("at a speed of", this.speed);
        }
        return ssb.toString();
    }

    static {
        Skript.registerEffect(EffPush.class, "(push|thrust|pull) %entities% [along] %direction% [(at|with) [a] (speed|velocity|force) [of] %-number%]", "(push|thrust|pull) %entities% (towards|away:away from) %location% [(at|with) [a] (speed|velocity|force) [of] %-number%]");
    }
}

