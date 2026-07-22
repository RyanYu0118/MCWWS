/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.entity.LookAnchor
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.PaperEntityUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Look At")
@Description(value={"Forces the mob(s) or player(s) to look at an entity, vector or location. Vanilla max head pitches range from 10 to 50."})
@Example.Examples(value={@Example(value="force the player to look towards event-entity's feet"), @Example(value="on entity explosion:\n\tset {_player} to the nearest player\n\t{_player} is set\n\tdistance between {_player} and the event-location is less than 15\n\tmake {_player} look towards vector from the {_player} to location of the event-entity\n"), @Example(value="force {_enderman} to face the block 3 meters above {_location} at head rotation speed 100.5 and max head pitch -40")})
@Since(value={"2.7"})
public class EffLook
extends Effect {
    private LookAnchor anchor = LookAnchor.EYES;
    private Expression<LivingEntity> entities;
    @Nullable
    private Expression<Number> speed;
    @Nullable
    private Expression<Number> maxPitch;
    private Expression<?> target;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.target = exprs[1];
        this.speed = exprs[2];
        this.maxPitch = exprs[3];
        if (parseResult.hasTag("feet")) {
            this.anchor = LookAnchor.FEET;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Object object = this.target.getSingle(event);
        if (object == null) {
            return;
        }
        Float speed = this.speed == null ? null : (Float)this.speed.getOptionalSingle(event).map(Number::floatValue).orElse(null);
        Float maxPitch = this.maxPitch == null ? null : (Float)this.maxPitch.getOptionalSingle(event).map(Number::floatValue).orElse(null);
        PaperEntityUtils.lookAt(this.anchor, object, speed, maxPitch, this.entities.getArray(event));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "force " + this.entities.toString(event, debug) + " to look at " + this.target.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffLook.class, "(force|make) %livingentities% [to] (face [towards]|look [(at|towards)]) %entity%'s (feet:feet|eyes) [(at|with) [head] [rotation] speed %-number%] [[and] max[imum] [head] pitch %-number%]", "(force|make) %livingentities% [to] (face [towards]|look [(at|towards)]) [the] (feet:feet|eyes) of %entity% [(at|with) [head] [rotation] speed %-number%] [[and] max[imum] [head] pitch %-number%]", "(force|make) %livingentities% [to] (face [towards]|look [(at|towards)]) %vector/location/entity% [(at|with) [head] [rotation] speed %-number%] [[and] max[imum] [head] pitch %-number%]");
    }
}

