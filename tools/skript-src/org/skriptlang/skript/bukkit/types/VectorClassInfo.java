/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.types;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.WXYZHandler;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Internal
public class VectorClassInfo
extends ClassInfo<Vector> {
    public VectorClassInfo() {
        super(Vector.class, "vector");
        this.user("vectors?").name("Vector").description("Vector is a collection of numbers. In Minecraft, 3D vectors are used to express velocities of entities.").usage("vector(x, y, z)").examples("").since("2.2-dev23").defaultExpression(new EventValueExpression<Vector>(Vector.class)).parser(new VectorParser()).serializer(new VectorSerializer()).cloner(Vector::clone).property(Property.WXYZ, "X, Y, or Z component of the vector.", Skript.instance(), new VectorWXYZHandler());
    }

    private static class VectorParser
    extends Parser<Vector> {
        private VectorParser() {
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(Vector vec, int flags) {
            return "x: " + Skript.toString(vec.getX()) + ", y: " + Skript.toString(vec.getY()) + ", z: " + Skript.toString(vec.getZ());
        }

        @Override
        public String toVariableNameString(Vector vec) {
            return "vector:" + vec.getX() + "," + vec.getY() + "," + vec.getZ();
        }

        @Override
        public String getDebugMessage(Vector vec) {
            return "(" + vec.getX() + "," + vec.getY() + "," + vec.getZ() + ")";
        }
    }

    private static class VectorSerializer
    extends Serializer<Vector> {
        private VectorSerializer() {
        }

        @Override
        public Fields serialize(Vector o) {
            Fields f = new Fields();
            f.putPrimitive("x", o.getX());
            f.putPrimitive("y", o.getY());
            f.putPrimitive("z", o.getZ());
            return f;
        }

        @Override
        public void deserialize(Vector o, Fields f) {
            assert (false);
        }

        @Override
        public Vector deserialize(Fields f) throws StreamCorruptedException {
            return new Vector(f.getPrimitive("x", Double.TYPE).doubleValue(), f.getPrimitive("y", Double.TYPE).doubleValue(), f.getPrimitive("z", Double.TYPE).doubleValue());
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }

        @Override
        protected boolean canBeInstantiated() {
            return false;
        }
    }

    private static class VectorWXYZHandler
    extends WXYZHandler<Vector, Double> {
        private VectorWXYZHandler() {
        }

        @Override
        public PropertyHandler<Vector> newInstance() {
            VectorWXYZHandler instance = new VectorWXYZHandler();
            instance.axis(this.axis);
            return instance;
        }

        @Override
        @Nullable
        public Double convert(Vector propertyHolder) {
            return switch (this.axis) {
                case WXYZHandler.Axis.X -> propertyHolder.getX();
                case WXYZHandler.Axis.Y -> propertyHolder.getY();
                case WXYZHandler.Axis.Z -> propertyHolder.getZ();
                default -> null;
            };
        }

        @Override
        public boolean supportsAxis(WXYZHandler.Axis axis) {
            return axis != WXYZHandler.Axis.W;
        }

        @Override
        public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
            Class[] classArray;
            switch (mode) {
                case ADD: 
                case SET: 
                case REMOVE: {
                    Class[] classArray2 = new Class[1];
                    classArray = classArray2;
                    classArray2[0] = Float.class;
                    break;
                }
                default: {
                    classArray = null;
                }
            }
            return classArray;
        }

        @Override
        public void change(Vector vector, Object @Nullable [] delta, Changer.ChangeMode mode) {
            assert (delta != null);
            float value = ((Float)delta[0]).floatValue();
            if (this.axis == WXYZHandler.Axis.W) {
                return;
            }
            switch (mode) {
                case REMOVE: {
                    value = -value;
                }
                case ADD: {
                    switch (this.axis) {
                        case X: {
                            vector.setX(vector.getX() + (double)value);
                            break;
                        }
                        case Y: {
                            vector.setY(vector.getY() + (double)value);
                            break;
                        }
                        case Z: {
                            vector.setZ(vector.getZ() + (double)value);
                        }
                    }
                    break;
                }
                case SET: {
                    switch (this.axis) {
                        case X: {
                            vector.setX(value);
                            break;
                        }
                        case Y: {
                            vector.setY(value);
                            break;
                        }
                        case Z: {
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

        @Override
        @NotNull
        public Class<Double> returnType() {
            return Double.class;
        }

        @Override
        public boolean requiresSourceExprChange() {
            return true;
        }
    }
}

