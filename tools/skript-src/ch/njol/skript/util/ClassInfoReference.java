/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClassInfoReference {
    private Kleenean plural;
    private ClassInfo<?> classInfo;

    @Nullable
    private static UnparsedLiteral getSourceUnparsedLiteral(Expression<?> expression) {
        while (!(expression instanceof UnparsedLiteral)) {
            Expression<?> nextEarliestExpression = expression.getSource();
            if (nextEarliestExpression == expression) {
                return null;
            }
            expression = nextEarliestExpression;
        }
        return (UnparsedLiteral)expression;
    }

    private static Kleenean determineIfPlural(Expression<ClassInfo<?>> classInfoExpression) {
        UnparsedLiteral sourceUnparsedLiteral = ClassInfoReference.getSourceUnparsedLiteral(classInfoExpression);
        if (sourceUnparsedLiteral == null) {
            return Kleenean.UNKNOWN;
        }
        String originalExpr = sourceUnparsedLiteral.getData();
        boolean isPlural = Utils.getEnglishPlural(originalExpr).getSecond();
        return Kleenean.get(isPlural);
    }

    @NotNull
    public static Expression<ClassInfoReference> wrap(final @NotNull Expression<ClassInfo<?>> classInfoExpression) {
        if (classInfoExpression instanceof ExpressionList) {
            ExpressionList classInfoExpressionList = (ExpressionList)classInfoExpression;
            Expression[] wrappedExpressions = (Expression[])Arrays.stream(classInfoExpressionList.getExpressions()).map(expression -> ClassInfoReference.wrap(expression)).toArray(Expression[]::new);
            return new ExpressionList<ClassInfoReference>(wrappedExpressions, ClassInfoReference.class, classInfoExpression.getAnd());
        }
        final Kleenean isPlural = ClassInfoReference.determineIfPlural(classInfoExpression);
        if (classInfoExpression instanceof Literal) {
            Literal classInfoLiteral = (Literal)classInfoExpression;
            ClassInfo classInfo = (ClassInfo)classInfoLiteral.getSingle();
            return new SimpleLiteral<ClassInfoReference>(new ClassInfoReference(classInfo, isPlural), classInfoLiteral.isDefault());
        }
        return new SimpleExpression<ClassInfoReference>(){

            @Nullable
            protected ClassInfoReference[] get(Event event) {
                if (classInfoExpression.isSingle()) {
                    ClassInfo classInfo = (ClassInfo)classInfoExpression.getSingle(event);
                    if (classInfo == null) {
                        return new ClassInfoReference[0];
                    }
                    return new ClassInfoReference[]{new ClassInfoReference(classInfo, isPlural)};
                }
                return (ClassInfoReference[])classInfoExpression.stream(event).map(ClassInfoReference::new).toArray(ClassInfoReference[]::new);
            }

            @Override
            public boolean isSingle() {
                return classInfoExpression.isSingle();
            }

            @Override
            public Class<? extends ClassInfoReference> getReturnType() {
                return ClassInfoReference.class;
            }

            @Override
            public String toString(@Nullable Event event, boolean debug) {
                if (debug) {
                    return classInfoExpression.toString(event, true) + "(wrapped by " + this.getClass().getSimpleName() + ")";
                }
                return classInfoExpression.toString(event, false);
            }

            @Override
            public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
                return classInfoExpression.init(expressions, matchedPattern, isDelayed, parseResult);
            }
        };
    }

    public ClassInfoReference(ClassInfo<?> classInfo) {
        this(classInfo, Kleenean.UNKNOWN);
    }

    public ClassInfoReference(ClassInfo<?> classInfo, Kleenean plural) {
        this.classInfo = classInfo;
        this.plural = plural;
    }

    public Kleenean isPlural() {
        return this.plural;
    }

    public void setPlural(Kleenean plural) {
        this.plural = plural;
    }

    public ClassInfo<?> getClassInfo() {
        return this.classInfo;
    }

    public void setClassInfo(ClassInfo<?> classInfo) {
        this.classInfo = classInfo;
    }
}

