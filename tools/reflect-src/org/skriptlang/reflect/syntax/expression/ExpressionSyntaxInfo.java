/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.skriptlang.skript.lang.script.Script
 */
package org.skriptlang.reflect.syntax.expression;

import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.skript.lang.script.Script;

public class ExpressionSyntaxInfo
extends CustomSyntaxStructure.SyntaxData {
    private final int[] inheritedSingles;
    private final boolean alwaysPlural;
    private final boolean adaptArgument;
    private final boolean property;

    private ExpressionSyntaxInfo(Script script, String pattern, int matchedPattern, int[] inheritedSingles, boolean alwaysPlural, boolean adaptArgument, boolean property) {
        super(script, pattern, matchedPattern);
        this.inheritedSingles = inheritedSingles;
        this.alwaysPlural = alwaysPlural;
        this.adaptArgument = adaptArgument;
        this.property = property;
    }

    public static ExpressionSyntaxInfo create(Script script, String pattern, int matchedPattern, boolean alwaysPlural, boolean adaptArgument, boolean property) {
        StringBuilder newPattern = new StringBuilder(pattern.length());
        ArrayList<Integer> inheritedSingles = new ArrayList<Integer>();
        String[] parts = pattern.split("%");
        for (int i2 = 0; i2 < parts.length; ++i2) {
            String part = parts[i2];
            if (i2 % 2 == 0) {
                newPattern.append(part);
                continue;
            }
            if (part.startsWith("$")) {
                part = part.substring(1);
                inheritedSingles.add(i2 / 2);
            }
            part = part.startsWith("_") ? (part.endsWith("s") ? "javaobject" : "javaobjects") : SkriptMirrorUtil.processTypes(part);
            newPattern.append('%');
            newPattern.append(part);
            newPattern.append('%');
        }
        return new ExpressionSyntaxInfo(script, newPattern.toString(), matchedPattern, inheritedSingles.stream().mapToInt(i -> i).toArray(), alwaysPlural, adaptArgument, property);
    }

    public int[] getInheritedSingles() {
        return this.inheritedSingles;
    }

    public boolean isAlwaysPlural() {
        return this.alwaysPlural;
    }

    public boolean shouldAdaptArgument() {
        return this.adaptArgument;
    }

    public boolean isProperty() {
        return this.property;
    }

    @Override
    public String toString() {
        return String.format("%s (singles: %s, plural: %s, adapt: %s, property: %s)", this.getPattern(), Arrays.toString(this.inheritedSingles), this.alwaysPlural, this.adaptArgument, this.property);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ExpressionSyntaxInfo that = (ExpressionSyntaxInfo)o;
        return this.alwaysPlural == that.alwaysPlural && this.adaptArgument == that.adaptArgument && this.property == that.property && Arrays.equals(this.inheritedSingles, that.inheritedSingles) && Objects.equals(this.getScript(), that.getScript()) && Objects.equals(this.getPattern(), that.getPattern());
    }

    public int hashCode() {
        int result = Objects.hash(this.alwaysPlural, this.adaptArgument, this.property, this.getScript(), this.getPattern());
        result = 31 * result + Arrays.hashCode(this.inheritedSingles);
        return result;
    }
}

