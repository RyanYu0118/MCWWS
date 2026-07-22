/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaternionf
 */
package org.skriptlang.skript.common.types;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.WXYZHandler;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Internal
public class QuaternionClassInfo
extends ClassInfo<Quaternionf> {
    public QuaternionClassInfo() {
        super(Quaternionf.class, "quaternion");
        this.user("quaternionf?s?").name("Quaternion").description("Quaternions are four dimensional vectors, often used for representing rotations.").since("2.10").parser(new QuaternionParser()).defaultExpression(new EventValueExpression<Quaternionf>(Quaternionf.class)).cloner(quaternion -> {
            try {
                return (Quaternionf)quaternion.clone();
            }
            catch (CloneNotSupportedException e) {
                return null;
            }
        }).property(Property.WXYZ, "W, X, Y, or Z component of the quaternion.", Skript.instance(), new QuaternionWXYZHandler());
    }

    private static class QuaternionParser
    extends Parser<Quaternionf> {
        private QuaternionParser() {
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(Quaternionf quaternion, int flags) {
            return "w:" + Skript.toString(quaternion.w()) + ", x:" + Skript.toString(quaternion.x()) + ", y:" + Skript.toString(quaternion.y()) + ", z:" + Skript.toString(quaternion.z());
        }

        @Override
        public String toVariableNameString(Quaternionf quaternion) {
            return quaternion.w() + "," + quaternion.x() + "," + quaternion.y() + "," + quaternion.z();
        }
    }

    private static class QuaternionWXYZHandler
    extends WXYZHandler<Quaternionf, Float> {
        private QuaternionWXYZHandler() {
        }

        @Override
        public PropertyHandler<Quaternionf> newInstance() {
            QuaternionWXYZHandler instance = new QuaternionWXYZHandler();
            instance.axis(this.axis);
            return instance;
        }

        @Override
        @NotNull
        public Float convert(Quaternionf propertyHolder) {
            return switch (this.axis) {
                default -> throw new MatchException(null, null);
                case WXYZHandler.Axis.W -> Float.valueOf(propertyHolder.w);
                case WXYZHandler.Axis.X -> Float.valueOf(propertyHolder.x);
                case WXYZHandler.Axis.Y -> Float.valueOf(propertyHolder.y);
                case WXYZHandler.Axis.Z -> Float.valueOf(propertyHolder.z);
            };
        }

        @Override
        public boolean supportsAxis(WXYZHandler.Axis axis) {
            return true;
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
        public void change(Quaternionf propertyHolder, Object @Nullable [] delta, Changer.ChangeMode mode) {
            assert (delta != null);
            float value = ((Float)delta[0]).floatValue();
            float x = propertyHolder.x();
            float y = propertyHolder.y();
            float z = propertyHolder.z();
            float w = propertyHolder.w();
            block0 : switch (mode) {
                case REMOVE: {
                    value = -value;
                }
                case ADD: {
                    switch (this.axis) {
                        case W: {
                            w += value;
                            break;
                        }
                        case X: {
                            x += value;
                            break;
                        }
                        case Y: {
                            y += value;
                            break;
                        }
                        case Z: {
                            z += value;
                        }
                    }
                    break;
                }
                case SET: {
                    switch (this.axis) {
                        case W: {
                            w = value;
                            break block0;
                        }
                        case X: {
                            x = value;
                            break block0;
                        }
                        case Y: {
                            y = value;
                            break block0;
                        }
                        case Z: {
                            z = value;
                        }
                    }
                }
            }
            propertyHolder.set(x, y, z, w);
        }

        @Override
        @NotNull
        public Class<Float> returnType() {
            return Float.class;
        }

        @Override
        public boolean requiresSourceExprChange() {
            return true;
        }
    }
}

