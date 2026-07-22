/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
 *  org.apache.commons.lang.ArrayUtils
 *  org.apache.commons.lang3.function.TriFunction
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.EmptyIterator;
import com.google.common.collect.Iterators;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.util.SkriptQueue;

@Name(value="Elements")
@Description(value={"The first, last, range or a random element of a set, e.g. a list variable, or a queue.", "Asking for elements from a queue will also remove them from the queue, see the new queue expression for more information.", "See also: <a href='#ExprRandom'>random expression</a>"})
@Example.Examples(value={@Example(value="broadcast the first 3 elements of {top players::*}"), @Example(value="set {_last} to last element of {top players::*}"), @Example(value="set {_random player} to random element out of all players"), @Example(value="send 2nd last element of {top players::*} to player"), @Example(value="set {page2::*} to elements from 11 to 20 of {top players::*}"), @Example(value="broadcast the 1st element in {queue}"), @Example(value="broadcast the first 3 elements in {queue}")})
@Since(value={"2.0, 2.7 (relative to last element), 2.8.0 (range of elements)"})
public class ExprElement<T>
extends SimpleExpression<T>
implements KeyProviderExpression<T> {
    private static final Patterns<ElementType[]> PATTERNS = new Patterns(new Object[][]{{"[the] (first|1:last) element [out] of %objects%", new ElementType[]{ElementType.FIRST_ELEMENT, ElementType.LAST_ELEMENT}}, {"[the] (first|1:last) %integer% elements [out] of %objects%", new ElementType[]{ElementType.FIRST_X_ELEMENTS, ElementType.LAST_X_ELEMENTS}}, {"[a] random element [out] of %objects%", new ElementType[]{ElementType.RANDOM}}, {"[the] %integer%(st|nd|rd|th) [1:[to] last] element [out] of %objects%", new ElementType[]{ElementType.ORDINAL, ElementType.TAIL_END_ORDINAL}}, {"[the] elements (from|between) %integer% (to|and) %integer% [out] of %objects%", new ElementType[]{ElementType.RANGE}}, {"[the] (first|next|1:last) element (of|in) %queue%", new ElementType[]{ElementType.FIRST_ELEMENT, ElementType.LAST_ELEMENT}}, {"[the] (first|1:last) %integer% elements (of|in) %queue%", new ElementType[]{ElementType.FIRST_X_ELEMENTS, ElementType.LAST_X_ELEMENTS}}, {"[a] random element (of|in) %queue%", new ElementType[]{ElementType.RANDOM}}, {"[the] %integer%(st|nd|rd|th) [1:[to] last] element (of|in) %queue%", new ElementType[]{ElementType.ORDINAL, ElementType.TAIL_END_ORDINAL}}, {"[the] elements (from|between) %integer% (to|and) %integer% (of|in) %queue%", new ElementType[]{ElementType.RANGE}}});
    private final Map<Event, List<String>> cache = new WeakHashMap<Event, List<String>>();
    private Expression<? extends T> expr;
    @Nullable
    private Expression<Integer> startIndex;
    @Nullable
    private Expression<Integer> endIndex;
    private ElementType type;
    private boolean queue;
    private boolean keyed;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ElementType[] types = PATTERNS.getInfo(matchedPattern);
        boolean bl = this.queue = matchedPattern > 4;
        if (this.queue && !this.getParser().hasExperiment(Feature.QUEUES)) {
            return false;
        }
        this.expr = this.queue ? expressions[expressions.length - 1] : LiteralUtils.defendExpression(expressions[expressions.length - 1]);
        this.type = types[parseResult.mark];
        switch (this.type.ordinal()) {
            case 7: {
                this.endIndex = expressions[1];
            }
            case 2: 
            case 3: 
            case 5: 
            case 6: {
                this.startIndex = expressions[0];
                break;
            }
            default: {
                this.startIndex = null;
            }
        }
        this.keyed = KeyProviderExpression.canReturnKeys(this.expr);
        return this.queue || LiteralUtils.canInitSafely(this.expr);
    }

    @Override
    protected T @Nullable [] get(Event event) {
        if (this.queue) {
            return this.getFromQueue(event);
        }
        if (this.keyed) {
            KeyedValue.UnzippedKeyValues<T> unzipped = KeyedValue.unzip(this.keyedIterator(event));
            this.cache.put(event, unzipped.keys());
            Object[] empty = (Object[])Array.newInstance(this.getReturnType(), 0);
            return unzipped.values().toArray(empty);
        }
        Iterator<T> iterator = this.iterator(event);
        assert (iterator != null);
        return Iterators.toArray(iterator, this.getReturnType());
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
        if (!this.keyed) {
            throw new UnsupportedOperationException();
        }
        if (!this.cache.containsKey(event)) {
            throw new SkriptAPIException("Cannot call getArrayKeys() before calling getArray() or getAll()");
        }
        return this.cache.remove(event).toArray(new String[0]);
    }

    @Override
    @Nullable
    public Iterator<? extends T> iterator(Event event) {
        if (this.queue) {
            return Optional.ofNullable(this.getFromQueue(event)).map(Iterators::forArray).orElse(null);
        }
        Iterator iterator = this.expr.iterator(event);
        return this.transformIterator(event, iterator);
    }

    @Override
    public Iterator<KeyedValue<T>> keyedIterator(Event event) {
        if (!this.keyed) {
            throw new UnsupportedOperationException();
        }
        Iterator iterator = ((KeyProviderExpression)this.expr).keyedIterator(event);
        return this.transformIterator(event, iterator);
    }

    private <A> Iterator<A> transformIterator(Event event, @Nullable Iterator<A> iterator) {
        if (iterator == null || !iterator.hasNext()) {
            return EmptyIterator.get();
        }
        Integer startIndex = 0;
        Integer endIndex = 0;
        if (this.startIndex != null && ((startIndex = this.startIndex.getSingle(event)) == null || startIndex <= 0 && this.type != ElementType.RANGE)) {
            return EmptyIterator.get();
        }
        if (this.endIndex != null && (endIndex = this.endIndex.getSingle(event)) == null) {
            return EmptyIterator.get();
        }
        return this.type.apply(iterator, startIndex, endIndex);
    }

    private T @Nullable [] getFromQueue(Event event) {
        SkriptQueue queue = (SkriptQueue)this.expr.getSingle(event);
        if (queue == null) {
            return null;
        }
        Integer startIndex = 0;
        Integer endIndex = 0;
        if (this.startIndex != null && ((startIndex = this.startIndex.getSingle(event)) == null || startIndex <= 0 && this.type != ElementType.RANGE)) {
            return null;
        }
        if (this.endIndex != null && (endIndex = this.endIndex.getSingle(event)) == null) {
            return null;
        }
        return switch (this.type.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> CollectionUtils.array(queue.pollFirst());
            case 1 -> CollectionUtils.array(queue.pollLast());
            case 4 -> CollectionUtils.array(queue.removeSafely(ThreadLocalRandom.current().nextInt(0, queue.size())));
            case 5 -> CollectionUtils.array(queue.removeSafely(startIndex - 1));
            case 6 -> CollectionUtils.array(queue.removeSafely(queue.size() - startIndex));
            case 2 -> CollectionUtils.array(queue.removeRangeSafely(0, startIndex));
            case 3 -> CollectionUtils.array(queue.removeRangeSafely(queue.size() - startIndex, queue.size()));
            case 7 -> {
                boolean reverse = startIndex > endIndex;
                Object[] elements = CollectionUtils.array(queue.removeRangeSafely(Math.min(startIndex, endIndex) - 1, Math.max(startIndex, endIndex)));
                if (reverse) {
                    ArrayUtils.reverse((Object[])elements);
                }
                yield elements;
            }
        };
    }

    @Override
    public boolean canReturnKeys() {
        if (!this.keyed) {
            return false;
        }
        return ((KeyProviderExpression)this.expr).canReturnKeys();
    }

    @Override
    public boolean areKeysRecommended() {
        if (!this.keyed) {
            return false;
        }
        return ((KeyProviderExpression)this.expr).areKeysRecommended();
    }

    @Override
    @Nullable
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        Expression<R> convExpr = this.expr.getConvertedExpression(to);
        if (convExpr == null) {
            return null;
        }
        ExprElement<T> exprElement = new ExprElement<T>();
        exprElement.expr = convExpr;
        exprElement.startIndex = this.startIndex;
        exprElement.endIndex = this.endIndex;
        exprElement.type = this.type;
        exprElement.queue = this.queue;
        return exprElement;
    }

    @Override
    public boolean isSingle() {
        return this.type != ElementType.FIRST_X_ELEMENTS && this.type != ElementType.LAST_X_ELEMENTS && this.type != ElementType.RANGE;
    }

    @Override
    public Class<? extends T> getReturnType() {
        if (this.queue) {
            return Object.class;
        }
        return this.expr.getReturnType();
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        if (!this.queue) {
            return this.expr.possibleReturnTypes();
        }
        return super.possibleReturnTypes();
    }

    @Override
    public boolean canReturn(Class<?> returnType) {
        if (!this.queue) {
            return this.expr.canReturn(returnType);
        }
        return super.canReturn(returnType);
    }

    @Override
    public Expression<? extends T> simplify() {
        if (!this.queue && this.expr instanceof Literal && this.type != ElementType.RANDOM && (this.startIndex == null || this.startIndex instanceof Literal) && (this.endIndex == null || this.endIndex instanceof Literal)) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Object prefix;
        switch (this.type.ordinal()) {
            case 0: {
                prefix = "the first";
                break;
            }
            case 1: {
                prefix = "the last";
                break;
            }
            case 2: {
                assert (this.startIndex != null);
                prefix = "the first " + this.startIndex.toString(event, debug);
                break;
            }
            case 3: {
                assert (this.startIndex != null);
                prefix = "the last " + this.startIndex.toString(event, debug);
                break;
            }
            case 4: {
                prefix = "a random";
                break;
            }
            case 5: 
            case 6: {
                Integer integer;
                assert (this.startIndex != null);
                prefix = "the ";
                prefix = this.startIndex instanceof Literal ? ((integer = (Integer)((Literal)this.startIndex).getSingle()) == null ? (String)prefix + this.startIndex.toString(event, debug) + "th" : (String)prefix + StringUtils.fancyOrderNumber(integer)) : (String)prefix + this.startIndex.toString(event, debug) + "th";
                if (this.type != ElementType.TAIL_END_ORDINAL) break;
                prefix = (String)prefix + " last";
                break;
            }
            case 7: {
                assert (this.startIndex != null && this.endIndex != null);
                return "the elements from " + this.startIndex.toString(event, debug) + " to " + this.endIndex.toString(event, debug) + " of " + this.expr.toString(event, debug);
            }
            default: {
                throw new IllegalStateException();
            }
        }
        return (String)prefix + (this.isSingle() ? " element" : " elements") + " of " + this.expr.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprElement.class, Object.class, ExpressionType.PROPERTY, PATTERNS.getPatterns());
    }

    private static enum ElementType {
        FIRST_ELEMENT(iterator -> Iterators.limit((Iterator)iterator, (int)1)),
        LAST_ELEMENT(iterator -> Iterators.singletonIterator((Object)Iterators.getLast((Iterator)iterator))),
        FIRST_X_ELEMENTS(Iterators::limit),
        LAST_X_ELEMENTS((iterator, index) -> {
            Object[] array = Iterators.toArray((Iterator)iterator, Object.class);
            index = Math.min(index, array.length);
            return Iterators.forArray((Object[])CollectionUtils.subarray(array, array.length - index, array.length));
        }),
        RANDOM(iterator -> {
            Object[] array = Iterators.toArray((Iterator)iterator, Object.class);
            if (array.length == 0) {
                return EmptyIterator.get();
            }
            Object element = CollectionUtils.getRandom(array);
            return Iterators.singletonIterator((Object)element);
        }),
        ORDINAL((iterator, index) -> {
            Iterators.advance((Iterator)iterator, (int)(index - 1));
            if (!iterator.hasNext()) {
                return EmptyIterator.get();
            }
            return Iterators.singletonIterator(iterator.next());
        }),
        TAIL_END_ORDINAL((iterator, index) -> {
            Object[] array = Iterators.toArray((Iterator)iterator, Object.class);
            if (index > array.length) {
                return EmptyIterator.get();
            }
            return Iterators.singletonIterator((Object)array[array.length - index]);
        }),
        RANGE((iterator, startIndex, endIndex) -> {
            boolean reverse = startIndex > endIndex;
            int from = Math.max(Math.min(startIndex, endIndex) - 1, 0);
            int to = Math.max(Math.max(startIndex, endIndex), 0);
            if (reverse) {
                Object[] array = Iterators.toArray((Iterator)iterator, Object.class);
                Object[] elements = CollectionUtils.subarray(array, from, to);
                ArrayUtils.reverse((Object[])elements);
                return Iterators.forArray((Object[])elements);
            }
            Iterators.advance((Iterator)iterator, (int)from);
            return Iterators.limit((Iterator)iterator, (int)(to - from));
        });

        private final TriFunction<Iterator<?>, Integer, Integer, Iterator<?>> function;

        private ElementType(Function<Iterator<?>, Iterator<?>> function) {
            this.function = (it, start, end) -> (Iterator)function.apply((Iterator<?>)it);
        }

        private ElementType(BiFunction<Iterator<?>, Integer, Iterator<?>> function) {
            this.function = (it, start, end) -> (Iterator)function.apply((Iterator<?>)it, (Integer)start);
        }

        private ElementType(TriFunction<Iterator<?>, Integer, Integer, Iterator<?>> function) {
            this.function = function;
        }

        public <T> Iterator<T> apply(Iterator<T> iterator, int startIndex, int endIndex) {
            return (Iterator)this.function.apply(iterator, (Object)startIndex, (Object)endIndex);
        }
    }
}

