/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Beacon
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.Nullable;

@Name(value="Beacon Tier")
@Description(value={"The tier of a beacon. Ranges from 0 to 4."})
@Example(value="if the beacon tier of the clicked block is 4:\n\tsend \"This is a max tier beacon!\"\n")
@Since(value={"2.10"})
public class ExprBeaconTier
extends SimplePropertyExpression<Block, Integer> {
    @Override
    @Nullable
    public Integer convert(Block block) {
        BlockState blockState = block.getState();
        if (blockState instanceof Beacon) {
            Beacon beacon = (Beacon)blockState;
            return beacon.getTier();
        }
        return null;
    }

    @Override
    public Class<Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "beacon tier";
    }

    static {
        ExprBeaconTier.register(ExprBeaconTier.class, Integer.class, "beacon tier", "blocks");
    }
}

