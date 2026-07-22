/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.skriptlang.skript.lang.script.Script
 */
package org.skriptlang.reflect.syntax.condition;

import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import java.util.Objects;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.skript.lang.script.Script;

public class ConditionSyntaxInfo
extends CustomSyntaxStructure.SyntaxData {
    private final boolean inverted;
    private final boolean property;

    private ConditionSyntaxInfo(Script script, String pattern, int matchedPattern, boolean inverted, boolean property) {
        super(script, pattern, matchedPattern);
        this.inverted = inverted;
        this.property = property;
    }

    public static ConditionSyntaxInfo create(Script script, String pattern, int matchedPattern, boolean inverted, boolean property) {
        return new ConditionSyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern), matchedPattern, inverted, property);
    }

    public boolean isInverted() {
        return this.inverted;
    }

    public boolean isProperty() {
        return this.property;
    }

    @Override
    public String toString() {
        return String.format("%s (inverted: %s, property: %s)", this.getPattern(), this.inverted, this.property);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ConditionSyntaxInfo that = (ConditionSyntaxInfo)o;
        return this.inverted == that.inverted && this.property == that.property && Objects.equals(this.getScript(), that.getScript()) && Objects.equals(this.getPattern(), that.getPattern());
    }

    public int hashCode() {
        return Objects.hash(this.inverted, this.property, this.getScript(), this.getPattern());
    }
}

