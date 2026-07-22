/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
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
import ch.njol.skript.lang.KeyedIterableExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Reversed List")
@Description(value={"Reverses given list."})
@Example(value="set {_list::*} to reversed {_list::*}")
@Since(value={"2.4, 2.14 (retain indices when looping)"})
public class ExprReversedList
extends SimpleExpression<Object>
implements KeyedIterableExpression<Object> {
    private Expression<?> list;
    private boolean keyed;

    public ExprReversedList() {
    }

    public ExprReversedList(Expression<?> list) {
        this.list = list;
        this.keyed = KeyedIterableExpression.canIterateWithKeys(list);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.list = LiteralUtils.defendExpression(expressions[0]);
        if (this.list.isSingle()) {
            Skript.error("A single object cannot be reversed.");
            return false;
        }
        this.keyed = KeyedIterableExpression.canIterateWithKeys(this.list);
        return LiteralUtils.canInitSafely(this.list);
    }

    @Override
    @Nullable
    protected Object[] get(Event event) {
        Object[] array = this.list.getArray(event);
        this.reverse(array);
        return array;
    }

    @Override
    @Nullable
    public Iterator<?> iterator(Event event) {
        final List<?> list = Arrays.asList(this.list.getArray(event));
        return new Iterator<Object>(this){
            private final ListIterator<?> listIterator;
            {
                this.listIterator = list.listIterator(list.size());
            }

            @Override
            public boolean hasNext() {
                return this.listIterator.hasPrevious();
            }

            @Override
            public Object next() {
                return this.listIterator.previous();
            }
        };
    }

    @Override
    public boolean canIterateWithKeys() {
        return this.keyed;
    }

    @Override
    public Iterator<KeyedValue<Object>> keyedIterator(Event event) {
        if (!this.keyed) {
            throw new UnsupportedOperationException();
        }
        Iterator iterator = ((KeyedIterableExpression)this.list).keyedIterator(event);
        final List<KeyedValue> list = Arrays.asList((KeyedValue[])Iterators.toArray(iterator, KeyedValue.class));
        return new Iterator<KeyedValue<Object>>(this){
            private final ListIterator<KeyedValue<?>> listIterator;
            {
                this.listIterator = list.listIterator(list.size());
            }

            @Override
            public boolean hasNext() {
                return this.listIterator.hasPrevious();
            }

            @Override
            public KeyedValue<Object> next() {
                return this.listIterator.previous();
            }
        };
    }

    @Override
    @SafeVarargs
    @Nullable
    public final <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        if (CollectionUtils.containsSuperclass(to, this.getReturnType())) {
            return this;
        }
        Expression<R> convertedList = this.list.getConvertedExpression(to);
        if (convertedList != null) {
            return new ExprReversedList(convertedList);
        }
        return null;
    }

    private void reverse(Object[] array) {
        for (int i = 0; i < array.length / 2; ++i) {
            Object temp = array[i];
            int reverse = array.length - i - 1;
            array[i] = array[reverse];
            array[reverse] = temp;
        }
    }

    @Override
    public Class<?> getReturnType() {
        return this.list.getReturnType();
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return this.list.possibleReturnTypes();
    }

    @Override
    public boolean canReturn(Class<?> returnType) {
        return this.list.canReturn(returnType);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean isIndexLoop(String input) {
        if (!this.keyed) {
            throw new IllegalStateException();
        }
        return ((KeyedIterableExpression)this.list).isIndexLoop(input);
    }

    @Override
    public boolean isLoopOf(String input) {
        return this.list.isLoopOf(input);
    }

    @Override
    public Expression<?> simplify() {
        if (this.list instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "reversed " + this.list.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprReversedList.class, Object.class, ExpressionType.COMBINED, "reversed %objects%");
    }
}

