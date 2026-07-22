/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.block.Block;

@Name(value="Redstone Block Power")
@Description(value={"Power of a redstone block"})
@Example(value="if redstone power of targeted block is 15:\n\tsend \"This block is very powerful!\"\n")
@Since(value={"2.5"})
public class ExprRedstoneBlockPower
extends SimplePropertyExpression<Block, Long> {
    @Override
    public Long convert(Block b) {
        return b.getBlockPower();
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "redstone power";
    }

    static {
        ExprRedstoneBlockPower.register(ExprRedstoneBlockPower.class, Long.class, "redstone power", "blocks");
    }
}

