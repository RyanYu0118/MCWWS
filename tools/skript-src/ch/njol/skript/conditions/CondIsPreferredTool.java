/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Preferred Tool")
@Description(value={"Checks whether an item is the preferred tool for a block. A preferred tool is one that will drop the block's item when used. For example, a wooden pickaxe is a preferred tool for grass and stone blocks, but not for iron ore."})
@Example(value="on left click:\n\tevent-block is set\n\tif player's tool is the preferred tool for event-block:\n\t\tbreak event-block naturally using player's tool\n\telse:\n\t\tcancel event\n")
@Since(value={"2.7"})
@RequiredPlugins(value={"1.16.5+, Paper 1.19.2+ (blockdata)"})
public class CondIsPreferredTool
extends Condition {
    private Expression<ItemType> items;
    private Expression<?> blocks;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(matchedPattern >= 2);
        this.items = exprs[0];
        this.blocks = exprs[1];
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.blocks.check(event, block -> this.items.check(event, item -> {
            ItemStack stack = item.getRandom();
            if (stack != null) {
                if (block instanceof Block) {
                    return ((Block)block).isPreferredTool(stack);
                }
                if (block instanceof BlockData) {
                    return ((BlockData)block).isPreferredTool(stack);
                }
            }
            return false;
        }), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.items.toString(event, debug) + " is the preferred tool for " + this.blocks.toString(event, debug);
    }

    static {
        Object types = "blocks";
        if (Skript.methodExists(BlockData.class, "isPreferredTool", ItemStack.class)) {
            types = (String)types + "/blockdatas";
        }
        Skript.registerCondition(CondIsPreferredTool.class, "%itemtypes% (is|are) %" + (String)types + "%'s preferred tool[s]", "%itemtypes% (is|are) [the|a] preferred tool[s] (for|of) %" + (String)types + "%", "%itemtypes% (is|are)(n't| not) %" + (String)types + "%'s preferred tool[s]", "%itemtypes% (is|are)(n't| not) [the|a] preferred tool[s] (for|of) %" + (String)types + "%");
    }
}

