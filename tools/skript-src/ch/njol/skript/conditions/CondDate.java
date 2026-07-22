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
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Time")
@Description(value={"Tests whether a given <a href='#date'>real time</a> was more or less than some <a href='#timespan'>time span</a> ago."})
@Example(value="command /command-with-cooldown:\n\ttrigger:\n\t\t{command::%player's uuid%::last-usage} was less than a minute ago:\n\t\t\tmessage \"Please wait a minute between uses of this command.\"\n\t\t\tstop\n\t\tset {command::%player's uuid%::last-usage} to now\n\t\t# ... actual command trigger here ...\n")
@Since(value={"2.0"})
public class CondDate
extends Condition {
    private Expression<Date> date;
    private Expression<Timespan> delta;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.date = exprs[0];
        this.delta = exprs[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        long now = System.currentTimeMillis();
        return this.date.check(e, date -> this.delta.check(e, timespan -> now - date.getTime() >= timespan.getAs(Timespan.TimePeriod.MILLISECOND)), this.isNegated());
    }

    @Override
    public Condition simplify() {
        if (this.date instanceof Literal && this.delta instanceof Literal) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.date.toString(e, debug) + " was " + (this.isNegated() ? "less" : "more") + " than " + this.delta.toString(e, debug) + " ago";
    }

    static {
        Skript.registerCondition(CondDate.class, "%date% (was|were)( more|(n't| not) less) than %timespan% [ago]", "%date% (was|were)((n't| not) more| less) than %timespan% [ago]");
    }
}

