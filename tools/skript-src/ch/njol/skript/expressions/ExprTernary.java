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
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.bukkit.event.Event;

@Name(value="Ternary")
@Description(value={"A shorthand expression for returning something based on a condition."})
@Example(value="set {points} to 500 if {admin::%player's uuid%} is set else 100")
@Since(value={"2.2-dev36"})
public class ExprTernary
extends SimpleExpression<Object> {
    private Class<?>[] types;
    private Class<?> superType;
    private Expression<Object> ifTrue;
    private Condition condition;
    private Expression<Object> ifFalse;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.ifTrue = LiteralUtils.defendExpression(exprs[0]);
        this.ifFalse = LiteralUtils.defendExpression(exprs[1]);
        if (this.ifFalse instanceof ExprTernary || this.ifTrue instanceof ExprTernary) {
            Skript.error("Ternary operators may not be nested!");
            return false;
        }
        if (!LiteralUtils.canInitSafely(this.ifTrue, this.ifFalse)) {
            return false;
        }
        String cond = parseResult.regexes.get(0).group();
        this.condition = Condition.parse(cond, "Can't understand this condition: " + cond);
        if (this.condition == null) {
            return false;
        }
        HashSet types = new HashSet();
        Collections.addAll(types, this.ifTrue.possibleReturnTypes());
        Collections.addAll(types, this.ifFalse.possibleReturnTypes());
        this.types = types.toArray(new Class[0]);
        this.superType = Utils.getSuperType(this.types);
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        return this.condition.check(event) ? this.ifTrue.getArray(event) : this.ifFalse.getArray(event);
    }

    @Override
    public boolean isSingle() {
        return this.ifTrue.isSingle() && this.ifFalse.isSingle();
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
    public String toString(Event event, boolean debug) {
        return this.ifTrue.toString(event, debug) + " if " + this.condition.toString(event, debug) + " otherwise " + this.ifFalse.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprTernary.class, Object.class, ExpressionType.COMBINED, "%objects% if <.+>[,] (otherwise|else) %objects%");
    }
}

