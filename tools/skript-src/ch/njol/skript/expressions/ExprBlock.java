/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.ConverterInfo;

@Name(value="Block")
@Description(value={"The block involved in the event, e.g. the clicked block or the placed block.", "Can optionally include a direction as well, e.g. 'block above' or 'block in front of the player'."})
@Example.Examples(value={@Example(value="block is iron ore"), @Example(value="set block below to air"), @Example(value="spawn a creeper above the block"), @Example(value="loop blocks in radius 4:\n\tloop-block is obsidian\n\tset loop-block to water\n"), @Example(value="block is a chest:\n\tclear the inventory of the block\n")})
@Since(value={"1.0"})
public class ExprBlock
extends WrapperExpression<Block> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        if (exprs.length > 0) {
            this.setExpr(new ConvertedExpression<Location, Block>(Direction.combine(exprs[0], exprs[1]), Block.class, new ConverterInfo<Location, Block>(Location.class, Block.class, Location::getBlock, 0)));
            return true;
        }
        this.setExpr(new EventValueExpression<Block>(Block.class));
        return ((EventValueExpression)this.getExpr()).init();
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.getExpr() instanceof EventValueExpression ? "the block" : "the block " + this.getExpr().toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprBlock.class, Block.class, ExpressionType.SIMPLE, "[the] [event-]block");
        Skript.registerExpression(ExprBlock.class, Block.class, ExpressionType.COMBINED, "[the] block %direction% [%location%]");
    }
}

