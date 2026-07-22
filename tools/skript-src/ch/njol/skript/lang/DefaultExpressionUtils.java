/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.StringUtils;
import java.util.List;
import org.jetbrains.annotations.Nullable;

final class DefaultExpressionUtils {
    DefaultExpressionUtils() {
    }

    @Nullable
    static DefaultExpressionError isValid(DefaultExpression<?> expr, SkriptParser.ExprInfo exprInfo, int index) {
        if (expr == null) {
            return DefaultExpressionError.NOT_FOUND;
        }
        if (!(expr instanceof Literal) && (exprInfo.flagMask & 1) == 0) {
            return DefaultExpressionError.NOT_LITERAL;
        }
        if (expr instanceof Literal && (exprInfo.flagMask & 2) == 0) {
            return DefaultExpressionError.LITERAL;
        }
        if (!exprInfo.isPlural[index] && !expr.isSingle()) {
            return DefaultExpressionError.NOT_SINGLE;
        }
        if (exprInfo.time != 0 && !expr.setTime(exprInfo.time)) {
            return DefaultExpressionError.TIME_STATE;
        }
        return null;
    }

    static abstract sealed class DefaultExpressionError
    extends Enum<DefaultExpressionError> {
        public static final /* enum */ DefaultExpressionError NOT_FOUND = new DefaultExpressionError(){

            @Override
            public String getError(List<String> codeNames, String pattern) {
                StringBuilder builder = new StringBuilder();
                String combinedComma = DefaultExpressionError.getCombinedComma(codeNames);
                String combinedSlash = StringUtils.join(codeNames, "/");
                builder.append(DefaultExpressionError.plurality(codeNames, "The class '", "The classes '"));
                builder.append(combinedComma).append("'").append(DefaultExpressionError.plurality(codeNames, " does ", " do ")).append("not provide a default expression. Either allow null (with %-").append(combinedSlash).append("%) or make it mandatory [pattern: ").append(pattern).append("]");
                return builder.toString();
            }
        };
        public static final /* enum */ DefaultExpressionError NOT_LITERAL = new DefaultExpressionError(){

            @Override
            public String getError(List<String> codeNames, String pattern) {
                StringBuilder builder = new StringBuilder();
                builder.append(DefaultExpressionError.defaultExpression(codeNames, " is not a literal. ", " are not literals. ")).append("Either allow null (with %-*").append(StringUtils.join(codeNames, "/")).append("%) or make it mandatory [pattern: ").append(pattern).append("]");
                return builder.toString();
            }
        };
        public static final /* enum */ DefaultExpressionError LITERAL = new DefaultExpressionError(){

            @Override
            public String getError(List<String> codeNames, String pattern) {
                StringBuilder builder = new StringBuilder();
                builder.append(DefaultExpressionError.defaultExpression(codeNames, " is a literal. ", " are literals. ")).append("Either allow null (with %-~").append(StringUtils.join(codeNames, "/")).append("%) or make it mandatory [pattern: ").append(pattern).append("]");
                return builder.toString();
            }
        };
        public static final /* enum */ DefaultExpressionError NOT_SINGLE = new DefaultExpressionError(){

            @Override
            public String getError(List<String> codeNames, String pattern) {
                StringBuilder builder = new StringBuilder();
                builder.append(DefaultExpressionError.defaultExpression(codeNames, " is not a single-element expression. ", " are not single-element expressions. ")).append("Change your pattern to allow multiple elements or make the expression mandatory [pattern: ").append(pattern).append("]");
                return builder.toString();
            }
        };
        public static final /* enum */ DefaultExpressionError TIME_STATE = new DefaultExpressionError(){

            @Override
            public String getError(List<String> codeNames, String pattern) {
                StringBuilder builder = new StringBuilder(DefaultExpressionError.defaultExpression(codeNames, " does ", " do "));
                builder.append("not have distinct time states. [pattern: ").append(pattern).append("]");
                return builder.toString();
            }
        };
        private static final /* synthetic */ DefaultExpressionError[] $VALUES;

        public static DefaultExpressionError[] values() {
            return (DefaultExpressionError[])$VALUES.clone();
        }

        public static DefaultExpressionError valueOf(String name) {
            return Enum.valueOf(DefaultExpressionError.class, name);
        }

        public abstract String getError(List<String> var1, String var2);

        private static String defaultExpression(List<String> codeNames, String single, String plural) {
            StringBuilder builder = new StringBuilder();
            String combinedComma = DefaultExpressionError.getCombinedComma(codeNames);
            builder.append("The default ").append(DefaultExpressionError.plurality(codeNames, "expression ", "expressions ")).append("of '").append(combinedComma).append("'").append(DefaultExpressionError.plurality(codeNames, single, plural));
            return builder.toString();
        }

        private static String plurality(List<String> codeNames, String single, String plural) {
            return codeNames.size() > 1 ? plural : single;
        }

        private static String getCombinedComma(List<String> codeNames) {
            assert (!codeNames.isEmpty());
            if (codeNames.size() == 1) {
                return codeNames.get(0);
            }
            if (codeNames.size() == 2) {
                return StringUtils.join(codeNames, " and ");
            }
            return StringUtils.join(codeNames, ", ", ", and ");
        }

        private static /* synthetic */ DefaultExpressionError[] $values() {
            return new DefaultExpressionError[]{NOT_FOUND, NOT_LITERAL, LITERAL, NOT_SINGLE, TIME_STATE};
        }

        static {
            $VALUES = DefaultExpressionError.$values();
        }
    }
}

