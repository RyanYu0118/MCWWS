/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Time Since/Until")
@Description(value={"The time since a date has passed or the time until a date will pass.", "This expression will return 0 seconds if the time since or time until would be negative, e.g. if one tries to get the time since a future date."})
@Example.Examples(value={@Example(value="send \"%time since 5 minecraft days ago% has passed since 5 minecraft days ago!\" to player"), @Example(value="send \"%time until {countdown::end}% until the game begins!\" to player")})
@Since(value={"2.5, 2.10 (time until)"})
public class ExprTimeSince
extends SimplePropertyExpression<Date, Timespan> {
    private boolean isSince;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isSince = matchedPattern == 0;
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Timespan convert(Date date) {
        Date now = Date.now();
        if (this.isSince ? date.compareTo(now) < 1 : date.compareTo(now) > -1) {
            return date.difference(now);
        }
        return new Timespan();
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "time " + (this.isSince ? "since" : "until");
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the time " + (this.isSince ? "since " : "until ") + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprTimeSince.class, Timespan.class, ExpressionType.PROPERTY, "[the] time since %dates%", "[the] (time [remaining]|remaining time) until %dates%");
    }
}

