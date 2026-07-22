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

@Name(value="Humidity")
@Description(value={"Humidity of given blocks."})
@Example(value="set {_humidity} to event-block's humidity")
@Since(value={"2.2-dev35"})
public class ExprHumidity
extends SimplePropertyExpression<Block, Number> {
    @Override
    public Number convert(Block block) {
        return block.getHumidity();
    }

    @Override
    protected String getPropertyName() {
        return "humidity";
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    static {
        ExprHumidity.register(ExprHumidity.class, Number.class, "humidit(y|ies)", "blocks");
    }
}

