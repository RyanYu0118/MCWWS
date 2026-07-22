/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.bukkit.generator.WorldInfo
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.Nullable;

@Name(value="World Seed")
@Description(value={"The seed of given world. Note that it will be returned as Minecraft internally treats seeds, not as you specified it in world configuration."})
@Example(value="broadcast \"Seed: %seed of player's world%\"")
@Since(value={"2.2-dev35"})
public class ExprSeed
extends PropertyExpression<World, Long> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected Long[] get(Event event, World[] source) {
        return this.get(source, WorldInfo::getSeed);
    }

    @Override
    public Class<Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (event == null) {
            return "the seed of " + this.getExpr().toString(event, debug);
        }
        return Classes.getDebugMessage(this.getAll(event));
    }

    static {
        Skript.registerExpression(ExprSeed.class, Long.class, ExpressionType.PROPERTY, "[the] seed[s] (from|of) %worlds%", "%worlds%'[s] seed[s]");
    }
}

