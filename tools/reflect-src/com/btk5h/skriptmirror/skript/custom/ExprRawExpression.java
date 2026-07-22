/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import com.btk5h.skriptmirror.skript.custom.ExprExpression;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

public class ExprRawExpression
extends SimpleExpression<Expression> {
    private Expression<?> expr;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 2) {
            Skript.warning((String)"Using 'raw %objects%' is deprecated, please use 'the (raw|underlying) expression of %objects%' instead. If you meant to use Skript's 'raw %strings%' expression, try 'raw string within %objects%'.");
        }
        this.expr = SkriptUtil.defendExpression(exprs[0]);
        return SkriptUtil.canInitSafely(this.expr);
    }

    protected Expression<?>[] get(Event event) {
        Expression<?> expr = this.expr;
        if (expr instanceof ExprExpression) {
            ExprExpression exprExpr = (ExprExpression)expr;
            if (event instanceof CustomSyntaxEvent) {
                expr = exprExpr.getExpression(event);
                if (expr == null) {
                    return null;
                }
                expr = expr.getSource();
            }
        }
        return new Expression[]{expr};
    }

    public Class<?>[] acceptChange(Changer.ChangeMode changeMode) {
        Class[] classArray;
        if (this.expr instanceof ExprExpression) {
            Class[] classArray2 = new Class[1];
            classArray = classArray2;
            classArray2[0] = Object[].class;
        } else {
            classArray = null;
        }
        return classArray;
    }

    public void change(Event event, Object[] delta, Changer.ChangeMode changeMode) {
        ExprExpression exprExpression;
        block7: {
            block6: {
                Expression<?> expression = this.expr;
                if (!(expression instanceof ExprExpression)) break block6;
                exprExpression = (ExprExpression)expression;
                if (event instanceof CustomSyntaxEvent) break block7;
            }
            return;
        }
        CustomSyntaxEvent customEvent = (CustomSyntaxEvent)event;
        Expression<?> expr = exprExpression.getExpression(event);
        if (expr == null) {
            return;
        }
        Expression source = expr.getSource();
        Event unwrappedEvent = customEvent.getDirectEvent();
        try {
            source.acceptChange(changeMode);
            source.change(unwrappedEvent, delta, changeMode);
        }
        catch (Throwable throwable) {
            ExprJavaCall.lastError = throwable;
        }
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends Expression> getReturnType() {
        return Expression.class;
    }

    public String toString(Event event, boolean debug) {
        return "the underlying expression of " + this.expr.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprRawExpression.class, Expression.class, (ExpressionType)ExpressionType.PATTERN_MATCHES_EVERYTHING, (String[])new String[]{"[the] (raw|underlying) expression[s] of %objects%", "%objects%'[s] (raw|underlying) expression[s]", "[the] raw [expression] %objects%"});
    }
}

