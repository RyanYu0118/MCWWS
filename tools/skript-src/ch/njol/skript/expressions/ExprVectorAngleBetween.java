/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
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
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Vectors - Angle Between")
@Description(value={"Gets the angle between two vectors."})
@Example(value="send \"%the angle between vector 1, 0, 0 and vector 0, 1, 1%\"")
@Since(value={"2.2-dev28"})
public class ExprVectorAngleBetween
extends SimpleExpression<Number> {
    private static final float RAD_TO_DEG = 57.29578f;
    private Expression<Vector> first;
    private Expression<Vector> second;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.first = exprs[0];
        this.second = exprs[1];
        return true;
    }

    protected Number[] get(Event event) {
        Vector first = this.first.getSingle(event);
        Vector second = this.second.getSingle(event);
        if (first == null || second == null) {
            return null;
        }
        return CollectionUtils.array(Float.valueOf(first.angle(second) * 57.29578f));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public Expression<? extends Number> simplify() {
        if (this.first instanceof Literal && this.second instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the angle between " + this.first.toString(event, debug) + " and " + this.second.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprVectorAngleBetween.class, Number.class, ExpressionType.COMBINED, "[the] angle between [[the] vectors] %vector% and %vector%");
    }
}

