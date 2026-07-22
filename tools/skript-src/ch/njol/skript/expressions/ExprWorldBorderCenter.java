/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.WorldBorder
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Center of World Border")
@Description(value={"The center of a world border."})
@Example(value="set world border center of {_worldborder} to location(10, 0, 20)")
@Since(value={"2.11"})
public class ExprWorldBorderCenter
extends SimplePropertyExpression<WorldBorder, Location> {
    @Override
    @Nullable
    public Location convert(WorldBorder worldBorder) {
        return worldBorder.getCenter();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> CollectionUtils.array(Location.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Location location;
        Location location2 = location = mode == Changer.ChangeMode.SET ? (Location)delta[0] : new Location(null, 0.0, 0.0, 0.0);
        if (Double.isNaN(location.getX()) || Double.isNaN(location.getZ())) {
            this.error("Your location can't have a NaN value as one of its components");
            return;
        }
        block4: for (WorldBorder worldBorder : (WorldBorder[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: {
                    if (Math.abs(location.getX()) > worldBorder.getMaxCenterCoordinate()) {
                        location.setX(worldBorder.getMaxCenterCoordinate() * Math.signum(location.getX()));
                    }
                    if (Math.abs(location.getZ()) > worldBorder.getMaxCenterCoordinate()) {
                        location.setZ(worldBorder.getMaxCenterCoordinate() * Math.signum(location.getZ()));
                    }
                    worldBorder.setCenter(location);
                    continue block4;
                }
                case RESET: {
                    worldBorder.setCenter(0.0, 0.0);
                }
            }
        }
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    protected String getPropertyName() {
        return "world border center";
    }

    static {
        ExprWorldBorderCenter.registerDefault(ExprWorldBorderCenter.class, Location.class, "world[ ]border (center|middle)", "worldborders");
    }
}

