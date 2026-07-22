/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Bell
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.BlockState
 *  org.bukkit.entity.Entity
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
import org.bukkit.block.Bell;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Ring Bell")
@Description(value={"Causes a bell to ring.", "Optionally, the entity that rang the bell and the direction the bell should ring can be specified.", "A bell can only ring in two directions, and the direction is determined by which way the bell is facing.", "By default, the bell will ring in the direction it is facing."})
@Example(value="make player ring target-block")
@Since(value={"2.9.0"})
public class EffRing
extends Effect {
    @Nullable
    private Expression<Entity> entity;
    private Expression<Block> blocks;
    @Nullable
    private Expression<Direction> direction;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entity = matchedPattern == 0 ? null : exprs[0];
        this.blocks = exprs[matchedPattern];
        this.direction = exprs[matchedPattern + 1];
        return true;
    }

    @Nullable
    private BlockFace getBlockFace(Event event) {
        if (this.direction == null) {
            return null;
        }
        Direction direction = this.direction.getSingle(event);
        if (direction == null) {
            return null;
        }
        return Direction.getFacing(direction.getDirection(), true);
    }

    @Override
    protected void execute(Event event) {
        BlockFace blockFace = this.getBlockFace(event);
        Entity actualEntity = this.entity == null ? null : this.entity.getSingle(event);
        for (Block block : this.blocks.getArray(event)) {
            BlockState state = block.getState(false);
            if (!(state instanceof Bell)) continue;
            Bell bell = (Bell)state;
            bell.ring(actualEntity, blockFace);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (String)(this.entity != null ? "make " + this.entity.toString(event, debug) + " " : "") + "ring " + this.blocks.toString(event, debug) + " from " + (this.direction != null ? this.direction.toString(event, debug) : "");
    }

    static {
        if (Skript.classExists("org.bukkit.block.Bell") && Skript.methodExists(Bell.class, "ring", Entity.class, BlockFace.class)) {
            Skript.registerEffect(EffRing.class, "ring %blocks% [from [the]] [%-direction%]", "(make|let) %entity% ring %blocks% [from [the]] [%-direction%]");
        }
    }
}

