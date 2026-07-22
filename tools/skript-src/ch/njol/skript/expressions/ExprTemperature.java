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

@Name(value="Temperature")
@Description(value={"Temperature at given block."})
@Example(value="message \"%temperature of the targeted block%\"")
@Since(value={"2.2-dev35"})
public class ExprTemperature
extends SimplePropertyExpression<Block, Number> {
    @Override
    public Number convert(Block block) {
        return block.getTemperature();
    }

    @Override
    protected String getPropertyName() {
        return "temperature";
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    static {
        ExprTemperature.register(ExprTemperature.class, Number.class, "temperature[s]", "blocks");
    }
}

