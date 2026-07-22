/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.CommandBlock
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CommandBlock;

@Name(value="Is Conditional")
@Description(value={"Checks whether a command block is conditional or not."})
@Example(value="if {_block} is conditional:\n\tmake {_block} unconditional\n")
@Since(value={"2.10"})
public class CondIsCommandBlockConditional
extends PropertyCondition<Block> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(parseResult.hasTag("un") ^ matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof CommandBlock) {
            CommandBlock cmdBlock = (CommandBlock)blockData;
            return cmdBlock.isConditional();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "conditional";
    }

    static {
        CondIsCommandBlockConditional.register(CondIsCommandBlockConditional.class, "[:un]conditional", "blocks");
    }
}

