/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.properties.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RelatedProperty;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyBaseExpression;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Amount")
@Description(value={"The amount of something.\nUsing 'amount of {list::*}' will return the length of the list, so if you want the amounts of the things inside the lists, use 'amounts of {list::*}'.\n"})
@Example.Examples(value={@Example(value="message \"There are %amount of all players% players online!\""), @Example(value="if amount of player's tool > 5:"), @Example(value="if amounts of player's tool and player's offhand tool > 5:")})
@Since(value={"1.0", "2.13 (amounts of)"})
@RelatedProperty(value="amount")
public class PropExprAmount
extends PropertyBaseExpression<ExpressionPropertyHandler<?, ?>> {
    private ExpressionList<?> exprs;
    @Nullable
    private Variable<?> list;
    private boolean useProperties;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)PropertyExpression.infoBuilder(PropExprAmount.class, Object.class, "amount[:s]", "objects", false).supplier(PropExprAmount::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean bl = this.useProperties = parseResult.hasTag("s") || expressions[0].isSingle();
        if (this.useProperties) {
            return super.init(expressions, matchedPattern, isDelayed, parseResult);
        }
        this.exprs = PropExprAmount.asExprList(expressions[0]);
        Expression<?> expression = expressions[0];
        if (expression instanceof Variable) {
            Variable variable;
            this.list = variable = (Variable)expression;
        }
        return LiteralUtils.canInitSafely(this.exprs);
    }

    @ApiStatus.Internal
    public static ExpressionList<?> asExprList(Expression<?> expr) {
        ExpressionList<Object> exprList;
        ExpressionList<Object> exprs = expr instanceof ExpressionList ? (exprList = (ExpressionList<Object>)expr) : new ExpressionList<Object>(new Expression[]{expr}, Object.class, false);
        return (ExpressionList)LiteralUtils.defendExpression(exprs);
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (this.useProperties) {
            return super.get(event);
        }
        if (this.list != null) {
            return new Long[]{this.list.size(event)};
        }
        return new Long[]{this.exprs.getArray(event).length};
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.useProperties) {
            return super.acceptChange(mode);
        }
        return null;
    }

    @Override
    @NotNull
    public Property<ExpressionPropertyHandler<?, ?>> getProperty() {
        return Property.AMOUNT;
    }

    @Override
    public boolean isSingle() {
        if (this.useProperties) {
            return super.isSingle();
        }
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        if (this.useProperties) {
            return super.getReturnType();
        }
        return Long.class;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        if (this.useProperties) {
            return super.possibleReturnTypes();
        }
        return new Class[]{Long.class};
    }

    @Override
    public String toString(Event event, boolean debug) {
        if (this.useProperties) {
            return super.toString(event, debug);
        }
        return "amount of " + this.exprs.toString(event, debug);
    }
}

