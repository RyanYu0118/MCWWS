/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.Beehive
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
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Consumer;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Beehive Honey Level")
@Description(value={"The current or max honey level of a beehive.", "The max level is 5, which cannot be changed."})
@Example(value="set the honey level of {_beehive} to the max honey level of {_beehive}")
@Since(value={"2.11"})
public class ExprBeehiveHoneyLevel
extends SimplePropertyExpression<Block, Integer> {
    private boolean isMax;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isMax = parseResult.hasTag("max");
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Integer convert(Block block) {
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Beehive)) {
            return null;
        }
        Beehive beehive = (Beehive)blockData;
        if (this.isMax) {
            return beehive.getMaximumHoneyLevel();
        }
        return beehive.getHoneyLevel();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.isMax) {
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Integer.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int value = delta != null ? (Integer)delta[0] : 0;
        Consumer<Beehive> consumer = switch (mode) {
            case Changer.ChangeMode.SET -> beehive -> beehive.setHoneyLevel(Math2.fit(0, value, 5));
            case Changer.ChangeMode.ADD -> beehive -> {
                int current = beehive.getHoneyLevel();
                beehive.setHoneyLevel(Math2.fit(0, current + value, 5));
            };
            case Changer.ChangeMode.REMOVE -> beehive -> {
                int current = beehive.getHoneyLevel();
                beehive.setHoneyLevel(Math2.fit(0, current - value, 5));
            };
            default -> throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)mode));
        };
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockData blockData = block.getBlockData();
            if (!(blockData instanceof Beehive)) continue;
            Beehive beehive2 = (Beehive)blockData;
            consumer.accept(beehive2);
            block.setBlockData((BlockData)beehive2);
        }
    }

    @Override
    public Class<Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.isMax ? "maximum " : "") + "honey level";
    }

    static {
        ExprBeehiveHoneyLevel.registerDefault(ExprBeehiveHoneyLevel.class, Integer.class, "[max:max[imum]] honey level", "blocks");
    }
}

