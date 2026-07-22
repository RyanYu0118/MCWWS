/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.CommandBlock
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
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CommandBlock;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Conditional / Unconditional")
@Description(value={"Sets whether the provided command blocks are conditional or not."})
@Example.Examples(value={@Example(value="make command block {_block} conditional"), @Example(value="make command block {_block} unconditional if {_block} is conditional")})
@Since(value={"2.10"})
public class EffCommandBlockConditional
extends Effect {
    private Expression<Block> blocks;
    private boolean conditional;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.blocks = exprs[0];
        this.conditional = !parseResult.hasTag("not");
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Block block : this.blocks.getArray(event)) {
            BlockData blockData = block.getBlockData();
            if (!(blockData instanceof CommandBlock)) continue;
            CommandBlock cmdBlock = (CommandBlock)blockData;
            cmdBlock.setConditional(this.conditional);
            block.setBlockData((BlockData)cmdBlock);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make command block " + this.blocks.toString(event, debug) + (this.conditional ? " " : " un") + "conditional";
    }

    static {
        Skript.registerEffect(EffCommandBlockConditional.class, "make command block[s] %blocks% [not:(un|not )]conditional");
    }
}

