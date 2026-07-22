/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Warden
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Warden;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Disturbance")
@Description(value={"Make a warden sense a disturbance at a location, causing the warden to investigate that area.", "The warden will not investigate if the warden is aggressive towards an entity.", "This effect does not add anger to the warden."})
@Example(value="make last spawned warden sense a disturbance at location(0, 0, 0)")
@Since(value={"2.11"})
public class EffWardenDisturbance
extends Effect {
    private Expression<LivingEntity> wardens;
    private Expression<Location> location;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.wardens = exprs[0];
        Expression<?> direction = exprs[1];
        Expression<?> location = exprs[2];
        this.location = Direction.combine(direction, location);
        return true;
    }

    @Override
    protected void execute(Event event) {
        Location finalLocation = this.location.getSingle(event);
        if (finalLocation == null) {
            return;
        }
        for (LivingEntity livingEntity : this.wardens.getArray(event)) {
            if (!(livingEntity instanceof Warden)) continue;
            Warden warden = (Warden)livingEntity;
            warden.setDisturbanceLocation(finalLocation);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.wardens.toString(event, debug) + " sense a disturbance " + this.location.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffWardenDisturbance.class, "make %livingentities% sense [a] disturbance %direction% %location%");
    }
}

