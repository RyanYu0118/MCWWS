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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.experiments.QueueExperimentSyntax;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import java.util.Iterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.util.SkriptQueue;

@Name(value="Queue (Experimental)")
@Description(value={"Requires the <code>using queues</code> experimental feature flag to be enabled.\n\nCreates a new queue.\nA queue is a set of elements that can have things removed from the start and added to the end.\n\nAny value can be added to a queue. Adding a non-existent value (e.g. `{variable that isn't set}`) will have no effect.\nThis means that removing an element from the queue will always return a value <i>unless the queue is empty</i>.\n\nRequesting an element from a queue (e.g. `the 1st element of {queue}`) also removes it from the queue."})
@Example.Examples(value={@Example(value="set {queue} to a new queue\nadd \"hello\" and \"there\" to {queue}\nbroadcast the first element of {queue} # hello\nbroadcast the first element of {queue} # there\n# queue is now empty\n"), @Example(value="set {queue} to a new queue of \"hello\" and \"there\"\nbroadcast the last element of {queue} # removes 'there'\nadd \"world\" to {queue}\nbroadcast the first 2 elements of {queue} # removes 'hello', 'world'\n")})
@Since(value={"2.10 (experimental)"})
public class ExprQueue
extends SimpleExpression<SkriptQueue>
implements QueueExperimentSyntax {
    @Nullable
    private Expression<?> contents;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        if (expressions[0] != null) {
            this.contents = LiteralUtils.defendExpression(expressions[0]);
        }
        return this.contents == null || LiteralUtils.canInitSafely(this.contents);
    }

    protected SkriptQueue @Nullable [] get(Event event) {
        SkriptQueue queue = new SkriptQueue();
        SkriptQueue[] result = new SkriptQueue[]{queue};
        if (this.contents == null) {
            return result;
        }
        Iterator iterator = this.contents.iterator(event);
        if (iterator == null) {
            return result;
        }
        while (iterator.hasNext()) {
            queue.add(iterator.next());
        }
        return result;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends SkriptQueue> getReturnType() {
        return SkriptQueue.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.contents == null) {
            return "a queue";
        }
        return "a queue of " + this.contents.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprQueue.class, SkriptQueue.class, ExpressionType.COMBINED, "[a] [new] queue [(of|with) %-objects%]");
    }
}

