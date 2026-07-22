/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Chunk")
@Description(value={"Returns the <a href='#chunk'>chunk</a> of a block, location or entity is in, or a list of the loaded chunks of a world."})
@Example.Examples(value={@Example(value="add the chunk at the player to {protected chunks::*}"), @Example(value="set {_chunks::*} to the loaded chunks of the player's world")})
@Since(value={"2.0, 2.8.0 (loaded chunks)"})
public class ExprChunk
extends SimpleExpression<Chunk> {
    private int pattern;
    private Expression<Location> locations;
    private Expression<World> worlds;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.pattern = matchedPattern;
        if (this.pattern == 0) {
            this.locations = exprs[1];
            if (exprs[0] != null) {
                this.locations = Direction.combine(exprs[0], this.locations);
            }
        } else if (this.pattern == 1) {
            this.locations = exprs[0];
        } else {
            this.worlds = exprs[0];
        }
        return true;
    }

    protected Chunk[] get(Event event) {
        if (this.pattern != 2) {
            return (Chunk[])this.locations.stream(event).map(Location::getChunk).toArray(Chunk[]::new);
        }
        return (Chunk[])this.worlds.stream(event).flatMap(world -> Arrays.stream(world.getLoadedChunks())).toArray(Chunk[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.RESET) {
            return new Class[0];
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (mode == Changer.ChangeMode.RESET);
        for (Chunk chunk : this.get(event)) {
            chunk.getWorld().regenerateChunk(chunk.getX(), chunk.getZ());
        }
    }

    @Override
    public boolean isSingle() {
        if (this.pattern == 2) {
            return false;
        }
        return this.locations.isSingle();
    }

    @Override
    public Class<? extends Chunk> getReturnType() {
        return Chunk.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.pattern == 2) {
            return "loaded chunks of " + this.worlds.toString(event, debug);
        }
        return "chunk at " + this.locations.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprChunk.class, Chunk.class, ExpressionType.COMBINED, "[(all [[of] the]|the)] chunk[s] (of|%-directions%) %locations%", "%locations%'[s] chunk[s]", "[(all [[of] the]|the)] loaded chunks (of|in) %worlds%");
    }
}

