/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.SeaPickle
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Sea Pickles")
@Description(value={"An expression to obtain or modify data relating to the pickles of a sea pickle block."})
@Example(value="on block break:\n\ttype of block is sea pickle\n\tsend \"Wow! This stack of sea pickles contained %event-block's sea pickle count% pickles!\"\n\tsend \"It could've contained a maximum of %event-block's maximum sea pickle count% pickles!\"\n\tsend \"It had to have contained at least %event-block's minimum sea pickle count% pickles!\"\n\tcancel event\n\tset event-block's sea pickle count to event-block's maximum sea pickle count\n\tsend \"This bad boy is going to hold so many pickles now!!\"\n")
@Since(value={"2.7"})
public class ExprSeaPickles
extends SimplePropertyExpression<Block, Integer> {
    private boolean minimum;
    private boolean maximum;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.minimum = parseResult.hasTag("min");
        this.maximum = parseResult.hasTag("max");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Integer convert(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof SeaPickle)) {
            return null;
        }
        SeaPickle pickleData = (SeaPickle)blockData;
        if (this.maximum) {
            return pickleData.getMaximumPickles();
        }
        if (this.minimum) {
            return pickleData.getMinimumPickles();
        }
        return pickleData.getPickles();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.minimum || this.maximum) {
            return null;
        }
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: 
            case RESET: 
            case DELETE: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int change;
        if (delta == null && mode != Changer.ChangeMode.RESET && mode != Changer.ChangeMode.DELETE) {
            return;
        }
        int n = change = delta != null ? ((Number)delta[0]).intValue() : 0;
        if (mode == Changer.ChangeMode.REMOVE) {
            change *= -1;
        }
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockData blockData = block.getBlockData();
            if (!(blockData instanceof SeaPickle)) {
                return;
            }
            SeaPickle pickleData = (SeaPickle)blockData;
            int newPickles = change;
            switch (mode) {
                case ADD: 
                case REMOVE: {
                    newPickles += pickleData.getPickles();
                }
                case SET: {
                    if (newPickles == 0) break;
                    newPickles = Math.max(pickleData.getMinimumPickles(), newPickles);
                    newPickles = Math.min(pickleData.getMaximumPickles(), newPickles);
                    break;
                }
                case RESET: {
                    newPickles = pickleData.getMinimumPickles();
                }
            }
            if (newPickles != 0) {
                pickleData.setPickles(newPickles);
                block.setBlockData((BlockData)pickleData);
                continue;
            }
            block.setType(pickleData.isWaterlogged() ? Material.WATER : Material.AIR);
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.maximum ? "maximum " : (this.minimum ? "minimum " : "")) + "sea pickle count";
    }

    static {
        ExprSeaPickles.register(ExprSeaPickles.class, Integer.class, "[:(min|max)[imum]] [sea] pickle(s| (count|amount))", "blocks");
    }
}

