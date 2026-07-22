/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Riding")
@Description(value={"Tests whether an entity is riding any entity, a specific entity type, or a specific entity."})
@Example.Examples(value={@Example(value="if player is riding:"), @Example(value="if player is riding an entity:"), @Example(value="if player is riding a saddled pig:"), @Example(value="if player is riding last spawned horse:")})
@Since(value={"2.0, 2.11 (entities)"})
public class CondIsRiding
extends Condition {
    private Expression<Entity> riders;
    @Nullable
    private Expression<?> riding;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.riders = exprs[0];
        this.riding = exprs[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.riding == null) {
            return this.riders.check(event, rider -> rider.getVehicle() != null, this.isNegated());
        }
        Object[] riding = this.riding.getArray(event);
        return this.riders.check(event, rider -> {
            Entity vehicle = rider.getVehicle();
            if (vehicle == null) {
                return false;
            }
            return SimpleExpression.check(riding, object -> {
                if (object instanceof EntityData) {
                    EntityData entityData = (EntityData)object;
                    return entityData.isInstance(vehicle);
                }
                if (object instanceof Entity) {
                    Entity entity = (Entity)object;
                    return vehicle == entity;
                }
                return false;
            }, false, false);
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Object property = "riding";
        if (this.riding != null) {
            property = (String)property + " " + this.riding.toString(event, debug);
        }
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, this.riders, (String)property);
    }

    static {
        PropertyCondition.register(CondIsRiding.class, "riding [%-entitydatas/entities%]", "entities");
    }
}

