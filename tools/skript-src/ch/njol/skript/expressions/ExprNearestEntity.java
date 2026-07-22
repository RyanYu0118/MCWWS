/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import java.lang.reflect.Array;
import java.util.Arrays;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Nearest Entity")
@Description(value={"Gets the entity nearest to a location or another entity."})
@Example.Examples(value={@Example(value="kill the nearest pig and cow relative to player"), @Example(value="teleport player to the nearest cow relative to player"), @Example(value="teleport player to the nearest entity relative to player"), @Example(value="on click:\n\tkill nearest pig\n")})
@Since(value={"2.7"})
public class ExprNearestEntity
extends SimpleExpression<Entity> {
    private EntityData<?>[] entityDatas;
    private Expression<?> relativeTo;
    @Nullable
    private transient Class<? extends Entity> knownReturnType;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entityDatas = (EntityData[])((Literal)exprs[0]).getArray();
        if ((long)this.entityDatas.length != Arrays.stream(this.entityDatas).distinct().count()) {
            Skript.error("Entity list may not contain duplicate entities");
            return false;
        }
        this.relativeTo = exprs[1];
        return true;
    }

    protected Entity[] get(Event event) {
        Object relativeTo = this.relativeTo.getSingle(event);
        if (relativeTo == null || relativeTo instanceof Location && ((Location)relativeTo).getWorld() == null) {
            return (Entity[])Array.newInstance(this.getReturnType(), 0);
        }
        Entity[] nearestEntities = (Entity[])Array.newInstance(this.getReturnType(), this.entityDatas.length);
        for (int i = 0; i < nearestEntities.length; ++i) {
            nearestEntities[i] = relativeTo instanceof Entity ? this.getNearestEntity(this.entityDatas[i], ((Entity)relativeTo).getLocation(), (Entity)relativeTo) : this.getNearestEntity(this.entityDatas[i], (Location)relativeTo, null);
        }
        return nearestEntities;
    }

    @Override
    public boolean isSingle() {
        return this.entityDatas.length == 1;
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        if (this.knownReturnType != null) {
            return this.knownReturnType;
        }
        Class[] types = new Class[this.entityDatas.length];
        for (int i = 0; i < types.length; ++i) {
            types[i] = this.entityDatas[i].getType();
        }
        this.knownReturnType = Utils.highestDenominator(Entity.class, types);
        return this.knownReturnType;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "nearest " + StringUtils.join(this.entityDatas) + " relative to " + this.relativeTo.toString(event, debug);
    }

    @Nullable
    private Entity getNearestEntity(EntityData<?> entityData, Location relativePoint, @Nullable Entity excludedEntity) {
        Entity nearestEntity = null;
        double nearestDistance = -1.0;
        for (Entity entity : relativePoint.getWorld().getEntitiesByClass(entityData.getType())) {
            if (entity == excludedEntity || !entityData.isInstance(entity)) continue;
            double distance = entity.getLocation().distance(relativePoint);
            if (nearestEntity != null && !(distance < nearestDistance)) continue;
            nearestDistance = distance;
            nearestEntity = entity;
        }
        return nearestEntity;
    }

    static {
        Skript.registerExpression(ExprNearestEntity.class, Entity.class, ExpressionType.COMBINED, "[the] (nearest|closest) %*entitydatas% [[relative] to %entity/location%]", "[the] %*entitydatas% (nearest|closest) [to %entity/location%]");
    }
}

