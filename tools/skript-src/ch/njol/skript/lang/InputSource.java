/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.lang;

import ch.njol.skript.expressions.ExprInput;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.LiteralUtils;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public interface InputSource {
    public Set<ExprInput<?>> getDependentInputs();

    @Nullable
    public Object getCurrentValue();

    default public boolean hasIndices() {
        return false;
    }

    default public @UnknownNullability String getCurrentIndex() {
        return null;
    }

    @Nullable
    default public Expression<?> parseExpression(String expr, ParserInstance parser, int flags) {
        InputData inputData = parser.getData(InputData.class);
        InputSource originalSource = inputData.getSource();
        inputData.setSource(this);
        Expression mappingExpr = new SkriptParser(expr, flags, ParseContext.DEFAULT).parseExpression(Object.class);
        if (mappingExpr != null && LiteralUtils.hasUnparsedLiteral(mappingExpr) && !LiteralUtils.canInitSafely(mappingExpr = LiteralUtils.defendExpression(mappingExpr))) {
            return null;
        }
        inputData.setSource(originalSource);
        return mappingExpr;
    }

    public static class InputData
    extends ParserInstance.Data {
        @Nullable
        private InputSource source;

        public InputData(ParserInstance parserInstance) {
            super(parserInstance);
        }

        public void setSource(@Nullable InputSource source) {
            this.source = source;
        }

        @Nullable
        public InputSource getSource() {
            return this.source;
        }
    }
}

