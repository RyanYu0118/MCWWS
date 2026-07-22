/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Beacon
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Beacon Range")
@Description(value={"The range of a beacon's effects, in blocks."})
@Example(value="if the beacon tier of the clicked block is 4:\n\tset the beacon effect range of the clicked block to 100\n")
@Since(value={"2.10"})
public class ExprBeaconRange
extends SimplePropertyExpression<Block, Double> {
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    @Nullable
    public Double convert(Block block) {
        BlockState blockState = block.getState();
        if (blockState instanceof Beacon) {
            Beacon beacon = (Beacon)blockState;
            return beacon.getEffectRange();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> CollectionUtils.array(Double.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.RESET) {
            for (Block block : (Block[])this.getExpr().getArray(event)) {
                BlockState blockState = block.getState();
                if (!(blockState instanceof Beacon)) continue;
                Beacon beacon = (Beacon)blockState;
                beacon.resetEffectRange();
                beacon.update(true);
            }
            return;
        }
        if (!$assertionsDisabled && delta == null) {
            throw new AssertionError();
        }
        double range = ((Number)delta[0]).doubleValue();
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof Beacon)) continue;
            Beacon beacon = (Beacon)blockState;
            switch (mode) {
                case SET: {
                    beacon.setEffectRange(Math2.fit(0.0, range, Double.MAX_VALUE));
                    break;
                }
                case ADD: {
                    beacon.setEffectRange(Math2.fit(0.0, beacon.getEffectRange() + range, Double.MAX_VALUE));
                    break;
                }
                case REMOVE: {
                    beacon.setEffectRange(Math2.fit(0.0, beacon.getEffectRange() - range, Double.MAX_VALUE));
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            beacon.update(true);
        }
    }

    @Override
    public Class<? extends Double> getReturnType() {
        return Double.class;
    }

    @Override
    protected String getPropertyName() {
        return "beacon range";
    }

    static {
        boolean bl = $assertionsDisabled = !ExprBeaconRange.class.desiredAssertionStatus();
        if (Skript.methodExists(Beacon.class, "getEffectRange", new Class[0])) {
            ExprBeaconRange.register(ExprBeaconRange.class, Double.class, "beacon [effect] range", "blocks");
        }
    }
}

