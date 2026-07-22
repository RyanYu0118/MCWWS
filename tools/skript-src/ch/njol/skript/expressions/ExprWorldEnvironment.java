/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

@Name(value="World Environment")
@Description(value={"The environment of a world"})
@Example(value="if environment of player's world is nether:\n\tapply fire resistance to player for 10 minutes\n")
@Since(value={"2.7"})
public class ExprWorldEnvironment
extends SimplePropertyExpression<World, World.Environment> {
    @Override
    @Nullable
    public World.Environment convert(World world) {
        return world.getEnvironment();
    }

    @Override
    public Class<? extends World.Environment> getReturnType() {
        return World.Environment.class;
    }

    @Override
    protected String getPropertyName() {
        return "environment";
    }

    static {
        ExprWorldEnvironment.register(ExprWorldEnvironment.class, World.Environment.class, "[world] environment", "worlds");
    }
}

