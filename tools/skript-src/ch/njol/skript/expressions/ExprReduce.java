/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprInput;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.InputSource;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Reduce")
@Description(value={"Reduces lists to single values by repeatedly applying an operation.", "The reduce expression takes each element and combines it with an accumulator value.", "Use 'reduced value' to access the current accumulated value and 'input' for the current element."})
@Example.Examples(value={@Example(value="set {_sum} to {_numbers::*} reduced with [reduced value + input]"), @Example(value="set {_product} to {_values::*} reduced with [reduced value * input]"), @Example(value="set {_concatenated} to {_strings::*} reduced with [\"%reduced value%%input%\"]")})
@Since(value={"2.15"})
@Keywords(value={"input", "reduced value", "accumulator"})
public class ExprReduce
extends SimpleExpression<Object>
implements InputSource {
    private boolean keyed;
    private Expression<?> reduceExpr;
    private Expression<?> unreducedObjects;
    private final Set<ExprInput<?>> dependentInputs = new HashSet();
    @Nullable
    private Object currentValue;
    @Nullable
    private Object reducedValue;
    private @UnknownNullability String currentIndex;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.unreducedObjects = LiteralUtils.defendExpression(expressions[0]);
        if (this.unreducedObjects.isSingle()) {
            Skript.error("A single value cannot be reduced. Only lists can be reduced.");
            return false;
        }
        if (!LiteralUtils.canInitSafely(this.unreducedObjects) || parseResult.regexes.isEmpty()) {
            return false;
        }
        this.keyed = KeyProviderExpression.canReturnKeys(this.unreducedObjects);
        @Nullable String unparsedExpression = parseResult.regexes.getFirst().group();
        assert (unparsedExpression != null);
        this.reduceExpr = this.parseExpression(unparsedExpression, this.getParser(), 3);
        return this.reduceExpr != null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected Object @Nullable [] get(Event event) {
        try {
            boolean hadNullResult = false;
            if (this.keyed) {
                Iterator keyedIterator = ((KeyProviderExpression)this.unreducedObjects).keyedIterator(event);
                if (keyedIterator == null || !keyedIterator.hasNext()) {
                    Object[] objectArray = new Object[]{};
                    return objectArray;
                }
                KeyedValue first = keyedIterator.next();
                this.reducedValue = first.value();
                this.currentIndex = first.key();
                while (keyedIterator.hasNext()) {
                    KeyedValue next = keyedIterator.next();
                    this.currentValue = next.value();
                    this.currentIndex = next.key();
                    Object result = this.reduceExpr.getSingle(event);
                    if (result != null) {
                        this.reducedValue = result;
                        continue;
                    }
                    hadNullResult = true;
                }
            } else {
                this.currentIndex = null;
                Iterator iterator = this.unreducedObjects.iterator(event);
                if (iterator == null || !iterator.hasNext()) {
                    Object[] first = null;
                    return first;
                }
                this.reducedValue = iterator.next();
                int index = 1;
                while (iterator.hasNext()) {
                    this.currentValue = iterator.next();
                    this.currentIndex = String.valueOf(index);
                    Object result = this.reduceExpr.getSingle(event);
                    if (result != null) {
                        this.reducedValue = result;
                    } else {
                        hadNullResult = true;
                    }
                    ++index;
                }
            }
            if (hadNullResult) {
                this.warning("The reduce expression returned null for one or more elements, which were skipped.");
            }
            Object[] objectArray = new Object[]{this.reducedValue};
            return objectArray;
        }
        finally {
            this.currentValue = null;
            this.reducedValue = null;
            this.currentIndex = null;
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return this.reduceExpr.getReturnType();
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return this.reduceExpr.possibleReturnTypes();
    }

    @Override
    public boolean canReturn(Class<?> returnType) {
        return this.reduceExpr.canReturn(returnType);
    }

    @Override
    public Set<ExprInput<?>> getDependentInputs() {
        return this.dependentInputs;
    }

    @Override
    @Nullable
    public Object getCurrentValue() {
        return this.currentValue;
    }

    @Nullable
    public Object getReducedValue() {
        return this.reducedValue;
    }

    @Override
    public boolean hasIndices() {
        return this.keyed;
    }

    @Override
    public @UnknownNullability String getCurrentIndex() {
        return this.currentIndex;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.unreducedObjects.toString(event, debug) + " reduced with [" + this.reduceExpr.toString(event, debug) + "]";
    }

    static {
        Skript.registerExpression(ExprReduce.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "%objects% (reduced|folded) (to|with|by) \\[<.+>\\]", "%objects% (reduced|folded) (to|with|by) \\(<.+>\\)");
        if (!ParserInstance.isRegistered(InputSource.InputData.class)) {
            ParserInstance.registerData(InputSource.InputData.class, InputSource.InputData::new);
        }
    }
}

