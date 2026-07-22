/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package com.btk5h.skriptmirror.util;

import ch.njol.skript.lang.Expression;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.util.JavaUtil;
import java.util.List;
import java.util.regex.MatchResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class JavaTypeWrapper {
    public static final String PRIMITIVE_PATTERNS = String.join((CharSequence)"|", JavaUtil.PRIMITIVE_CLASS_NAMES.keySet());
    private final Expression<? extends JavaType> javaTypeExpression;
    private final Class<?> clazz;

    public JavaTypeWrapper(@Nullable Expression<? extends JavaType> javaTypeExpression, @Nullable Class<?> clazz) {
        this.javaTypeExpression = javaTypeExpression;
        this.clazz = clazz;
    }

    public static JavaTypeWrapper ofExpression(Expression<? extends JavaType> javaTypeExpression) {
        return new JavaTypeWrapper(javaTypeExpression, null);
    }

    public static JavaTypeWrapper ofClass(Class<?> clazz) {
        return new JavaTypeWrapper(null, clazz);
    }

    public static JavaTypeWrapper of(Expression<?> expression, List<MatchResult> matchResults) {
        if (expression != null) {
            return JavaTypeWrapper.ofExpression(expression);
        }
        String primitiveType = matchResults.get(0).group();
        return JavaTypeWrapper.ofClass(JavaUtil.PRIMITIVE_CLASS_NAMES.get(primitiveType));
    }

    public JavaType get(Event event) {
        if (this.clazz != null) {
            return new JavaType(this.clazz);
        }
        return (JavaType)this.javaTypeExpression.getSingle(event);
    }

    public String toString(@Nullable Event event, boolean debug) {
        if (this.clazz != null) {
            return this.clazz.getName();
        }
        return this.javaTypeExpression.toString(event, debug);
    }

    public String toString() {
        if (this.clazz != null) {
            return this.clazz.getName();
        }
        return this.javaTypeExpression.toString(null, false);
    }
}

