/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.world.MoonPhase
 *  org.bukkit.World
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import io.papermc.paper.world.MoonPhase;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

@Name(value="Moon Phase")
@Description(value={"The current moon phase of a world."})
@Example(value="if moon phase of player's world is full moon:\n\tsend \"Watch for the wolves!\"\n")
@Since(value={"2.7"})
public class ExprMoonPhase
extends SimplePropertyExpression<World, MoonPhase> {
    @Override
    @Nullable
    public MoonPhase convert(World world) {
        return world.getMoonPhase();
    }

    @Override
    public Class<? extends MoonPhase> getReturnType() {
        return MoonPhase.class;
    }

    @Override
    protected String getPropertyName() {
        return "moon phase";
    }

    static {
        if (Skript.classExists("io.papermc.paper.world.MoonPhase")) {
            ExprMoonPhase.register(ExprMoonPhase.class, MoonPhase.class, "(lunar|moon) phase[s]", "worlds");
        }
    }
}

