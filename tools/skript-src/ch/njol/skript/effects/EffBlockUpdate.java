/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
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
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Update Block")
@Description(value={"Updates the blocks by setting them to a selected block", "Using 'without physics' will not send updates to the surrounding blocks of the blocks being set.", "Example: Updating a block next to a sand block in the air 'without physics' will not cause the sand block to fall."})
@Example.Examples(value={@Example(value="update {_blocks::*} as gravel"), @Example(value="update {_blocks::*} to be sand without physics updates"), @Example(value="update {_blocks::*} as stone without neighbouring updates")})
@Since(value={"2.10"})
public class EffBlockUpdate
extends Effect {
    private boolean physics;
    private Expression<Block> blocks;
    private Expression<BlockData> blockData;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.physics = !parseResult.hasTag("physics");
        this.blocks = exprs[0];
        this.blockData = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        BlockData data = this.blockData.getSingle(event);
        if (data == null) {
            return;
        }
        for (Block block : this.blocks.getArray(event)) {
            block.setBlockData(data, this.physics);
        }
    }

    @Override
    @NotNull
    public String toString(@Nullable Event event, boolean debug) {
        return "update " + this.blocks.toString(event, debug) + " as " + this.blockData.toString(event, debug) + (this.physics ? "without neighbour updates" : "");
    }

    static {
        Skript.registerEffect(EffBlockUpdate.class, "update %blocks% (as|to be) %blockdata% [physics:without [neighbo[u]r[ing]|adjacent] [physics] update[s]]");
    }
}

