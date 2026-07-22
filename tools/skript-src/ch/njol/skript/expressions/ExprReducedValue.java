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
import ch.njol.skript.expressions.ExprReduce;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.InputSource;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Reduced Value")
@Description(value={"Returns the current accumulated/reduced value within a <a href='#ExprReduce'>reduce expression</a>.", "This represents the result of all previous reduction operations.", "Can only be used inside the reduce expression's operation block."})
@Example.Examples(value={@Example(value="set {_sum} to {_numbers::*} reduced with [reduced value + input]"), @Example(value="set {_max} to {_values::*} reduced with [reduced value if reduced value > input else input]"), @Example(value="set {_combined} to {_items::*} reduced with (\"%reduced value%, %input%\")")})
@Since(value={"2.15"})
public class ExprReducedValue
extends SimpleExpression<Object> {
    private ExprReduce reduce;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        InputSource inputSource = this.getParser().getData(InputSource.InputData.class).getSource();
        if (!(inputSource instanceof ExprReduce)) {
            Skript.error("The 'reduced value' expression can only be used within a reduce operation");
            return false;
        }
        ExprReduce exprReduce = (ExprReduce)inputSource;
        this.reduce = exprReduce;
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        Object[] objectArray;
        Object reducedValue = this.reduce.getReducedValue();
        if (reducedValue == null) {
            objectArray = new Object[]{};
        } else {
            Object[] objectArray2 = new Object[1];
            objectArray = objectArray2;
            objectArray2[0] = reducedValue;
        }
        return objectArray;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "reduced value";
    }

    static {
        Skript.registerExpression(ExprReducedValue.class, Object.class, ExpressionType.SIMPLE, "[the] reduced value", "[the] (accumulator|accumulated) [value]", "[the] folded value");
    }
}

