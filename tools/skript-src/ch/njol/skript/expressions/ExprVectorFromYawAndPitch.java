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
import ch.njol.skript.expressions.ExprYawPitch;
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

@Name(value="Vectors - Vector from Yaw and Pitch")
@Description(value={"Creates a vector from a yaw and pitch value."})
@Example(value="set {_v} to vector from yaw 45 and pitch 45")
@Since(value={"2.2-dev28"})
public class ExprVectorFromYawAndPitch
extends SimpleExpression<Vector> {
    private Expression<Number> pitch;
    private Expression<Number> yaw;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.pitch = exprs[matchedPattern ^ 1];
        this.yaw = exprs[matchedPattern];
        return true;
    }

    protected Vector[] get(Event event) {
        Number skriptYaw = this.yaw.getSingle(event);
        Number skriptPitch = this.pitch.getSingle(event);
        if (skriptYaw == null || skriptPitch == null) {
            return null;
        }
        float yaw = ExprYawPitch.fromSkriptYaw(ExprVectorFromYawAndPitch.wrapAngleDeg(skriptYaw.floatValue()));
        float pitch = ExprYawPitch.fromSkriptPitch(ExprVectorFromYawAndPitch.wrapAngleDeg(skriptPitch.floatValue()));
        return CollectionUtils.array(ExprYawPitch.fromYawAndPitch(yaw, pitch));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Vector> getReturnType() {
        return Vector.class;
    }

    @Override
    public Expression<? extends Vector> simplify() {
        if (this.pitch instanceof Literal && this.yaw instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "vector from yaw " + this.yaw.toString(event, debug) + " and pitch " + this.pitch.toString(event, debug);
    }

    public static float wrapAngleDeg(float angle) {
        if ((angle %= 360.0f) <= -180.0f) {
            return angle + 360.0f;
        }
        if (angle > 180.0f) {
            return angle - 360.0f;
        }
        return angle;
    }

    static {
        Skript.registerExpression(ExprVectorFromYawAndPitch.class, Vector.class, ExpressionType.COMBINED, "[a] [new] vector (from|with) yaw %number% and pitch %number%", "[a] [new] vector (from|with) pitch %number% and yaw %number%");
    }
}

