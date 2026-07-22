/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.PeekingIterator
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.KeyedIterableExpression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LoopSection;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.ContainerExpression;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.util.Container;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import com.google.common.collect.PeekingIterator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Loop")
@Description(value={"Loop sections repeat their code with multiple values.", "", "A loop will loop through all elements of the given expression, e.g. all players, worlds, items, etc. The conditions & effects inside the loop will be executed for every of those elements, which can be accessed with \u2018loop-<what>\u2019, e.g. <code>send \"hello\" to loop-player</code>. When a condition inside a loop is not fulfilled the loop will start over with the next element of the loop. You can however use <code>stop loop</code> to exit the loop completely and resume code execution after the end of the loop.", "", "<b>Loopable Values</b>", "All expressions that represent more than one value, e.g. \u2018all players\u2019, \u2018worlds\u2019, etc., as well as list variables, can be looped. You can also use a list of expressions, e.g. <code>loop the victim and the attacker</code>, to execute the same code for only a few values.", "", "<b>List Variables</b>", "When looping list variables, you can also use <code>loop-index</code> in addition to <code>loop-value</code> inside the loop. <code>loop-value</code> is the value of the currently looped variable, and <code>loop-index</code> is the last part of the variable's name (the part where the list variable has its asterisk *)."})
@Example.Examples(value={@Example(value="loop all players:\n\tsend \"Hello %loop-player%!\" to loop-player\n"), @Example(value="loop items in player's inventory:\n\tif loop-item is dirt:\n\t\tset loop-item to air\n"), @Example(value="loop 10 times:\n\tsend title \"%11 - loop-value%\" and subtitle \"seconds left until the game begins\" to player for 1 second # 10, 9, 8 etc.\n\twait 1 second\n"), @Example(value="loop {Coins::*}:\n\tset {Coins::%loop-index%} to loop-value + 5 # Same as \"add 5 to {Coins::%loop-index%}\" where loop-index is the uuid of \" +\n\t\"the player and loop-value is the number of coins for the player\n"), @Example(value="loop shuffled (integers between 0 and 8):\n\tif all:\n\t\tprevious loop-value = 1\n\t\tloop-value = 4\n\t\tnext loop-value = 8\n\tthen:\n\t\tkill all players\n")})
@Since(value={"1.0"})
public class SecLoop
extends LoopSection {
    protected @UnknownNullability Expression<?> expression;
    private final transient Map<Event, Object> current = new WeakHashMap<Event, Object>();
    private final transient Map<Event, Iterator<?>> iteratorMap = new WeakHashMap();
    private final transient Map<Event, Object> previous = new WeakHashMap<Event, Object>();
    @Nullable
    protected TriggerItem actualNext;
    private boolean guaranteedToLoop;
    private Object nextValue = null;
    private boolean loopPeeking;
    protected boolean iterableSingle;
    protected boolean keyed;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        this.expression = LiteralUtils.defendExpression(exprs[0]);
        if (!LiteralUtils.canInitSafely(this.expression)) {
            Skript.error("Can't understand this loop: '" + parseResult.expr.substring(5) + "'");
            return false;
        }
        if (!(this.expression instanceof Variable) && Container.class.isAssignableFrom(this.expression.getReturnType())) {
            Container.ContainerType type = this.expression.getReturnType().getAnnotation(Container.ContainerType.class);
            if (type == null) {
                throw new SkriptAPIException(this.expression.getReturnType().getName() + " implements Container but is missing the required @ContainerType annotation");
            }
            this.expression = new ContainerExpression(this.expression, type.value());
        }
        if (this.getParser().hasExperiment(Feature.QUEUES) && this.expression.isSingle() && (this.expression instanceof Variable || this.expression.canReturn(Iterable.class))) {
            this.iterableSingle = true;
        } else if (this.expression.isSingle()) {
            Skript.error("Can't loop '" + String.valueOf(this.expression) + "' because it's only a single value");
            return false;
        }
        this.loopPeeking = exprs[0].supportsLoopPeeking();
        this.guaranteedToLoop = SecLoop.guaranteedToLoop(this.expression);
        this.keyed = KeyedIterableExpression.canIterateWithKeys(this.expression);
        this.loadOptionalCode(sectionNode);
        this.setInternalNext(this);
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        Iterator<Object> iter = this.iteratorMap.get(event);
        if (iter == null) {
            if (this.iterableSingle) {
                Object value = this.expression.getSingle(event);
                if (value instanceof Container) {
                    Container container = (Container)value;
                    iter = container.containerIterator();
                } else if (value instanceof Iterable) {
                    Iterable iterable = (Iterable)value;
                    iter = iterable.iterator();
                } else {
                    iter = Collections.singleton(value).iterator();
                }
            } else {
                Iterator<Object> iterator = iter = this.keyed ? ((KeyedIterableExpression)this.expression).keyedIterator(event) : this.expression.iterator(event);
                if (iter != null && iter.hasNext()) {
                    this.iteratorMap.put(event, iter);
                } else {
                    iter = null;
                }
            }
        }
        if (iter == null || !iter.hasNext() && this.nextValue == null) {
            this.exit(event);
            this.debug(event, false);
            return this.actualNext;
        }
        this.previous.put(event, this.current.get(event));
        if (this.nextValue != null) {
            this.store(event, this.nextValue);
            this.nextValue = null;
        } else if (iter.hasNext()) {
            this.store(event, iter.next());
        }
        return this.walk(event, true);
    }

    protected void store(Event event, Object next) {
        this.current.put(event, next);
        this.currentLoopCounter.put(event, this.currentLoopCounter.getOrDefault(event, 0L) + 1L);
    }

    @Override
    @Nullable
    public ExecutionIntent executionIntent() {
        return this.guaranteedToLoop ? this.triggerExecutionIntent() : null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "loop " + this.expression.toString(event, debug);
    }

    @Nullable
    public Object getCurrent(Event event) {
        return this.current.get(event);
    }

    @Nullable
    public Object getNext(Event event) {
        if (!this.loopPeeking) {
            return null;
        }
        Iterator<?> iter = this.iteratorMap.get(event);
        if (iter == null || !iter.hasNext()) {
            return null;
        }
        if (iter instanceof PeekingIterator) {
            PeekingIterator peekingIterator = (PeekingIterator)iter;
            return peekingIterator.peek();
        }
        this.nextValue = iter.next();
        return this.nextValue;
    }

    @Nullable
    public Object getPrevious(Event event) {
        return this.previous.get(event);
    }

    public Expression<?> getLoopedExpression() {
        return this.expression;
    }

    public boolean isKeyedLoop() {
        return this.keyed;
    }

    @Override
    public SecLoop setNext(@Nullable TriggerItem next) {
        this.actualNext = next;
        return this;
    }

    protected void setInternalNext(TriggerItem item) {
        super.setNext(item);
    }

    @Override
    @Nullable
    public TriggerItem getActualNext() {
        return this.actualNext;
    }

    @Override
    public void exit(Event event) {
        this.current.remove(event);
        this.iteratorMap.remove(event);
        this.previous.remove(event);
        this.nextValue = null;
        super.exit(event);
    }

    private static boolean guaranteedToLoop(Expression<?> expression) {
        if (expression instanceof Literal) {
            Literal literal = (Literal)expression;
            return literal.getAll().length > 0;
        }
        if (!(expression instanceof ExpressionList)) {
            return false;
        }
        ExpressionList list = (ExpressionList)expression;
        if (!list.getAnd()) {
            for (Expression expr : list.getExpressions()) {
                if (SecLoop.guaranteedToLoop(expr)) continue;
                return false;
            }
            return true;
        }
        for (Expression expr : list.getExpressions()) {
            if (!SecLoop.guaranteedToLoop(expr)) continue;
            return true;
        }
        return false;
    }

    public boolean supportsPeeking() {
        return this.loopPeeking;
    }

    public Expression<?> getExpression() {
        return this.expression;
    }

    static {
        Skript.registerSection(SecLoop.class, "loop %objects%");
    }
}

