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
import ch.njol.skript.lang.LoopSection;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Loop Iteration")
@Description(value={"Returns the loop's current iteration count (for both normal and while loops)."})
@Example.Examples(value={@Example(value="while player is online:\n\tgive player 1 stone\n\twait 5 ticks\n\tif loop-counter > 30:\n\t\tstop loop\n"), @Example(value="loop {top-balances::*}:\n\tif loop-iteration <= 10:\n\t\tbroadcast \"#%loop-iteration% %loop-index% has $%loop-value%\"\n")})
@Since(value={"2.8.0"})
public class ExprLoopIteration
extends SimpleExpression<Long> {
    private LoopSection loop;
    private int loopNumber;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.loopNumber = -1;
        if (exprs[0] != null) {
            this.loopNumber = ((Number)((Literal)exprs[0]).getSingle()).intValue();
        }
        int i = 1;
        LoopSection loop = null;
        for (LoopSection l : this.getParser().getCurrentSections(LoopSection.class)) {
            if (i < this.loopNumber) {
                ++i;
                continue;
            }
            if (loop != null) {
                Skript.error("There are multiple loops. Use loop-iteration-1/2/3/etc. to specify which loop-iteration you want.");
                return false;
            }
            loop = l;
            if (i != this.loopNumber) continue;
            break;
        }
        if (loop == null) {
            Skript.error("The loop iteration expression must be used in a loop");
            return false;
        }
        this.loop = loop;
        return true;
    }

    protected Long[] get(Event event) {
        return new Long[]{this.loop.getLoopCounter(event)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "loop-iteration" + (String)(this.loopNumber != -1 ? "-" + this.loopNumber : "");
    }

    static {
        Skript.registerExpression(ExprLoopIteration.class, Long.class, ExpressionType.SIMPLE, "[the] loop(-| )(counter|iteration)[-%-*number%]");
    }
}

