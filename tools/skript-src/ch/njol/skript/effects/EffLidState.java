/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Lidded
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
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Open/Close Lid")
@Description(value={"Open or close the lid of the block(s)."})
@Example.Examples(value={@Example(value="open the lid of {_chest}"), @Example(value="close the lid of {_blocks::*}")})
@Since(value={"2.10"})
public class EffLidState
extends Effect {
    private boolean setOpen;
    private Expression<Block> blocks;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setOpen = !parseResult.hasTag("close");
        this.blocks = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Block block : this.blocks.getArray(event)) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof Lidded)) continue;
            Lidded lidded = (Lidded)blockState;
            if (this.setOpen) {
                lidded.open();
                continue;
            }
            lidded.close();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.setOpen ? "open" : "close") + " lid of " + this.blocks.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffLidState.class, "(open|:close) [the] lid[s] (of|for) %blocks%", "(open|:close) %blocks%'[s] lid[s]");
    }
}

