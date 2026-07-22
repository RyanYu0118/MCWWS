/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import java.util.Map;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Recursive Size")
@Description(value={"The recursive size of list.", "Returns the recursive size of the list with sublists included, e.g.", "", "<pre>", "{list::*} Structure<br>", "  \u251c\u2500\u2500\u2500\u2500 {list::1}: 1<br>", "  \u251c\u2500\u2500\u2500\u2500 {list::2}: 2<br>", "  \u2502     \u251c\u2500\u2500\u2500\u2500 {list::2::1}: 3<br>", "  \u2502     \u2502    \u2514\u2500\u2500\u2500\u2500 {list::2::1::1}: 4<br>", "  \u2502     \u2514\u2500\u2500\u2500\u2500 {list::2::2}: 5<br>", "  \u2514\u2500\u2500\u2500\u2500 {list::3}: 6", "</pre>", "", "Where using %size of {list::*}% will only return 3 (the first layer of indices only), while %recursive size of {list::*}% will return 6 (the entire list)", "Please note that getting a list's recursive size can cause lag if the list is large, so only use this expression if you need to!"})
@Example(value="if recursive size of {player-data::*} > 1000:")
@Since(value={"1.0"})
public class ExprRecursiveSize
extends SimpleExpression<Long> {
    private ExpressionList<?> exprs;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprRecursiveSize.class, Long.class).supplier(ExprRecursiveSize::new)).priority(PropertyExpression.DEFAULT_PRIORITY)).addPattern("[the] recursive (amount|number|size) of %objects%")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ExpressionList<Object> expressionList;
        Expression<?> expression = expressions[0];
        if (expression instanceof ExpressionList) {
            ExpressionList exprList = (ExpressionList)expression;
            expressionList = exprList;
        } else {
            expressionList = new ExpressionList<Object>(new Expression[]{expressions[0]}, Object.class, false);
        }
        this.exprs = expressionList;
        this.exprs = (ExpressionList)LiteralUtils.defendExpression(this.exprs);
        if (!LiteralUtils.canInitSafely(this.exprs)) {
            return false;
        }
        if (this.exprs.isSingle()) {
            Skript.error("'" + this.exprs.toString(null, Skript.debug()) + "' can only ever have one value at most, thus the 'recursive size of ...' expression is useless. Use '... exists' instead to find out whether the expression has a value.");
            return false;
        }
        for (Expression<?> expr : this.exprs.getExpressions()) {
            if (expr instanceof Variable) continue;
            Skript.error("Getting the recursive size of a list only applies to variables, thus the '" + expr.toString(null, Skript.debug()) + "' expression is useless.");
            return false;
        }
        return true;
    }

    protected Long @Nullable [] get(Event event) {
        int currentSize = 0;
        for (Expression<?> expr : this.exprs.getExpressions()) {
            Object var = ((Variable)expr).getRaw(event);
            if (var == null) continue;
            currentSize += ExprRecursiveSize.getRecursiveSize((Map)var);
        }
        return new Long[]{currentSize};
    }

    private static int getRecursiveSize(Map<?, ?> map) {
        return ExprRecursiveSize.getRecursiveSize(map, true);
    }

    private static int getRecursiveSize(Map<?, ?> map, boolean skipNull) {
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (skipNull && entry.getKey() == null) continue;
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map nestedMap = (Map)value;
                count += ExprRecursiveSize.getRecursiveSize(nestedMap, false);
                continue;
            }
            ++count;
        }
        return count;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "recursive size of " + this.exprs.toString(event, debug);
    }
}

