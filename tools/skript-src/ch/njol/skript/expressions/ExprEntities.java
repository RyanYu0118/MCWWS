/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.util.BoundingBox
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
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
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.iterator.CheckedIterator;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Entities")
@Description(value={"All entities in all worlds, in a specific world, in a chunk, in a radius around a certain location or within two locations. e.g. <code>all players</code>, <code>all creepers in the player's world</code>, or <code>players in radius 100 of the player</code>."})
@Example.Examples(value={@Example(value="kill all creepers in the player's world"), @Example(value="send \"Psst!\" to all players within 100 meters of the player"), @Example(value="give a diamond to all ops"), @Example(value="heal all tamed wolves in radius 2000 around {town center}"), @Example(value="delete all monsters in chunk at player"), @Example(value="size of all players within {_corner::1} and {_corner::2}}")})
@Since(value={"1.2.1, 2.5 (chunks), 2.10 (within)"})
public class ExprEntities
extends SimpleExpression<Entity> {
    Expression<? extends EntityData<?>> types;
    private @UnknownNullability Expression<?> worldsOrChunks;
    private @UnknownNullability Expression<Number> radius;
    private @UnknownNullability Expression<Location> center;
    private @UnknownNullability Expression<Location> from;
    private @UnknownNullability Expression<Location> to;
    private Class<? extends Entity> returnType = Entity.class;
    private boolean isUsingRadius;
    private boolean isUsingCuboid;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.types = exprs[0];
        if (matchedPattern % 2 == 0) {
            for (EntityData entityType : (EntityData[])((Literal)this.types).getAll()) {
                if (!entityType.isPlural().isFalse() && (!entityType.isPlural().isUnknown() || StringUtils.startsWithIgnoreCase(parseResult.expr, "all"))) continue;
                return false;
            }
        }
        this.isUsingRadius = matchedPattern == 2 || matchedPattern == 3;
        boolean bl = this.isUsingCuboid = matchedPattern >= 4;
        if (this.isUsingRadius) {
            this.radius = exprs[1];
            this.center = exprs[2];
        } else if (this.isUsingCuboid) {
            this.from = exprs[1];
            this.to = exprs[2];
        } else {
            this.worldsOrChunks = parseResult.mark == 1 ? exprs[2] : exprs[1];
        }
        if (this.types instanceof Literal && ((EntityData[])((Literal)this.types).getAll()).length == 1) {
            this.returnType = ((EntityData)((Literal)this.types).getSingle()).getType();
        }
        return true;
    }

    @Override
    public boolean isLoopOf(String s) {
        if (!(this.types instanceof Literal)) {
            return false;
        }
        try (BlockingLogHandler ignored = new BlockingLogHandler().start();){
            EntityData<?> entityData = EntityData.parseWithoutIndefiniteArticle(s);
            if (entityData != null) {
                for (EntityData entityType : (EntityData[])((Literal)this.types).getAll()) {
                    assert (entityType != null);
                    if (entityData.isSupertypeOf(entityType)) continue;
                    boolean bl = false;
                    return bl;
                }
                boolean bl = true;
                return bl;
            }
        }
        return false;
    }

    protected Entity @Nullable [] get(Event event) {
        if (this.isUsingRadius || this.isUsingCuboid) {
            Iterator<? extends Entity> iter = this.iterator(event);
            if (iter == null || !iter.hasNext()) {
                return null;
            }
            ArrayList<Entity> list = new ArrayList<Entity>();
            while (iter.hasNext()) {
                list.add(iter.next());
            }
            return list.toArray((Entity[])Array.newInstance(this.returnType, list.size()));
        }
        EntityData[] types = this.types.getAll(event);
        if (this.worldsOrChunks == null) {
            return EntityData.getAll((EntityData[])types, this.returnType, (World[])null);
        }
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        ArrayList<World> worlds = new ArrayList<World>();
        for (Object obj : this.worldsOrChunks.getArray(event)) {
            if (obj instanceof Chunk) {
                Chunk chunk = (Chunk)obj;
                chunks.add(chunk);
                continue;
            }
            if (!(obj instanceof World)) continue;
            World world = (World)obj;
            worlds.add(world);
        }
        HashSet<Entity> entities = new HashSet<Entity>();
        if (!chunks.isEmpty()) {
            entities.addAll(Arrays.asList(EntityData.getAll((EntityData[])types, this.returnType, (Chunk[])chunks.toArray(new Chunk[0]))));
        }
        if (!worlds.isEmpty()) {
            entities.addAll(Arrays.asList(EntityData.getAll((EntityData[])types, this.returnType, (World[])worlds.toArray(new World[0]))));
        }
        return entities.toArray((Entity[])Array.newInstance(this.returnType, entities.size()));
    }

    @Override
    @Nullable
    public Iterator<? extends Entity> iterator(Event event) {
        if (this.isUsingRadius) {
            Location location = this.center.getSingle(event);
            if (location == null) {
                return null;
            }
            Number number = this.radius.getSingle(event);
            if (number == null) {
                return null;
            }
            double rad = number.doubleValue();
            if (location.getWorld() == null) {
                return null;
            }
            Collection nearbyEntities = location.getWorld().getNearbyEntities(location, rad, rad, rad);
            double radiusSquared = rad * rad * 1.00001;
            EntityData[] entityTypes = this.types.getAll(event);
            return new CheckedIterator<Entity>(nearbyEntities.iterator(), entity -> {
                if (entity == null || entity.getLocation().distanceSquared(location) > radiusSquared) {
                    return false;
                }
                for (EntityData entityType : entityTypes) {
                    if (!entityType.isInstance((Entity)entity)) continue;
                    return true;
                }
                return false;
            });
        }
        if (this.isUsingCuboid) {
            Location corner1 = this.from.getSingle(event);
            if (corner1 == null) {
                return null;
            }
            Location corner2 = this.to.getSingle(event);
            if (corner2 == null) {
                return null;
            }
            EntityData[] entityTypes = this.types.getAll(event);
            World world = corner1.getWorld();
            if (world == null) {
                world = corner2.getWorld();
            }
            if (world == null) {
                return null;
            }
            Collection entities = corner1.getWorld().getNearbyEntities(BoundingBox.of((Location)corner1, (Location)corner2));
            return new CheckedIterator<Entity>(entities.iterator(), entity -> {
                if (entity == null) {
                    return false;
                }
                for (EntityData entityType : entityTypes) {
                    if (!entityType.isInstance((Entity)entity)) continue;
                    return true;
                }
                return false;
            });
        }
        return super.iterator(event);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return this.returnType;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String message = "all entities of type " + this.types.toString(event, debug);
        if (this.worldsOrChunks != null) {
            message = message + " in " + this.worldsOrChunks.toString(event, debug);
        } else if (this.radius != null && this.center != null) {
            message = message + " in radius " + this.radius.toString(event, debug) + " around " + this.center.toString(event, debug);
        } else if (this.from != null && this.to != null) {
            message = message + " within " + this.from.toString(event, debug) + " and " + this.to.toString(event, debug);
        }
        return message;
    }

    static {
        Skript.registerExpression(ExprEntities.class, Entity.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "[(all [[of] the]|the)] %*entitydatas% [(in|of) (world[s] %-worlds%|1:%-worlds/chunks%)]", "[(all [[of] the]|the)] entities of type[s] %entitydatas% [(in|of) (world[s] %-worlds%|1:%-worlds/chunks%)]", "[(all [[of] the]|the)] %*entitydatas% (within|[with]in radius) %number% [(block[s]|met(er|re)[s])] (of|around) %location%", "[(all [[of] the]|the)] entities of type[s] %entitydatas% in radius %number% (of|around) %location%", "[(all [[of] the]|the)] %*entitydatas% within %location% and %location%", "[(all [[of] the]|the)] entities of type[s] %entitydatas% within %location% and %location%");
    }
}

