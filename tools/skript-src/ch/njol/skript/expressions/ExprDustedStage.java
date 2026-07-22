/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.Brushable
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Brushable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Brushing Stage")
@Description(value={"Represents how far the block has been uncovered.\nThe only blocks that can currently be \"brushed\" are Suspicious Gravel and Suspicious Sand.\n0 means the block is untouched, the max (usually 3) means nearly fulled brushed.\nResetting this value will set it to 0.\n"})
@Example.Examples(value={@Example(value="# prevent dusting past level 1\non player change block:\n\tif dusting progress of future event-blockdata > 1:\n\t\tcancel event\n"), @Example(value="# draw particles when dusting is complete!\non player change block:\n\tif brushing progress of event-block is max brushing stage of event-block:\n\t\tdraw 20 totem of undying particles at event-block\n")})
@Since(value={"2.12"})
@Keywords(value={"brush", "brushing", "dusting"})
public class ExprDustedStage
extends PropertyExpression<Object, Integer> {
    private boolean isMax;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.isMax = parseResult.hasTag("max");
        return true;
    }

    protected Integer @Nullable [] get(Event event, Object[] source) {
        return this.get(source, obj -> {
            Brushable brushable = this.getBrushable(obj);
            if (brushable != null) {
                return this.isMax ? brushable.getMaximumDusted() : brushable.getDusted();
            }
            return null;
        });
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.isMax) {
            Skript.error("Attempting to modify the max dusted stage is not supported.");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> CollectionUtils.array(Integer.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (this.isMax) {
            return;
        }
        int value = delta == null ? 0 : (Integer)delta[0];
        for (Object obj : this.getExpr().getArray(event)) {
            Brushable brushable = this.getBrushable(obj);
            if (brushable == null) continue;
            int currentValue = brushable.getDusted();
            int maxValue = brushable.getMaximumDusted();
            long newValue = switch (mode) {
                case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> value;
                case Changer.ChangeMode.ADD -> Math2.addClamped(currentValue, value);
                case Changer.ChangeMode.REMOVE -> Math2.addClamped(currentValue, -value);
                default -> throw new IllegalArgumentException("Change mode " + String.valueOf((Object)mode) + " is not valid for ExprDustedStage!");
            };
            brushable.setDusted(Math.clamp(newValue, 0, maxValue));
            if (!(obj instanceof Block)) continue;
            Block block = (Block)obj;
            block.setBlockData((BlockData)brushable);
        }
    }

    @Nullable
    private Brushable getBrushable(Object obj) {
        if (obj instanceof Block) {
            Block block = (Block)obj;
            BlockData blockData = block.getBlockData();
            if (blockData instanceof Brushable) {
                Brushable brushable = (Brushable)blockData;
                return brushable;
            }
        } else if (obj instanceof Brushable) {
            Brushable brushable = (Brushable)obj;
            return brushable;
        }
        return null;
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getExpr().toString(event, debug) + "'s " + (this.isMax ? "maximum " : "") + " dusted stage";
    }

    static {
        ExprDustedStage.register(ExprDustedStage.class, Integer.class, "[:max[imum]] (dust|brush)[ed|ing] (value|stage|progress[ion])", "blocks/blockdatas");
    }
}

