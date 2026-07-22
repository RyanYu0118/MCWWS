/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.properties.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RelatedProperty;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyBaseExpression;
import org.skriptlang.skript.lang.properties.PropertyBaseSyntax;
import org.skriptlang.skript.lang.properties.handlers.TypedValueHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Value")
@Description(value={"Returns the value of something that has a value, e.g. a node in a config.", "The value is automatically converted to the specified type (e.g. text, number) where possible."})
@Example(value="set {_node} to node \"update check interval\" in the skript config\n\nbroadcast text value of {_node}\n# text value of {_node} = \"12 hours\" (text)\n\nwait for {_node}'s timespan value\n# timespan value of {_node} = 12 hours (duration)\n")
@Since(value={"2.10"})
@RelatedProperty(value="typed value")
public class PropExprValueOf
extends PropertyBaseExpression<TypedValueHandler<?, ?>> {
    private ClassInfo<?> type;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)PropertyExpression.infoBuilder(PropExprValueOf.class, Object.class, "[%-*classinfo%] value", "objects", false).supplier(PropExprValueOf::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<?> propertyExpr;
        if (matchedPattern == 0) {
            this.type = expressions[0] == null ? null : (ClassInfo)((Literal)expressions[0]).getSingle();
            propertyExpr = expressions[1];
        } else {
            this.type = expressions[1] == null ? null : (ClassInfo)((Literal)expressions[1]).getSingle();
            propertyExpr = expressions[0];
        }
        this.expr = PropertyBaseSyntax.asProperty(this.property, propertyExpr);
        if (this.expr == null) {
            Skript.error(this.getBadTypesErrorMessage(propertyExpr));
            return false;
        }
        this.properties = PropertyBaseSyntax.getPossiblePropertyInfos(this.property, this.expr);
        if (this.properties.isEmpty()) {
            Skript.error(this.getBadTypesErrorMessage(this.expr));
            return false;
        }
        if (this.type == null) {
            this.returnTypes = this.getPropertyReturnTypes(this.properties, ExpressionPropertyHandler::possibleReturnTypes);
            this.returnType = Utils.getSuperType(this.returnTypes);
        } else {
            this.returnTypes = new Class[]{this.type.getC()};
            this.returnType = this.type.getC();
        }
        return LiteralUtils.canInitSafely(this.expr);
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (this.type == null) {
            return super.get(event);
        }
        return this.expr.stream(event).flatMap(source -> {
            TypedValueHandler handler = (TypedValueHandler)this.properties.getHandler(source.getClass());
            if (handler == null) {
                return null;
            }
            Object value = handler.convert(source, this.type);
            if (value != null && value.getClass().isArray()) {
                return Arrays.stream((Object[])value);
            }
            return Stream.of(value);
        }).filter(Objects::nonNull).toArray(size -> (Object[])Array.newInstance(this.getReturnType(), size));
    }

    @Override
    @NotNull
    public Property<TypedValueHandler<?, ?>> getProperty() {
        return Property.TYPED_VALUE;
    }
}

