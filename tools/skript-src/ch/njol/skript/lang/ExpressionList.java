/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExpressionList<T>
implements Expression<T> {
    protected final Expression<? extends T>[] expressions;
    private final Class<T> returnType;
    private final Class<?>[] possibleReturnTypes;
    protected boolean and;
    private final boolean single;
    @Nullable
    private final ExpressionList<?> source;
    private int time = 0;

    public ExpressionList(Expression<? extends T>[] expressions, Class<T> returnType, boolean and) {
        this(expressions, returnType, and, null);
    }

    public ExpressionList(Expression<? extends T>[] expressions, Class<T> returnType, Class<?>[] possibleReturnTypes, boolean and) {
        this(expressions, returnType, possibleReturnTypes, and, null);
    }

    protected ExpressionList(Expression<? extends T>[] expressions, Class<T> returnType, boolean and, @Nullable ExpressionList<?> source) {
        this(expressions, returnType, new Class[]{returnType}, and, source);
    }

    protected ExpressionList(Expression<? extends T>[] expressions, Class<T> returnType, Class<?>[] possibleReturnTypes, boolean and, @Nullable ExpressionList<?> source) {
        assert (expressions != null);
        this.expressions = expressions;
        this.returnType = returnType;
        this.possibleReturnTypes = (Class[])ImmutableSet.copyOf((Object[])possibleReturnTypes).toArray((Object[])new Class[0]);
        this.and = and;
        if (and) {
            this.single = false;
        } else {
            boolean single = true;
            for (Expression<T> expression : expressions) {
                if (expression.isSingle()) continue;
                single = false;
                break;
            }
            this.single = single;
        }
        this.source = source;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public T getSingle(Event event) {
        if (!this.single) {
            throw new UnsupportedOperationException();
        }
        Expression<? extends T> expression = CollectionUtils.getRandom(this.expressions);
        return expression != null ? (T)expression.getSingle(event) : null;
    }

    @Override
    public T[] getArray(Event event) {
        if (this.and) {
            return this.getAll(event);
        }
        Expression<T> expression = CollectionUtils.getRandom(this.expressions);
        return expression != null ? expression.getArray(event) : (Object[])Array.newInstance(this.returnType, 0);
    }

    @Override
    public T[] getAll(Event event) {
        ArrayList<T> values = new ArrayList<T>();
        for (Expression<T> expression : this.expressions) {
            values.addAll(Arrays.asList(expression.getAll(event)));
        }
        return values.toArray((Object[])Array.newInstance(this.returnType, values.size()));
    }

    @Override
    @Nullable
    public Iterator<? extends T> iterator(final Event event) {
        if (!this.and) {
            Expression<T> expression = CollectionUtils.getRandom(this.expressions);
            return expression != null ? expression.iterator(event) : null;
        }
        return new Iterator<T>(this){
            private int i = 0;
            @Nullable
            private Iterator<? extends T> current = null;
            final /* synthetic */ ExpressionList this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            public boolean hasNext() {
                Iterator iterator = this.current;
                while (!(this.i >= this.this$0.expressions.length || iterator != null && iterator.hasNext())) {
                    iterator = this.this$0.expressions[this.i++].iterator(event);
                    this.current = iterator;
                }
                return iterator != null && iterator.hasNext();
            }

            @Override
            public T next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                Iterator iterator = this.current;
                if (iterator == null) {
                    throw new NoSuchElementException();
                }
                Object value = iterator.next();
                assert (value != null) : this.current;
                return value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean isSingle() {
        return this.single;
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker, boolean negated) {
        return CollectionUtils.check(this.expressions, expr -> expr.check(event, checker) ^ negated, this.and);
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker) {
        return this.check(event, checker, false);
    }

    @Override
    @Nullable
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        Expression[] exprs = new Expression[this.expressions.length];
        HashSet possibleReturnTypeSet = new HashSet();
        for (int i = 0; i < exprs.length; ++i) {
            exprs[i] = this.expressions[i].getConvertedExpression(to);
            if (exprs[i] == null) {
                return null;
            }
            possibleReturnTypeSet.addAll(Arrays.asList(exprs[i].possibleReturnTypes()));
        }
        Class[] possibleReturnTypes = possibleReturnTypeSet.toArray(new Class[0]);
        return new ExpressionList(exprs, Classes.getSuperClassInfo(possibleReturnTypes).getC(), possibleReturnTypes, this.and, this);
    }

    @Override
    public Class<T> getReturnType() {
        return this.returnType;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        return this.possibleReturnTypes;
    }

    @Override
    public boolean getAnd() {
        return this.and;
    }

    public void invertAnd() {
        this.and = !this.and;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        ArrayList<Class<?>[]> expressionTypes = new ArrayList<Class<?>[]>();
        for (Expression<T> expression : this.expressions) {
            Class<?>[] classArray = expression.acceptChange(mode);
            if (classArray == null) {
                return null;
            }
            expressionTypes.add(classArray);
        }
        if (expressionTypes.size() == 1) {
            return (Class[])expressionTypes.get(0);
        }
        LinkedHashSet<Class<Object>> acceptable = new LinkedHashSet<Class>(Arrays.asList((Class[])expressionTypes.get(0)));
        for (int i = 1; i < expressionTypes.size(); ++i) {
            LinkedHashSet<Class> newAcceptable = new LinkedHashSet<Class>();
            block2: for (Class clazz : acceptable) {
                for (Class accepted : (Class[])expressionTypes.get(i)) {
                    if (accepted.isAssignableFrom(clazz)) {
                        newAcceptable.add(clazz);
                        continue block2;
                    }
                    if (!clazz.isAssignableFrom(accepted)) continue;
                    newAcceptable.add(accepted);
                    continue block2;
                }
            }
            acceptable = newAcceptable;
            if (!acceptable.isEmpty()) continue;
            return new Class[0];
        }
        return acceptable.toArray(new Class[0]);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        if (this.and) {
            for (Expression<T> expression : this.expressions) {
                expression.change(event, delta, mode);
            }
        } else {
            int i = ThreadLocalRandom.current().nextInt(this.expressions.length);
            this.expressions[i].change(event, delta, mode);
        }
    }

    @Override
    public <R> void changeInPlace(Event event, Function<T, R> changeFunction, boolean getAll) {
        if (this.and || getAll) {
            for (Expression<T> expression : this.expressions) {
                expression.changeInPlace(event, changeFunction, getAll);
            }
        } else {
            int i = ThreadLocalRandom.current().nextInt(this.expressions.length);
            this.expressions[i].changeInPlace(event, changeFunction, false);
        }
    }

    @Override
    public boolean setTime(int time) {
        boolean ok = false;
        for (Expression<T> expression : this.expressions) {
            ok |= expression.setTime(time);
        }
        if (ok) {
            this.time = time;
        }
        return ok;
    }

    @Override
    public int getTime() {
        return this.time;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean isLoopOf(String input) {
        for (Expression<T> expression : this.expressions) {
            if (!expression.isLoopOf(input)) continue;
            return true;
        }
        return false;
    }

    @Override
    public Expression<?> getSource() {
        ExpressionList<?> source = this.source;
        return source == null ? this : source;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < this.expressions.length; ++i) {
            if (i != 0) {
                if (i == this.expressions.length - 1) {
                    result.append(this.and ? " and " : " or ");
                } else {
                    result.append(", ");
                }
            }
            result.append(this.expressions[i].toString(event, debug));
        }
        result.append(")");
        if (debug) {
            result.append("[").append(this.returnType).append("]");
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return this.toString(null, false);
    }

    public Expression<? extends T>[] getExpressions() {
        return this.expressions;
    }

    public List<Expression<? extends T>> getAllExpressions() {
        ArrayList<Expression<T>> expressions = new ArrayList<Expression<T>>();
        for (Expression<? extends T> expression : this.expressions) {
            if (expression instanceof ExpressionList) {
                ExpressionList innerList = (ExpressionList)expression;
                expressions.addAll(innerList.getAllExpressions());
                continue;
            }
            expressions.add(expression);
        }
        return expressions;
    }

    @Override
    public Expression<T> simplify() {
        boolean isLiteralList = true;
        for (int i = 0; i < this.expressions.length; ++i) {
            this.expressions[i] = this.expressions[i].simplify();
            isLiteralList &= this.expressions[i] instanceof Literal;
        }
        if (isLiteralList) {
            Literal[] ls = (Literal[])Arrays.copyOf(this.expressions, this.expressions.length, Literal[].class);
            return new LiteralList<T>(ls, this.returnType, this.and);
        }
        return this;
    }
}

