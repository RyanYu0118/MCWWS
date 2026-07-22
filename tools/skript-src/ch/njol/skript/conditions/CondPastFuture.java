/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprNow;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Date;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="In The Past/Future")
@Description(value={"Checks whether a date is in the past or future.", "Note that using the 'now' expression will not be in the past or future when used directly in the condition."})
@Example.Examples(value={@Example(value="set {_date} to now\nwait 5 seconds\nif {_date} is in the past:\n\t# this will be true\n"), @Example(value="if now is in the future:\n\t# this will be false\n"), @Example(value="set {_dates::*} to 1 day from now, 12 days from now, and 1 year from now\nif {_dates::*} are in the future:\n\t# this will be true\nif {_dates::*} have passed:\n\t# this will be false\n")})
@Since(value={"2.10"})
public class CondPastFuture
extends Condition {
    private Expression<Date> dates;
    private boolean isFuture;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(parseResult.hasTag("negated"));
        this.isFuture = parseResult.hasTag("future");
        this.dates = expressions[0];
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.dates instanceof ExprNow) {
            return this.isNegated();
        }
        Expression<T>[] expressionArray = this.dates;
        if (expressionArray instanceof ExpressionList) {
            ExpressionList list = (ExpressionList)expressionArray;
            for (Expression dateExpression : list.getExpressions()) {
                if (!(dateExpression instanceof ExprNow) || !list.getAnd()) continue;
                return this.isNegated();
            }
        }
        if (this.isFuture) {
            return this.dates.check(event, date -> date.compareTo(new Date()) > 0, this.isNegated());
        }
        return this.dates.check(event, date -> date.compareTo(new Date()) < 0, this.isNegated());
    }

    @Override
    public Condition simplify() {
        if (this.dates instanceof Literal) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.dates.toString(event, debug) + (this.dates.isSingle() ? " is" : " are") + " in the" + (this.isFuture ? " future" : " past");
    }

    static {
        Skript.registerCondition(CondPastFuture.class, "%dates% (is|are)[negated:(n't| not)] in the (past|:future)", "%dates% ha(s|ve)[negated:(n't| not)] passed");
    }
}

