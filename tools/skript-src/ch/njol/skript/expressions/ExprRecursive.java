/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.Iterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Recursive")
@Description(value={"Returns all values of an expression, including those in nested structures such as lists of lists."})
@Example(value="on load:\n\tset {_data::a::b::c} to \"value1\"\n\tset {_data::a::b::d} to \"value2\"\n\tset {_data::a::e} to \"value3\"\n\tset {_data::f} to \"value4\"\n\n\tbroadcast recursive {_data::*}\n\t# broadcasts \"value1\", \"value2\", \"value3\", \"value4\"\n\n\tbroadcast recursive indices of {_data::*}\n\t# broadcasts \"a::b::c\", \"a::b::d\", \"a::e\", \"f\"\n")
@Since(value={"2.14"})
@Keywords(value={"deep", "nested"})
public class ExprRecursive
extends WrapperExpression<Object>
implements KeyProviderExpression<Object> {
    private boolean returnsKeys;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!expressions[0].returnNestedStructures(true)) {
            Skript.error(String.valueOf(expressions[0]) + " does not support nested structures.");
            return false;
        }
        this.setExpr(expressions[0]);
        this.returnsKeys = KeyProviderExpression.canReturnKeys(this.getExpr());
        return true;
    }

    @Override
    public Object[] getAll(Event event) {
        return this.getExpr().getAll(event);
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
        if (!this.returnsKeys) {
            throw new UnsupportedOperationException();
        }
        return ((KeyProviderExpression)this.getExpr()).getArrayKeys(event);
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getAllKeys(Event event) {
        if (!this.returnsKeys) {
            throw new UnsupportedOperationException();
        }
        return ((KeyProviderExpression)this.getExpr()).getAllKeys(event);
    }

    @Override
    public Iterator<KeyedValue<Object>> keyedIterator(Event event) {
        if (!this.returnsKeys) {
            throw new UnsupportedOperationException();
        }
        return ((KeyProviderExpression)this.getExpr()).keyedIterator(event);
    }

    @Override
    public boolean canReturnKeys() {
        return this.returnsKeys;
    }

    @Override
    public boolean areKeysRecommended() {
        return KeyProviderExpression.areKeysRecommended(this.getExpr());
    }

    @Override
    public boolean isIndexLoop(String input) {
        if (!this.returnsKeys) {
            throw new IllegalStateException();
        }
        return ((KeyProviderExpression)this.getExpr()).isIndexLoop(input);
    }

    @Override
    public boolean isLoopOf(String input) {
        return this.getExpr().isLoopOf(input);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "recursive " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprRecursive.class, Object.class, ExpressionType.COMBINED, "recursive %~objects%");
    }
}

