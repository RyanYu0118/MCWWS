/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.PropertyExpression;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.util.Priority;

@Deprecated(since="2.14", forRemoval=true)
public enum ExpressionType {
    SIMPLE(SyntaxInfo.SIMPLE),
    EVENT(EventValueExpression.DEFAULT_PRIORITY),
    COMBINED(SyntaxInfo.COMBINED),
    PROPERTY(PropertyExpression.DEFAULT_PRIORITY),
    PATTERN_MATCHES_EVERYTHING(SyntaxInfo.PATTERN_MATCHES_EVERYTHING);

    private final Priority priority;

    private ExpressionType(Priority priority) {
        this.priority = priority;
    }

    public Priority priority() {
        return this.priority;
    }

    @Nullable
    public static ExpressionType fromModern(Priority priority) {
        if (priority == SyntaxInfo.SIMPLE) {
            return SIMPLE;
        }
        if (priority == EventValueExpression.DEFAULT_PRIORITY) {
            return EVENT;
        }
        if (priority == SyntaxInfo.COMBINED) {
            return COMBINED;
        }
        if (priority == PropertyExpression.DEFAULT_PRIORITY) {
            return PROPERTY;
        }
        if (priority == SyntaxInfo.PATTERN_MATCHES_EVERYTHING) {
            return PATTERN_MATCHES_EVERYTHING;
        }
        return null;
    }
}

