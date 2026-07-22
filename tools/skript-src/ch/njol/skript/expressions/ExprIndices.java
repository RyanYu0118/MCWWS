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
import ch.njol.skript.expressions.ExprSortedList;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Indices of List")
@Description(value={"Returns all the indices of a list variable, optionally sorted by their values.", "To sort the indices, all objects in the list must be comparable;", "Otherwise, this expression will just return the unsorted indices."})
@Example(value="set {l::*} to \"some\", \"cool\" and \"values\"\nbroadcast \"%indices of {l::*}%\" # result is 1, 2 and 3\", \"\nset {_leader-board::first} to 17\nset {_leader-board::third} to 30\nset {_leader-board::second} to 25\nset {_leader-board::fourth} to 42\nset {_ascending-indices::*} to sorted indices of {_leader-board::*} in ascending order\nbroadcast \"%{_ascending-indices::*}%\" #result is first, second, third, fourth\nset {_descending-indices::*} to sorted indices of {_leader-board::*} in descending order\nbroadcast \"%{_descending-indices::*}%\" #result is fourth, third, second, first\n")
@Since(value={"2.4 (indices), 2.6.1 (sorting)"})
public class ExprIndices
extends SimpleExpression<String> {
    private KeyProviderExpression<?> keyedExpression;
    private boolean sort;
    private boolean descending;
    private boolean recursive;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.sort = matchedPattern > 1;
        this.descending = parseResult.mark == 1;
        Expression expression = LiteralUtils.defendExpression(exprs[0]);
        if (!KeyProviderExpression.canReturnKeys(expression)) {
            Skript.error("The indices expression may only be used with keyed expressions");
            return false;
        }
        this.keyedExpression = (KeyProviderExpression)exprs[0];
        this.recursive = exprs[0].returnsNestedStructures();
        if (!this.sort) {
            exprs[0].returnNestedStructures(true);
        }
        return true;
    }

    protected String @Nullable [] get(Event event) {
        T[] values = this.keyedExpression.getArray(event);
        String[] keys = this.keyedExpression.getArrayKeys(event);
        if (this.sort) {
            int direction = this.descending ? -1 : 1;
            return (String[])Arrays.stream(KeyedValue.zip(values, keys)).sorted((a, b) -> ExprSortedList.compare(a.value(), b.value()) * direction).map(KeyedValue::key).toArray(String[]::new);
        }
        if (this.recursive) {
            return keys;
        }
        for (int i = 0; i < keys.length; ++i) {
            int separator = keys[i].indexOf("::");
            if (separator == -1) continue;
            keys[i] = keys[i].substring(0, keys[i].indexOf("::"));
        }
        return (String[])Arrays.stream(keys).distinct().toArray(String[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public boolean returnNestedStructures(boolean nested) {
        return this.keyedExpression.returnNestedStructures(nested);
    }

    @Override
    public boolean returnsNestedStructures() {
        return this.keyedExpression.returnsNestedStructures();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String text = "indices of " + this.keyedExpression.toString(event, debug);
        if (this.sort) {
            text = "sorted " + text + " in " + (this.descending ? "descending" : "ascending") + " order";
        }
        return text;
    }

    static {
        Skript.registerExpression(ExprIndices.class, String.class, ExpressionType.COMBINED, "[(the|all [[of] the])] (indexes|indices) of %~objects%", "%~objects%'[s] (indexes|indices)", "[sorted] (indices|indexes) of %~objects% in (ascending|1\u00a6descending) order", "[sorted] %~objects%'[s] (indices|indexes) in (ascending|1\u00a6descending) order");
    }
}

