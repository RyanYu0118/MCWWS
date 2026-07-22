/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.util.Color;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Hex Code")
@Description(value={"Returns the hexadecimal value representing the given color(s).\nThe hex value of a colour does not contain a leading #, just the RRGGBB value.\nFor those looking for hex values of numbers, see the asBase and fromBase functions.\n"})
@Example(value="send formatted \"<#%hex code of rgb(100, 10, 10)%>darker red\" to all players")
@Since(value={"2.14"})
public class ExprHexCode
extends SimplePropertyExpression<Color, String> {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprHexCode.infoBuilder(ExprHexCode.class, String.class, "hex[adecimal] code", "colors", false).supplier(ExprHexCode::new)).build());
    }

    @Override
    @Nullable
    public String convert(Color color) {
        return color.toHexString();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "hexadecimal code";
    }

    @Override
    public Expression<? extends String> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }
}

