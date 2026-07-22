/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="World")
@Description(value={"The world the event occurred in."})
@Example.Examples(value={@Example(value="world is \"world_nether\""), @Example(value="teleport the player to the world's spawn"), @Example(value="set the weather in the player's world to rain"), @Example(value="set {_world} to world of event-chunk")})
@Since(value={"1.0"})
public class ExprWorld
extends PropertyExpression<Object, World> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected World[] get(Event event, Object[] source) {
        return this.get(source, objInWorld -> {
            if (event instanceof PlayerTeleportEvent) {
                PlayerTeleportEvent playerTeleportEvent = (PlayerTeleportEvent)event;
                if (this.getTime() == EventValues.TIME_FUTURE) {
                    return playerTeleportEvent.getTo().getWorld();
                }
                if (this.getTime() == EventValues.TIME_PAST) {
                    return playerTeleportEvent.getFrom().getWorld();
                }
            }
            if (objInWorld instanceof Entity) {
                Entity entity = (Entity)objInWorld;
                return entity.getWorld();
            }
            if (objInWorld instanceof Location) {
                Location location = (Location)objInWorld;
                return location.getWorld();
            }
            if (objInWorld instanceof Chunk) {
                Chunk chunk = (Chunk)objInWorld;
                return chunk.getWorld();
            }
            assert (false) : objInWorld;
            return null;
        });
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET && this.getExpr().canReturn(Location.class)) {
            return CollectionUtils.array(World.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        for (Object object : this.getExpr().getArray(event)) {
            if (!(object instanceof Location)) continue;
            Location location = (Location)object;
            location.setWorld((World)delta[0]);
        }
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, this.getExpr(), PlayerTeleportEvent.class);
    }

    @Override
    public Class<World> getReturnType() {
        return World.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the world" + (String)(this.getExpr().isDefault() ? "" : " of " + this.getExpr().toString(event, debug));
    }

    static {
        Skript.registerExpression(ExprWorld.class, World.class, ExpressionType.PROPERTY, "[the] world [of %locations/entities/chunk%]", "%locations/entities/chunk%'[s] world");
    }
}

