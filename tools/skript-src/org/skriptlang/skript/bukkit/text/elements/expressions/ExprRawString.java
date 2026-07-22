/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.text.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Raw String")
@Description(value={"Returns the string without formatting (colors, decorations, etc.) and without stripping them from it.", "For example, <code>raw \"&aHello There!\"</code> would output <code>&aHello There!</code>"})
@Example(value="send raw \"&aThis text is unformatted!\" to all players")
@Since(value={"2.7"})
public class ExprRawString
extends SimplePropertyExpression<String, Object> {
    private boolean isComponent = true;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprRawString.class, Object.class).supplier(ExprRawString::new)).addPatterns("raw %strings%")).build());
    }

    @Override
    public Object convert(String from) {
        return this.isComponent ? Component.text((String)from) : from;
    }

    @Override
    public Class<?> getReturnType() {
        return this.isComponent ? Component.class : String.class;
    }

    @Override
    protected String getPropertyName() {
        return "raw";
    }

    @Override
    @SafeVarargs
    @Nullable
    public final <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        for (Class<R> clazz : to) {
            if (!String.class.isAssignableFrom(clazz)) continue;
            ExprRawString converted = new ExprRawString();
            converted.setExpr(this.getExpr());
            converted.rawExpr = this.rawExpr;
            converted.isComponent = false;
            return converted;
        }
        return super.getConvertedExpression(to);
    }
}

