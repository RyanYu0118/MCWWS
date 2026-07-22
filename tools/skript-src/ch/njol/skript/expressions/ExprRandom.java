/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Random")
@Description(value={"Gets a random item out of a set, e.g. a random player out of all players online."})
@Example.Examples(value={@Example(value="give a diamond to a random player out of all players"), @Example(value="give a random item out of all items to the player")})
@Since(value={"1.4.9"})
public class ExprRandom
extends SimpleExpression<Object> {
    private Expression<?> expr;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (LiteralUtils.hasUnparsedLiteral(exprs[1])) {
            this.expr = LiteralUtils.defendExpression(exprs[1]);
            if (this.expr instanceof ExpressionList) {
                Class type = ((ClassInfo)((Literal)exprs[0]).getSingle()).getC();
                List list = Arrays.stream(((ExpressionList)this.expr).getExpressions()).map(expression -> expression.getConvertedExpression(type)).filter(Objects::nonNull).collect(Collectors.toList());
                if (list.isEmpty()) {
                    Skript.error("There are no objects of type '" + exprs[0].toString() + "' in the list " + exprs[1].toString());
                    return false;
                }
                this.expr = (Expression)CollectionUtils.getRandom(list);
            }
        } else {
            this.expr = exprs[1].getConvertedExpression(((ClassInfo)((Literal)exprs[0]).getSingle()).getC());
        }
        return this.expr != null && LiteralUtils.canInitSafely(this.expr);
    }

    @Override
    protected Object[] get(Event event) {
        Object[] set = this.expr.getAll(event);
        if (set.length <= 1) {
            return set;
        }
        Object[] one = (Object[])Array.newInstance(set.getClass().getComponentType(), 1);
        one[0] = CollectionUtils.getRandom(set);
        return one;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Object> getReturnType() {
        return this.expr.getReturnType();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a random element out of " + this.expr.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprRandom.class, Object.class, ExpressionType.COMBINED, "[a] random %*classinfo% [out] of %objects%");
    }
}

