/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockSpreadEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
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
import org.bukkit.event.block.BlockSpreadEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Source Block")
@Description(value={"The source block in a spread event."})
@Events(value={"Spread"})
@Example(value="on spread:\n\tif the source block is a grass block:\n\t\tset the source block to dirt\n")
@Since(value={"2.7"})
public class ExprSourceBlock
extends SimpleExpression<Block>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(BlockSpreadEvent.class);
    }

    protected Block[] get(Event event) {
        if (!(event instanceof BlockSpreadEvent)) {
            return new Block[0];
        }
        return new Block[]{((BlockSpreadEvent)event).getSource()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the source block";
    }

    static {
        Skript.registerExpression(ExprSourceBlock.class, Block.class, ExpressionType.SIMPLE, "[the] source block");
    }
}

