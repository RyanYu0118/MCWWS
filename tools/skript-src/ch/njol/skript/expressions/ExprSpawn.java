/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.bukkit.event.world.SpawnChangeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.world.SpawnChangeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Spawn")
@Description(value={"The spawn point of a world."})
@Example.Examples(value={@Example(value="teleport all players to spawn"), @Example(value="set the spawn point of \"world\" to the player's location")})
@Since(value={"1.4.2"})
public class ExprSpawn
extends PropertyExpression<World, Location> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected Location[] get(Event event, World[] source) {
        if (this.getTime() == -1 && event instanceof SpawnChangeEvent && !Delay.isDelayed(event)) {
            return new Location[]{((SpawnChangeEvent)event).getPreviousLocation()};
        }
        return this.get(source, World::getSpawnLocation);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Location.class);
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        Location originalLocation = (Location)delta[0];
        assert (originalLocation != null);
        for (World world : (World[])this.getExpr().getArray(event)) {
            Location location = originalLocation.clone();
            World locationWorld = location.getWorld();
            if (locationWorld == null) {
                location.setWorld(world);
                world.setSpawnLocation(location);
                continue;
            }
            if (!locationWorld.equals((Object)world)) continue;
            world.setSpawnLocation(location);
        }
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, this.getExpr(), SpawnChangeEvent.class);
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the spawn point of " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprSpawn.class, Location.class, ExpressionType.PROPERTY, "[the] spawn[s] [(point|location)[s]] [of %worlds%]", "%worlds%'[s] spawn[s] [(point|location)[s]]");
    }
}

