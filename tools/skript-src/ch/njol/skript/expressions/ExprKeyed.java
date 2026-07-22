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

@Name(value="Keyed")
@Description(value={"This expression is used to explicitly pass the keys of an expression alongside its values.", "For example, when setting a list variable or passing an expression to a function."})
@Example.Examples(value={@Example(value="set {_first::foo} to \"value1\"\nset {_first::bar} to \"value2\"\nset {_second::*} to keyed {_first::*}\n# {_second::foo} is \"value1\" and {_second::bar} is \"value2\"\n"), @Example(value="function indices(objects: objects) returns strings:\n\treturn indices of {_objects::*}\n\non load:\n\tset {_list::foo} to \"value1\"\n\tset {_list::bar} to \"value2\"\n\tset {_list::baz} to \"value3\"\n\n\tbroadcast indices({_list::*}) # \"1\", \"2\", \"3\"\n\tbroadcast indices(keyed {_list::*}) # \"foo\", \"bar\", \"baz\"\n"), @Example(value="function plusOne(numbers: numbers) returns numbers:\n\tloop {_numbers::*}:\n\t\tset {_numbers::%loop-index%} to loop-value + 1\n\treturn {_numbers::*}\n\non load:\n\tset {_numbers::foo} to 1\n\tset {_numbers::bar} to 2\n\tset {_numbers::baz} to 3\n\n\tset {_result::*} to keyed plusOne(keyed {_numbers::*})\n\t# {_result::foo} is 2, {_result::bar} is 3, {_result::baz} is 4\n")})
@Since(value={"2.12"})
@Keywords(value={"indexed"})
public class ExprKeyed
extends WrapperExpression<Object>
implements KeyProviderExpression<Object> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!KeyProviderExpression.canReturnKeys(expressions[0])) {
            Skript.error(String.valueOf(expressions[0]) + " is not a keyed expression.");
            return false;
        }
        this.setExpr(expressions[0]);
        return true;
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
        return this.getExpr().getArrayKeys(event);
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getAllKeys(Event event) {
        return this.getExpr().getAllKeys(event);
    }

    @Override
    public Iterator<KeyedValue<Object>> keyedIterator(Event event) {
        return this.getExpr().keyedIterator(event);
    }

    @Override
    public boolean isIndexLoop(String input) {
        return this.getExpr().isIndexLoop(input);
    }

    @Override
    public boolean isLoopOf(String input) {
        return this.getExpr().isLoopOf(input);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "keyed " + this.getExpr().toString(event, debug);
    }

    @Override
    public KeyProviderExpression<?> getExpr() {
        return (KeyProviderExpression)super.getExpr();
    }

    static {
        Skript.registerExpression(ExprKeyed.class, Object.class, ExpressionType.COMBINED, "(keyed|indexed) %~objects%");
    }
}

