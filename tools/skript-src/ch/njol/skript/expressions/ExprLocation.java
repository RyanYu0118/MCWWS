/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Location")
@Description(value={"The location where an event happened (e.g. at an entity or block), or a location <a href='#ExprDirection'>relative</a> to another (e.g. 1 meter above another location)."})
@Example.Examples(value={@Example(value="drop 5 apples at the event-location # exactly the same as writing 'drop 5 apples'"), @Example(value="set {_loc} to the location 1 meter above the player")})
@Since(value={"2.0"})
public class ExprLocation
extends WrapperExpression<Location> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (exprs.length > 0) {
            super.setExpr(Direction.combine(exprs[0], exprs[1]));
            return true;
        }
        this.setExpr(new EventValueExpression<Location>(Location.class));
        return ((EventValueExpression)this.getExpr()).init();
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.getExpr() instanceof EventValueExpression ? "the location" : "the location " + this.getExpr().toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprLocation.class, Location.class, ExpressionType.SIMPLE, "[the] [event-](location|position)");
        Skript.registerExpression(ExprLocation.class, Location.class, ExpressionType.COMBINED, "[the] (location|position) %directions% [%location%]");
    }
}

