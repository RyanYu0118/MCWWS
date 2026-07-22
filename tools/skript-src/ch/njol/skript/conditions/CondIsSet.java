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
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VerboseAssert;
import ch.njol.skript.localization.Language;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Exists/Is Set")
@Description(value={"Checks whether a given expression or variable is set."})
@Example.Examples(value={@Example(value="{teams::%player's uuid%::preferred-team} is not set"), @Example(value="on damage:\n\tprojectile exists\n\tbroadcast \"%attacker% used a %projectile% to attack %victim%!\"\n")})
@Since(value={"1.2"})
public class CondIsSet
extends Condition
implements VerboseAssert {
    private Expression<?> expr;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.expr = exprs[0];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    private boolean check(Expression<?> expr, Event e) {
        if (expr instanceof ExpressionList) {
            for (Expression ex : ((ExpressionList)expr).getExpressions()) {
                assert (ex != null);
                boolean b = this.check(ex, e);
                if (!(expr.getAnd() ^ b)) continue;
                return !expr.getAnd();
            }
            return expr.getAnd();
        }
        assert (expr.getAnd());
        ?[] all = expr.getAll(e);
        return this.isNegated() ^ all.length != 0;
    }

    @Override
    public boolean check(Event e) {
        return this.check(this.expr, e);
    }

    @Override
    public String getExpectedMessage(Event event) {
        return this.isNegated() ? Language.get("none") : "a value";
    }

    @Override
    public String getReceivedMessage(Event event) {
        return VerboseAssert.getExpressionValue(this.expr, event);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.expr.toString(e, debug) + (this.isNegated() ? " isn't" : " is") + " set";
    }

    static {
        Skript.registerCondition(CondIsSet.class, "%~objects% (exist[s]|(is|are) set)", "%~objects% (do[es](n't| not) exist|(is|are)(n't| not) set)");
    }
}

