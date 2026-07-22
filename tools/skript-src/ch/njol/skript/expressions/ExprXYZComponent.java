/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 *  org.joml.Quaternionf
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Locale;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Quaternionf;

@Name(value="Vector/Quaternion - WXYZ Component")
@Description(value={"Gets or changes the W, X, Y or Z component of <a href='#vector'>vectors</a>/<a href='#quaternion'>quaternions</a>.", "You cannot use the W component with vectors; it is for quaternions only."})
@Example(value="set {_v} to vector 1, 2, 3\nsend \"%x of {_v}%, %y of {_v}%, %z of {_v}%\"\nadd 1 to x of {_v}\nadd 2 to y of {_v}\nadd 3 to z of {_v}\nsend \"%x of {_v}%, %y of {_v}%, %z of {_v}%\"\nset x component of {_v} to 1\nset y component of {_v} to 2\nset z component of {_v} to 3\nsend \"%x component of {_v}%, %y component of {_v}%, %z component of {_v}%\"\n")
@Since(value={"2.2-dev28, 2.10 (quaternions)"})
public class ExprXYZComponent
extends SimplePropertyExpression<Object, Number> {
    private static final boolean IS_RUNNING_1194 = Skript.isRunningMinecraft(1, 19, 4);
    private @UnknownNullability Axis axis;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.axis = Axis.valueOf(parseResult.tags.get(0).toUpperCase(Locale.ENGLISH));
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Number convert(Object object) {
        if (object instanceof Vector) {
            Vector vector = (Vector)object;
            return switch (this.axis.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> null;
                case 1 -> vector.getX();
                case 2 -> vector.getY();
                case 3 -> vector.getZ();
            };
        }
        if (object instanceof Quaternionf) {
            Quaternionf quaternion = (Quaternionf)object;
            return switch (this.axis.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> Float.valueOf(quaternion.w());
                case 1 -> Float.valueOf(quaternion.x());
                case 2 -> Float.valueOf(quaternion.y());
                case 3 -> Float.valueOf(quaternion.z());
            };
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        boolean acceptsChange;
        if ((mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.SET) && (acceptsChange = IS_RUNNING_1194 ? Changer.ChangerUtils.acceptsChange(this.getExpr(), Changer.ChangeMode.SET, Vector.class, Quaternionf.class) : Changer.ChangerUtils.acceptsChange(this.getExpr(), Changer.ChangeMode.SET, Vector.class))) {
            return CollectionUtils.array(Number.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        double value = ((Number)delta[0]).doubleValue();
        boolean acceptsVectors = Changer.ChangerUtils.acceptsChange(this.getExpr(), Changer.ChangeMode.SET, Vector.class);
        boolean acceptsQuaternions = Changer.ChangerUtils.acceptsChange(this.getExpr(), Changer.ChangeMode.SET, Quaternionf.class);
        this.getExpr().changeInPlace(event, object -> {
            if (acceptsVectors && object instanceof Vector) {
                Vector vector = (Vector)object;
                ExprXYZComponent.changeVector(vector, this.axis, value, mode);
            } else if (acceptsQuaternions && object instanceof Quaternionf) {
                Quaternionf quaternion = (Quaternionf)object;
                ExprXYZComponent.changeQuaternion(quaternion, this.axis, (float)value, mode);
            }
            return object;
        });
    }

    private static void changeVector(Vector vector, Axis axis, double value, Changer.ChangeMode mode) {
        if (axis == Axis.W) {
            return;
        }
        switch (mode) {
            case REMOVE: {
                value = -value;
            }
            case ADD: {
                switch (axis.ordinal()) {
                    case 1: {
                        vector.setX(vector.getX() + value);
                        break;
                    }
                    case 2: {
                        vector.setY(vector.getY() + value);
                        break;
                    }
                    case 3: {
                        vector.setZ(vector.getZ() + value);
                    }
                }
                break;
            }
            case SET: {
                switch (axis.ordinal()) {
                    case 1: {
                        vector.setX(value);
                        break;
                    }
                    case 2: {
                        vector.setY(value);
                        break;
                    }
                    case 3: {
                        vector.setZ(value);
                    }
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    private static void changeQuaternion(Quaternionf quaternion, Axis axis, float value, Changer.ChangeMode mode) {
        float x = quaternion.x();
        float y = quaternion.y();
        float z = quaternion.z();
        float w = quaternion.w();
        block0 : switch (mode) {
            case REMOVE: {
                value = -value;
            }
            case ADD: {
                switch (axis.ordinal()) {
                    case 0: {
                        w += value;
                        break;
                    }
                    case 1: {
                        x += value;
                        break;
                    }
                    case 2: {
                        y += value;
                        break;
                    }
                    case 3: {
                        z += value;
                    }
                }
                break;
            }
            case SET: {
                switch (axis.ordinal()) {
                    case 0: {
                        w = value;
                        break block0;
                    }
                    case 1: {
                        x = value;
                        break block0;
                    }
                    case 2: {
                        y = value;
                        break block0;
                    }
                    case 3: {
                        z = value;
                    }
                }
            }
        }
        quaternion.set(x, y, z, w);
    }

    @Override
    public Class<Number> getReturnType() {
        return Number.class;
    }

    @Override
    public Expression<? extends Number> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return this.axis.name().toLowerCase(Locale.ENGLISH) + " component";
    }

    static {
        Object types = "vectors";
        if (IS_RUNNING_1194) {
            types = (String)types + "/quaternions";
        }
        if (!SkriptConfig.useTypeProperties.value().booleanValue()) {
            ExprXYZComponent.register(ExprXYZComponent.class, Number.class, "[vector|quaternion] (:w|:x|:y|:z) [component[s]]", (String)types);
        }
    }

    private static enum Axis {
        W,
        X,
        Y,
        Z;

    }
}

