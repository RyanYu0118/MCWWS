/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.World;

@Name(value="Sea Level")
@Description(value={"Gets the sea level of a world."})
@Example(value="send \"The sea level in your world is %sea level in player's world%\"")
@Since(value={"2.5.1"})
public class ExprSeaLevel
extends SimplePropertyExpression<World, Long> {
    @Override
    public Long convert(World world) {
        return world.getSeaLevel();
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "sea level";
    }

    static {
        ExprSeaLevel.register(ExprSeaLevel.class, Long.class, "sea level", "worlds");
    }
}

