/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Location At")
@Description(value={"Allows to create a <a href='#location'>location</a> from three coordinates and a world."})
@Example.Examples(value={@Example(value="set {_loc} to the location at arg-1, arg-2, arg-3 of the world arg-4"), @Example(value="distance between the player and the location (0, 0, 0) is less than 200")})
@Since(value={"2.0"})
public class ExprLocationAt
extends SimpleExpression<Location> {
    private Expression<World> world;
    private Expression<Number> x;
    private Expression<Number> y;
    private Expression<Number> z;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.x = exprs[0];
        this.y = exprs[1];
        this.z = exprs[2];
        this.world = exprs[3];
        return true;
    }

    @Nullable
    protected Location[] get(Event e) {
        World w = this.world.getSingle(e);
        Number x = this.x.getSingle(e);
        Number y = this.y.getSingle(e);
        Number z = this.z.getSingle(e);
        if (w == null || x == null || y == null || z == null) {
            return new Location[0];
        }
        return new Location[]{new Location(w, x.doubleValue(), y.doubleValue(), z.doubleValue())};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the location at (" + this.x.toString(e, debug) + ", " + this.y.toString(e, debug) + ", " + this.z.toString(e, debug) + ") in " + this.world.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprLocationAt.class, Location.class, ExpressionType.COMBINED, "[the] (location|position) [at] [\\(][x[ ][=[ ]]]%number%, [y[ ][=[ ]]]%number%, [and] [z[ ][=[ ]]]%number%[\\)] [[(in|of) [[the] world]] %world%]");
    }
}

