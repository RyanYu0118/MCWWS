/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Targeted Block")
@Description(value={"The block at the crosshair. This regards all blocks that are not air as fully solid, e.g. torches will be like a solid stone block for this expression.", "The actual target block will regard the actual hit box of the block."})
@Example.Examples(value={@Example(value="set target block of player to stone"), @Example(value="set target block of player to oak_stairs[waterlogged=true]"), @Example(value="break target block of player using player's tool"), @Example(value="give player 1 of type of target block"), @Example(value="teleport player to location above target block"), @Example(value="kill all entities in radius 3 around target block of player"), @Example(value="set {_block} to actual target block of player"), @Example(value="break actual target block of player")})
@Since(value={"1.0, 2.9.0 (actual/exact)"})
public class ExprTargetedBlock
extends PropertyExpression<LivingEntity, Block> {
    private boolean actual;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(exprs[0]);
        this.actual = parser.hasTag("actual");
        return true;
    }

    protected Block[] get(Event event, LivingEntity[] source) {
        Integer distance = SkriptConfig.maxTargetBlockDistance.value();
        return this.get(source, livingEntity -> {
            Block block = this.actual ? livingEntity.getTargetBlockExact(distance.intValue()) : livingEntity.getTargetBlock(null, distance.intValue());
            if (block != null && ItemUtils.isAir(block.getType())) {
                return null;
            }
            return block;
        });
    }

    @Override
    public boolean setTime(int time) {
        super.setTime(time);
        return true;
    }

    @Override
    public Class<Block> getReturnType() {
        return Block.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String block = this.getExpr().isSingle() ? "block" : "blocks";
        return "the " + (this.actual ? "actual " : "") + "target " + block + " of " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprTargetedBlock.class, Block.class, ExpressionType.COMBINED, "[the] [actual:(actual[ly]|exact)] target[ed] block[s] [of %livingentities%]", "%livingentities%'[s] [actual:(actual[ly]|exact)] target[ed] block[s]");
    }
}

