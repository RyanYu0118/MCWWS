/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import org.jetbrains.annotations.Nullable;

@Name(value="Character Codepoint")
@Description(value={"Returns the Unicode codepoint of a character"})
@Example(value="function is_in_order(letters: strings) :: boolean:\n\tloop {_letters::*}:\n\t\tset {_codepoint} to codepoint of lowercase loop-value\n\n\t\treturn false if {_codepoint} is not set # 'loop-value is not a single character'\n\n\t\tif:\n\t\t\t{_previous-codepoint} is set\n\t\t\t# if the codepoint of the current character is not\n\t\t\t#  1 more than the codepoint of the previous character\n\t\t\t#  then the letters are not in order\n\t\t\t{_codepoint} - {_previous-codepoint} is not 1\n\t\tthen:\n\t\t\treturn false\n\n\t\tset {_previous-codepoint} to {_codepoint}\n\treturn true\n")
@Since(value={"2.9.0"})
public class ExprCodepoint
extends SimplePropertyExpression<String, Integer> {
    @Override
    @Nullable
    public Integer convert(String string) {
        if (string.isEmpty()) {
            return null;
        }
        return string.codePointAt(0);
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public Expression<? extends Integer> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return "codepoint";
    }

    static {
        ExprCodepoint.register(ExprCodepoint.class, Integer.class, "[unicode|character] code([ ]point| position)", "strings");
    }
}

