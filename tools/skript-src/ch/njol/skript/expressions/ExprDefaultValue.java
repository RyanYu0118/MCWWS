/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.bukkit.event.Event;

@Name(value="Default Value")
@Description(value={"A shorthand expression for giving things a default value. If the first thing isn't set, the second thing will be returned."})
@Example(value="broadcast {score::%player's uuid%} otherwise \"%player% has no score!\"")
@Since(value={"2.2-dev36"})
public class ExprDefaultValue
extends SimpleExpression<Object> {
    private Class<?>[] types;
    private Class<?> superType;
    private Expression<Object> values;
    private Expression<Object> defaultValues;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.values = LiteralUtils.defendExpression(exprs[0]);
        this.defaultValues = LiteralUtils.defendExpression(exprs[1]);
        if (!LiteralUtils.canInitSafely(this.values, this.defaultValues)) {
            return false;
        }
        HashSet types = new HashSet();
        Collections.addAll(types, this.values.possibleReturnTypes());
        Collections.addAll(types, this.defaultValues.possibleReturnTypes());
        this.types = types.toArray(new Class[0]);
        this.superType = Utils.getSuperType(this.types);
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        Object[] values = this.values.getArray(event);
        if (values.length != 0) {
            return values;
        }
        return this.defaultValues.getArray(event);
    }

    @Override
    public boolean isSingle() {
        return this.values.isSingle() && this.defaultValues.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        return this.superType;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return Arrays.copyOf(this.types, this.types.length);
    }

    @Override
    public Expression<?> simplify() {
        Expression<Object> expression = this.values;
        if (expression instanceof Literal) {
            Literal literal = (Literal)expression;
            if (this.defaultValues instanceof Literal || literal.getAll().length > 0) {
                return SimplifiedLiteral.fromExpression(this);
            }
        }
        return this;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return this.values.toString(event, debug) + " or else " + this.defaultValues.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprDefaultValue.class, Object.class, ExpressionType.COMBINED, "%objects% (otherwise|?) %objects%");
    }
}

