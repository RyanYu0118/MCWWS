/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.WXYZHandler;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Internal
public class LocationClassInfo
extends ClassInfo<Location> {
    public LocationClassInfo() {
        super(Location.class, "location");
        this.user("locations?").name("Location").description("A location in a <a href='#world'>world</a>. Locations are world-specific and even store a <a href='#direction'>direction</a>, e.g. if you save a location and later teleport to it you will face the exact same direction you did when you saved the location.").usage("").examples("teleport player to location at 0, 69, 0", "set {home::%uuid of player%} to location of the player").since("1.0").defaultExpression(new EventValueExpression<Location>(Location.class)).parser(new LocationParser()).serializer(new LocationSerializer()).cloner(Location::clone).property(Property.WXYZ, "The X, Y, or Z coordinate of the location.", Skript.instance(), new LocationWXYZHandler());
    }

    private static class LocationParser
    extends Parser<Location> {
        private LocationParser() {
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(Location loc, int flags) {
            String worldPart = loc.getWorld() == null ? "" : " in '" + loc.getWorld().getName() + "'";
            return "x: " + Skript.toString(loc.getX()) + ", y: " + Skript.toString(loc.getY()) + ", z: " + Skript.toString(loc.getZ()) + ", yaw: " + Skript.toString(loc.getYaw()) + ", pitch: " + Skript.toString(loc.getPitch()) + worldPart;
        }

        @Override
        public String toVariableNameString(Location loc) {
            return loc.getWorld().getName() + ":" + loc.getX() + "," + loc.getY() + "," + loc.getZ();
        }

        @Override
        public String getDebugMessage(Location loc) {
            return "(" + loc.getWorld().getName() + ":" + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "|yaw=" + loc.getYaw() + "/pitch=" + loc.getPitch() + ")";
        }
    }

    private static class LocationSerializer
    extends Serializer<Location> {
        private LocationSerializer() {
        }

        @Override
        public Fields serialize(Location location) {
            Fields fields = new Fields();
            World world = null;
            try {
                world = location.getWorld();
            }
            catch (IllegalArgumentException exception) {
                Skript.warning("A location failed to serialize with its defined world, as the world was unloaded.");
            }
            fields.putObject("world", world);
            fields.putPrimitive("x", location.getX());
            fields.putPrimitive("y", location.getY());
            fields.putPrimitive("z", location.getZ());
            fields.putPrimitive("yaw", Float.valueOf(location.getYaw()));
            fields.putPrimitive("pitch", Float.valueOf(location.getPitch()));
            return fields;
        }

        @Override
        public void deserialize(Location o, Fields f) {
            assert (false);
        }

        @Override
        public Location deserialize(Fields f) throws StreamCorruptedException {
            return new Location(f.getObject("world", World.class), f.getPrimitive("x", Double.TYPE).doubleValue(), f.getPrimitive("y", Double.TYPE).doubleValue(), f.getPrimitive("z", Double.TYPE).doubleValue(), f.getPrimitive("yaw", Float.TYPE).floatValue(), f.getPrimitive("pitch", Float.TYPE).floatValue());
        }

        @Override
        public boolean canBeInstantiated() {
            return false;
        }

        @Override
        public boolean mustSyncDeserialization() {
            return true;
        }

        @Override
        @Nullable
        public Location deserialize(String input) {
            String[] split = input.split("[:,|/]");
            if (split.length != 6) {
                return null;
            }
            World w = Bukkit.getWorld((String)split[0]);
            if (w == null) {
                return null;
            }
            try {
                double[] l = new double[5];
                for (int i = 0; i < 5; ++i) {
                    l[i] = Double.parseDouble(split[i + 1]);
                }
                return new Location(w, l[0], l[1], l[2], (float)l[3], (float)l[4]);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private static class LocationWXYZHandler
    extends WXYZHandler<Location, Double> {
        private LocationWXYZHandler() {
        }

        @Override
        public PropertyHandler<Location> newInstance() {
            LocationWXYZHandler instance = new LocationWXYZHandler();
            instance.axis(this.axis);
            return instance;
        }

        @Override
        @Nullable
        public Double convert(Location propertyHolder) {
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
        public void change(Location location, Object @Nullable [] delta, Changer.ChangeMode mode) {
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
                            location.setX(location.getX() + (double)value);
                            break;
                        }
                        case Y: {
                            location.setY(location.getY() + (double)value);
                            break;
                        }
                        case Z: {
                            location.setZ(location.getZ() + (double)value);
                        }
                    }
                    break;
                }
                case SET: {
                    switch (this.axis) {
                        case X: {
                            location.setX((double)value);
                            break;
                        }
                        case Y: {
                            location.setY((double)value);
                            break;
                        }
                        case Z: {
                            location.setZ((double)value);
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

