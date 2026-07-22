/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.skript.lang.util.SimpleLiteral
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.skript.util.Utils
 *  ch.njol.util.Kleenean
 *  ch.njol.util.coll.iterator.ArrayIterator
 *  org.bukkit.event.Event
 *  org.skriptlang.skript.lang.converter.Converters
 */
package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionSyntaxInfo;
import org.skriptlang.reflect.syntax.expression.elements.StructCustomExpression;
import org.skriptlang.skript.lang.converter.Converters;

public class CustomExpression<T>
implements Expression<T> {
    private ExpressionSyntaxInfo which;
    private Expression<?>[] exprs;
    private SkriptParser.ParseResult parseResult;
    private Object variablesMap;
    private final CustomExpression<?> source;
    private Class<? extends T>[] types;
    private Class<T> superType;

    public CustomExpression() {
        this(null, Object.class);
    }

    @SafeVarargs
    private CustomExpression(CustomExpression<?> source, Class<? extends T> ... types) {
        this.source = source;
        if (source != null) {
            this.which = source.which;
            this.exprs = source.exprs;
            this.parseResult = source.parseResult;
            this.variablesMap = source.variablesMap;
        }
        this.types = types;
        this.superType = Utils.getSuperType((Class[])types);
    }

    public T getSingle(Event e) {
        T[] all = this.getAll(e);
        return all.length == 0 ? null : (T)all[0];
    }

    public T[] getArray(Event e) {
        return this.getAll(e);
    }

    public T[] getAll(Event e) {
        Trigger getter = StructCustomExpression.expressionHandlers.get(this.which);
        if (getter == null) {
            Skript.error((String)String.format("The custom expression '%s' no longer has a get handler.", this.which.getPattern()));
            return JavaUtil.newArray(this.superType, 0);
        }
        if (this.which.isProperty()) {
            return this.getByProperty(e, getter);
        }
        return this.getByStandard(e, getter);
    }

    private T[] getByStandard(Event e, Trigger getter) {
        ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, this.exprs, this.which.getMatchedPattern(), this.parseResult);
        SkriptReflection.putLocals(this.variablesMap, expressionEvent);
        getter.execute((Event)expressionEvent);
        if (expressionEvent.getOutput() == null) {
            Skript.error((String)String.format("The get handler for '%s' did not return.", this.which.getPattern()));
            return JavaUtil.newArray(this.superType, 0);
        }
        return Converters.convert((Object[])expressionEvent.getOutput(), (Class[])this.types, this.superType);
    }

    private T[] getByProperty(Event e, Trigger getter) {
        ArrayList<Object> output = new ArrayList<Object>();
        for (Object o : this.exprs[0].getArray(e)) {
            Expression<?>[] localExprs = Arrays.copyOf(this.exprs, this.exprs.length);
            localExprs[0] = new SimpleLiteral(o, false);
            ExpressionGetEvent expressionEvent = new ExpressionGetEvent(e, localExprs, this.which.getMatchedPattern(), this.parseResult);
            SkriptReflection.putLocals(this.variablesMap, expressionEvent);
            getter.execute((Event)expressionEvent);
            Object[] exprOutput = expressionEvent.getOutput();
            if (exprOutput == null) {
                Skript.error((String)String.format("The get handler for '%s' did not return for the value %s", this.which.getPattern(), Classes.toString((Object)o)));
                return JavaUtil.newArray(this.superType, 0);
            }
            if (!this.which.isAlwaysPlural() && exprOutput.length > 1) {
                Skript.error((String)String.format("The get handler for '%s' returned more than one value.", this.which.getPattern()));
                return JavaUtil.newArray(this.superType, 0);
            }
            Object[] converted = Converters.convert((Object[])exprOutput, this.superType);
            output.addAll(Arrays.asList(converted));
        }
        return output.toArray(JavaUtil.newArray(this.superType, 0));
    }

    public boolean isSingle() {
        return !this.which.isAlwaysPlural() && Arrays.stream(this.which.getInheritedSingles()).mapToObj(i -> this.exprs[i]).filter(Objects::nonNull).allMatch(Expression::isSingle);
    }

    public boolean check(Event e, Predicate<? super T> c, boolean negated) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)negated, (boolean)this.getAnd());
    }

    public boolean check(Event e, Predicate<? super T> c) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)false, (boolean)this.getAnd());
    }

    public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
        if (StructCustomExpression.returnTypes.containsKey(this.which) && !Converters.converterExists(StructCustomExpression.returnTypes.get(this.which), (Class[])to)) {
            return null;
        }
        return new CustomExpression<R>(this, to);
    }

    public Class<T> getReturnType() {
        return this.superType;
    }

    public boolean getAnd() {
        return true;
    }

    public boolean setTime(int time) {
        return false;
    }

    public int getTime() {
        return 0;
    }

    public boolean isDefault() {
        return false;
    }

    public Iterator<? extends T> iterator(Event e) {
        return new ArrayIterator((Object[])this.getAll(e));
    }

    public boolean isLoopOf(String s) {
        return s.equalsIgnoreCase(StructCustomExpression.loopOfs.get(this.which));
    }

    public Expression<?> getSource() {
        return this.source == null ? this : this.source;
    }

    public Expression<? extends T> simplify() {
        return this;
    }

    public String toString(Event e, boolean debug) {
        return this.which.getPattern();
    }

    public String toString() {
        return this.toString(null, false);
    }

    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (StructCustomExpression.hasChanger.containsKey(this.which) && StructCustomExpression.hasChanger.get(this.which).contains(mode)) {
            return StructCustomExpression.changerTypes.getOrDefault(this.which, Collections.emptyMap()).getOrDefault(mode, new Class[]{Object[].class});
        }
        return null;
    }

    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
        Trigger changer = (Trigger)StructCustomExpression.changerHandlers.getOrDefault(this.which, Collections.emptyMap()).get(mode);
        if (changer == null) {
            Skript.error((String)String.format("The custom expression '%s' no longer has a %s handler.", this.which.getPattern(), mode.name()));
        } else {
            ExpressionChangeEvent changeEvent = new ExpressionChangeEvent(e, this.exprs, this.which.getMatchedPattern(), this.parseResult, delta);
            SkriptReflection.putLocals(SkriptReflection.copyLocals(this.variablesMap), changeEvent);
            changer.execute((Event)changeEvent);
        }
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        List<Supplier<Boolean>> suppliers;
        if (matchedPattern == 0) {
            return false;
        }
        this.which = StructCustomExpression.lookup(SkriptUtil.getCurrentScript(), matchedPattern);
        if (this.which == null) {
            return false;
        }
        if (this.which.shouldAdaptArgument()) {
            Expression<?> lastExpression = exprs[exprs.length - 1];
            System.arraycopy(exprs, 0, exprs, 1, exprs.length - 1);
            exprs[0] = lastExpression;
        }
        this.exprs = (Expression[])Arrays.stream(exprs).map(SkriptUtil::defendExpression).toArray(Expression[]::new);
        this.parseResult = parseResult;
        if (!SkriptUtil.canInitSafely(this.exprs)) {
            return false;
        }
        Class<?> returnType = StructCustomExpression.returnTypes.get(this.which);
        if (returnType != null) {
            this.types = new Class[]{returnType};
            this.superType = returnType;
        }
        if ((suppliers = StructCustomExpression.usableSuppliers.get(this.which)) != null && suppliers.size() != 0 && suppliers.stream().noneMatch(Supplier::get)) {
            return false;
        }
        Boolean bool = StructCustomExpression.parseSectionLoaded.get(this.which);
        if (bool != null && !bool.booleanValue()) {
            Skript.error((String)"You can't use custom expressions with parse sections before they're loaded.");
            return false;
        }
        Trigger parseHandler = StructCustomExpression.parserHandlers.get(this.which);
        if (parseHandler != null) {
            SyntaxParseEvent event = new SyntaxParseEvent(this.exprs, matchedPattern, parseResult, this.getParser().getCurrentEvents());
            TriggerItem.walk((TriggerItem)parseHandler, (Event)event);
            this.variablesMap = SkriptReflection.removeLocals(event);
            return event.isMarkedContinue();
        }
        return true;
    }
}

