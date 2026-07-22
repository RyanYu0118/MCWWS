/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.RespawnAnchor
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Respawn Anchor Charges")
@Description(value={"The charges of a respawn anchor."})
@Example(value="set the charges of event-block to 3")
@RequiredPlugins(value={"Minecraft 1.16+"})
@Since(value={"2.7"})
public class ExprCharges
extends SimplePropertyExpression<Block, Integer> {
    private boolean maxCharges;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.maxCharges = parseResult.hasTag("max");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Integer convert(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof RespawnAnchor) {
            RespawnAnchor respawnAnchor = (RespawnAnchor)blockData;
            if (this.maxCharges) {
                return respawnAnchor.getMaximumCharges();
            }
            return respawnAnchor.getCharges();
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.REMOVE, Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int charge = 0;
        int charges = delta != null ? ((Number)delta[0]).intValue() : 0;
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockData blockData = block.getBlockData();
            if (!(blockData instanceof RespawnAnchor)) continue;
            RespawnAnchor respawnAnchor = (RespawnAnchor)blockData;
            switch (mode) {
                case REMOVE: {
                    charge = respawnAnchor.getCharges() - charges;
                    break;
                }
                case ADD: {
                    charge = respawnAnchor.getCharges() + charges;
                    break;
                }
                case SET: {
                    charge = charges;
                }
            }
            respawnAnchor.setCharges(Math.min(Math.max(charge, 0), 4));
            block.setBlockData((BlockData)respawnAnchor);
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "charges";
    }

    static {
        ExprCharges.register(ExprCharges.class, Integer.class, "[:max[imum]] charge[s]", "blocks");
    }
}

