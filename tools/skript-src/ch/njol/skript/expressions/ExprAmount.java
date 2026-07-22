/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.common.AnyAmount;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Amount")
@Description(value={"The amount or size of something.", "Please note that <code>amount of %items%</code> will not return the number of items, but the number of stacks, e.g. 1 for a stack of 64 torches. To get the amount of items in a stack, see the <a href='#ExprItemAmount'>item amount</a> expression."})
@Example(value="message \"There are %number of all players% players online!\"")
@Since(value={"1.0"})
@Deprecated(since="2.13", forRemoval=true)
public class ExprAmount
extends SimpleExpression<Number> {
    private ExpressionList<?> exprs;
    @Nullable
    private Expression<AnyAmount> any;
    @Nullable
    private Variable<?> list;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ExpressionList<Object> expressionList;
        if (matchedPattern == 0) {
            this.any = exprs[0];
            return true;
        }
        Expression<?> expression = exprs[0];
        if (expression instanceof ExpressionList) {
            ExpressionList exprList = (ExpressionList)expression;
            expressionList = exprList;
        } else {
            expressionList = new ExpressionList<Object>(new Expression[]{exprs[0]}, Object.class, false);
        }
        this.exprs = expressionList;
        this.exprs = (ExpressionList)LiteralUtils.defendExpression(this.exprs);
        if (!LiteralUtils.canInitSafely(this.exprs)) {
            return false;
        }
        if (this.exprs.isSingle()) {
            Skript.error("'" + this.exprs.toString(null, Skript.debug()) + "' can only ever have one value at most, thus the 'amount of ...' expression is useless. Use '... exists' instead to find out whether the expression has a value.");
            return false;
        }
        expression = exprs[0];
        if (expression instanceof Variable) {
            Variable variable;
            this.list = variable = (Variable)expression;
        }
        return true;
    }

    protected Number[] get(Event event) {
        if (this.any != null) {
            return new Number[]{this.any.getOptionalSingle(event).orElse(() -> 0).amount()};
        }
        if (this.list != null) {
            return new Long[]{this.list.size(event)};
        }
        return new Long[]{this.exprs.getArray(event).length};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.any != null) {
            return switch (mode) {
                case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Number.class);
                default -> null;
            };
        }
        return super.acceptChange(mode);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (this.any == null) {
            super.change(event, delta, mode);
            return;
        }
        double amount = delta != null ? ((Number)delta[0]).doubleValue() : 1.0;
        switch (mode) {
            case REMOVE: {
                amount = -amount;
            }
            case ADD: {
                for (AnyAmount obj : this.any.getArray(event)) {
                    if (!obj.supportsAmountChange()) continue;
                    obj.setAmount(obj.amount().doubleValue() + amount);
                }
                break;
            }
            case SET: 
            case RESET: 
            case DELETE: {
                for (AnyAmount any : this.any.getArray(event)) {
                    if (!any.supportsAmountChange()) continue;
                    any.setAmount(amount);
                }
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return this.any != null ? Number.class : Long.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.any != null) {
            return "amount of " + this.any.toString(event, debug);
        }
        return "amount of " + this.exprs.toString(event, debug);
    }

    static {
        if (!SkriptConfig.useTypeProperties.value().booleanValue()) {
            Skript.registerExpression(ExprAmount.class, Number.class, ExpressionType.PROPERTY, "[the] (amount|number|size) of %numbered%", "[the] (amount|number|size) of %objects%");
        }
    }
}

