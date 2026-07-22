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
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Lid Is Open/Closed")
@Description(value={"Check to see whether lidded blocks (chests, shulkers, etc.) are open or closed."})
@Example(value="if the lid of {_chest} is closed:\n\topen the lid of {_block}\n")
@Since(value={"2.10"})
public class CondLidState
extends PropertyCondition<Block> {
    private boolean checkOpen;
    private Expression<Block> blocks;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.checkOpen = !parseResult.hasTag("close");
        this.blocks = exprs[0];
        this.setExpr(this.blocks);
        this.setNegated(matchedPattern == 1 || matchedPattern == 3);
        return true;
    }

    @Override
    public boolean check(Block block) {
        Lidded lidded;
        BlockState blockState = block.getState();
        return blockState instanceof Lidded ? (lidded = (Lidded)blockState).isOpen() == this.checkOpen : false;
    }

    @Override
    protected String getPropertyName() {
        return (this.checkOpen ? "opened" : "closed") + " lid state";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the lids of " + this.blocks.toString(event, debug) + (this.isNegated() ? "are not " : "are ") + (this.checkOpen ? "opened" : "closed");
    }

    static {
        Skript.registerCondition(CondLidState.class, Condition.ConditionType.PROPERTY, "[the] lid[s] of %blocks% (is|are) (open[ed]|:close[d])", "[the] lid[s] of %blocks% (isn't|is not|aren't|are not) (open[ed]|:close[d])", "%blocks%'[s] lid[s] (is|are) (open[ed]|:close[d])", "%blocks%'[s] lid[s] (isn't|is not|aren't|are not) (open[ed]|:close[d])");
    }
}

