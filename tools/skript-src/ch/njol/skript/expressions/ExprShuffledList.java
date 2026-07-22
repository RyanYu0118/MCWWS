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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Shuffled List")
@Description(value={"Shuffles given list randomly."})
@Example(value="set {_list::*} to shuffled {_list::*}")
@Since(value={"2.2-dev32, 2.14 (retain indices when looping)"})
public class ExprShuffledList
extends SimpleExpression<Object>
implements KeyedIterableExpression<Object> {
    private Expression<?> list;
    private boolean keyed;

    public ExprShuffledList() {
    }

    public ExprShuffledList(Expression<?> list) {
        this.list = list;
        this.keyed = KeyedIterableExpression.canIterateWithKeys(list);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.list = LiteralUtils.defendExpression(expressions[0]);
        this.keyed = KeyedIterableExpression.canIterateWithKeys(this.list);
        return LiteralUtils.canInitSafely(this.list);
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        Object[] origin = (Object[])this.list.getArray(event).clone();
        List<Object> shuffled = Arrays.asList(origin);
        Collections.shuffle(shuffled);
        Object[] array = (Object[])Array.newInstance(this.getReturnType(), origin.length);
        return shuffled.toArray(array);
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
        List<KeyedValue> list = Arrays.asList((KeyedValue[])Iterators.toArray(iterator, KeyedValue.class));
        Collections.shuffle(list);
        return list.iterator();
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
            return new ExprShuffledList(convertedList);
        }
        return null;
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
        return "shuffled " + this.list.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprShuffledList.class, Object.class, ExpressionType.COMBINED, "shuffled %objects%");
    }
}

