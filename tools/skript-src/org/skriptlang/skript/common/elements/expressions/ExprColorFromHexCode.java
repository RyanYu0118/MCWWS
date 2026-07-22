/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
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
import ch.njol.skript.util.ColorRGB;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Color from Hex Code")
@Description(value={"Returns a proper argb color from a hex code string. The hex code must contain RRGGBB values, but can also contain a leading # or AARRGGBB format. Invalid codes will cause runtime errors."})
@Example.Examples(value={@Example(value="send color from hex code \"#FFBBA7\""), @Example(value="send color from hex code \"FFBBA7\""), @Example(value="send color from hex code \"#AAFFBBA7\"")})
@Since(value={"2.14"})
public class ExprColorFromHexCode
extends SimplePropertyExpression<String, Color> {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprColorFromHexCode.class, Color.class).supplier(ExprColorFromHexCode::new)).priority(DEFAULT_PRIORITY)).addPattern("[the] colo[u]r[s] (from|of) hex[adecimal] code[s] %strings%")).build());
    }

    @Override
    @Nullable
    public Color convert(String from) {
        ColorRGB color;
        if (from.startsWith("#")) {
            from = from.substring(1);
        }
        if ((color = ColorRGB.fromHexString(from)) == null) {
            this.error("Could not parse '" + from + "' as a hex code!");
        }
        return color;
    }

    @Override
    public Class<? extends Color> getReturnType() {
        return Color.class;
    }

    @Override
    protected String getPropertyName() {
        return "ExprColorFromHexCode - UNUSED";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the color of hex code " + this.getExpr().toString(event, debug);
    }

    @Override
    public Expression<? extends Color> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }
}

