/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.skriptlang.skript.lang.script.Script
 */
package org.skriptlang.reflect.syntax.expression;

import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import java.util.Objects;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.skript.lang.script.Script;

public class ConstantSyntaxInfo
extends CustomSyntaxStructure.SyntaxData {
    private ConstantSyntaxInfo(Script script, String pattern, int matchedPattern) {
        super(script, pattern, matchedPattern);
    }

    public static ConstantSyntaxInfo create(Script script, String pattern, int matchedPattern) {
        return new ConstantSyntaxInfo(script, SkriptMirrorUtil.preprocessPattern(pattern), matchedPattern);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ConstantSyntaxInfo that = (ConstantSyntaxInfo)o;
        return Objects.equals(this.getScript(), that.getScript()) && Objects.equals(this.getPattern(), that.getPattern());
    }

    public int hashCode() {
        return Objects.hash(this.getScript(), this.getPattern());
    }
}

