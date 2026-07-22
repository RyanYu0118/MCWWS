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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Date Ago/Later")
@Description(value={"A date the specified timespan before/after another date."})
@Example.Examples(value={@Example(value="set {_yesterday} to 1 day ago"), @Example(value="set {_hourAfter} to 1 hour after {someOtherDate}"), @Example(value="set {_hoursBefore} to 5 hours before {someOtherDate}")})
@Since(value={"2.2-dev33"})
public class ExprDateAgoLater
extends SimpleExpression<Date> {
    private Expression<Timespan> timespan;
    @Nullable
    private Expression<Date> date;
    private boolean ago;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.timespan = exprs[0];
        this.date = exprs[1];
        this.ago = matchedPattern == 0;
        return true;
    }

    @Nullable
    protected Date[] get(Event e) {
        Date date;
        Timespan timespan = this.timespan.getSingle(e);
        Date date2 = date = this.date != null ? this.date.getSingle(e) : new Date();
        if (timespan == null || date == null) {
            return null;
        }
        return new Date[]{this.ago ? date.minus(timespan) : date.plus(timespan)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Date> getReturnType() {
        return Date.class;
    }

    @Override
    public Expression<? extends Date> simplify() {
        if (this.date instanceof Literal && this.timespan instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.timespan.toString(e, debug) + " " + (String)(this.ago ? (this.date != null ? "before " + this.date.toString(e, debug) : "ago") : (this.date != null ? "after " + this.date.toString(e, debug) : "later"));
    }

    static {
        Skript.registerExpression(ExprDateAgoLater.class, Date.class, ExpressionType.COMBINED, "%timespan% (ago|in the past|before [the] [date] %-date%)", "%timespan% (later|(from|after) [the] [date] %-date%)");
    }
}

