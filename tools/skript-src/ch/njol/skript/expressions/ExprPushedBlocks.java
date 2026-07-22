/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockPistonExtendEvent
 *  org.bukkit.event.block.BlockPistonRetractEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Moved blocks")
@Description(value={"Blocks which are moved in a piston event. Cannot be used outside of piston events."})
@Example(value="the moved blocks")
@Since(value={"2.2-dev27"})
public class ExprPushedBlocks
extends SimpleExpression<Block>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(BlockPistonExtendEvent.class, BlockPistonRetractEvent.class);
    }

    @Nullable
    protected Block[] get(Event e) {
        if (!CollectionUtils.isAnyInstanceOf(e, BlockPistonExtendEvent.class, BlockPistonRetractEvent.class)) {
            return null;
        }
        return e instanceof BlockPistonExtendEvent ? ((BlockPistonExtendEvent)e).getBlocks().toArray(new Block[0]) : ((BlockPistonRetractEvent)e).getBlocks().toArray(new Block[0]);
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "moved blocks";
    }

    static {
        Skript.registerExpression(ExprPushedBlocks.class, Block.class, ExpressionType.SIMPLE, "[the] moved blocks");
    }
}

