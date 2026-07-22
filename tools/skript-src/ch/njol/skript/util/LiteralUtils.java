/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.util;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.registrations.Classes;
import java.util.stream.Stream;

public class LiteralUtils {
    public static <T> Expression<T> defendExpression(Expression<?> expr) {
        if (expr instanceof ExpressionList) {
            Expression<T>[] oldExpressions = ((ExpressionList)expr).getExpressions();
            Expression[] newExpressions = new Expression[oldExpressions.length];
            Class[] returnTypes = new Class[oldExpressions.length];
            for (int i = 0; i < oldExpressions.length; ++i) {
                newExpressions[i] = LiteralUtils.defendExpression(oldExpressions[i]);
                returnTypes[i] = newExpressions[i].getReturnType();
            }
            return new ExpressionList(newExpressions, Classes.getSuperClassInfo(returnTypes).getC(), returnTypes, expr.getAnd());
        }
        if (expr instanceof UnparsedLiteral) {
            Expression parsedLiteral = ((UnparsedLiteral)expr).getConvertedExpression(new Class[]{Object.class});
            return parsedLiteral == null ? expr : parsedLiteral;
        }
        return expr;
    }

    public static boolean hasUnparsedLiteral(Expression<?> expr) {
        if (expr instanceof UnparsedLiteral) {
            return true;
        }
        if (expr instanceof ExpressionList) {
            ExpressionList exprList = (ExpressionList)expr;
            return Stream.of(exprList.getExpressions()).anyMatch(LiteralUtils::hasUnparsedLiteral);
        }
        return false;
    }

    public static boolean canInitSafely(Expression<?> ... expressions) {
        for (Expression<?> expression : expressions) {
            if (expression != null && !LiteralUtils.hasUnparsedLiteral(expression)) continue;
            return false;
        }
        return true;
    }
}

