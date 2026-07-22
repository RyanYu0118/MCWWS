/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@Name(value="Yaw / Pitch")
@Description(value={"The yaw or pitch of a location or vector.", "A yaw of 0 or 360 represents the positive z direction. Adding a positive number to the yaw of a player will rotate it clockwise.", "A pitch of 90 represents the negative y direction, or downward facing. A pitch of -90 represents upward facing. Adding a positive number to the pitch will rotate the direction downwards.", "Only Paper 1.19+ users may directly change the yaw/pitch of players."})
@Example.Examples(value={@Example(value="log \"%player%: %location of player%, %player's yaw%, %player's pitch%\" to \"playerlocs.log\""), @Example(value="set {_yaw} to yaw of player"), @Example(value="set {_p} to pitch of target entity"), @Example(value="set pitch of player to -90 # Makes the player look upwards, Paper 1.19+ only"), @Example(value="add 180 to yaw of target of player # Makes the target look behind themselves")})
@Since(value={"2.0, 2.2-dev28 (vector yaw/pitch), 2.9.0 (entity changers)"})
public class ExprYawPitch
extends SimplePropertyExpression<Object, Float> {
    private static final double DEG_TO_RAD = Math.PI / 180;
    private static final double RAD_TO_DEG = 57.29577951308232;
    private boolean usesYaw;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.usesYaw = parseResult.hasTag("yaw");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public Float convert(Object object) {
        if (object instanceof Entity) {
            Entity entity = (Entity)object;
            Location location = entity.getLocation();
            return Float.valueOf(this.usesYaw ? ExprYawPitch.normalizeYaw(location.getYaw()) : location.getPitch());
        }
        if (object instanceof Location) {
            Location location = (Location)object;
            return Float.valueOf(this.usesYaw ? ExprYawPitch.normalizeYaw(location.getYaw()) : location.getPitch());
        }
        if (object instanceof Vector) {
            Vector vector = (Vector)object;
            return Float.valueOf(this.usesYaw ? ExprYawPitch.skriptYaw(ExprYawPitch.getYaw(vector)) : ExprYawPitch.skriptPitch(ExprYawPitch.getPitch(vector)));
        }
        return null;
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        float value;
        float f = value = delta == null ? 0.0f : ((Number)delta[0]).floatValue();
        if (!Float.isFinite(value)) {
            return;
        }
        for (Object object : this.getExpr().getArray(event)) {
            if (object instanceof Entity) {
                Entity entity = (Entity)object;
                this.changeForEntity(entity, value, mode);
                continue;
            }
            if (object instanceof Location) {
                Location location = (Location)object;
                this.changeForLocation(location, value, mode);
                continue;
            }
            if (!(object instanceof Vector)) continue;
            Vector vector = (Vector)object;
            this.changeForVector(vector, value, mode);
        }
    }

    private void changeForEntity(Entity entity, float value, Changer.ChangeMode mode) {
        Location location = entity.getLocation();
        this.changeForLocation(location, value, mode);
        entity.setRotation(location.getYaw(), location.getPitch());
    }

    private void changeForLocation(Location location, float value, Changer.ChangeMode mode) {
        switch (mode) {
            case SET: {
                if (this.usesYaw) {
                    location.setYaw(value);
                    break;
                }
                location.setPitch(value);
                break;
            }
            case REMOVE: {
                value = -value;
            }
            case ADD: {
                if (this.usesYaw) {
                    location.setYaw(location.getYaw() + value);
                    break;
                }
                location.setPitch(location.getPitch() - value);
                break;
            }
            case RESET: {
                if (this.usesYaw) {
                    location.setYaw(0.0f);
                    break;
                }
                location.setPitch(0.0f);
            }
        }
    }

    private void changeForVector(Vector vector, float value, Changer.ChangeMode mode) {
        float yaw = ExprYawPitch.getYaw(vector);
        float pitch = ExprYawPitch.getPitch(vector);
        switch (mode) {
            case REMOVE: {
                value = -value;
            }
            case ADD: {
                if (this.usesYaw) {
                    yaw += value;
                    break;
                }
                pitch -= value;
                break;
            }
            case SET: {
                if (this.usesYaw) {
                    yaw = ExprYawPitch.fromSkriptYaw(value);
                    break;
                }
                pitch = ExprYawPitch.fromSkriptPitch(value);
            }
        }
        Vector newVector = ExprYawPitch.fromYawAndPitch(yaw, pitch).multiply(vector.length());
        vector.copy(newVector);
    }

    private static float normalizeYaw(float yaw) {
        return (yaw = Location.normalizeYaw((float)yaw)) < 0.0f ? yaw + 360.0f : yaw;
    }

    @Override
    public Class<? extends Float> getReturnType() {
        return Float.class;
    }

    @Override
    protected String getPropertyName() {
        return this.usesYaw ? "yaw" : "pitch";
    }

    @ApiStatus.Internal
    public static Vector fromYawAndPitch(float yaw, float pitch) {
        double y = Math.sin((double)pitch * (Math.PI / 180));
        double div = Math.cos((double)pitch * (Math.PI / 180));
        double x = Math.cos((double)yaw * (Math.PI / 180));
        double z = Math.sin((double)yaw * (Math.PI / 180));
        return new Vector(x *= div, y, z *= div);
    }

    private static float getYaw(Vector vector) {
        if (Double.valueOf(vector.getX()).equals(0.0) && Double.valueOf(vector.getZ()).equals(0.0)) {
            return 0.0f;
        }
        return (float)(Math.atan2(vector.getZ(), vector.getX()) * 57.29577951308232);
    }

    private static float getPitch(Vector vector) {
        double xy = Math.sqrt(vector.getX() * vector.getX() + vector.getZ() * vector.getZ());
        return (float)(Math.atan(vector.getY() / xy) * 57.29577951308232);
    }

    private static float skriptYaw(float yaw) {
        return yaw < 90.0f ? yaw + 270.0f : yaw - 90.0f;
    }

    private static float skriptPitch(float pitch) {
        return -pitch;
    }

    @ApiStatus.Internal
    public static float fromSkriptYaw(float yaw) {
        return yaw > 270.0f ? yaw - 270.0f : yaw + 90.0f;
    }

    @ApiStatus.Internal
    public static float fromSkriptPitch(float pitch) {
        return -pitch;
    }

    static {
        ExprYawPitch.register(ExprYawPitch.class, Float.class, "(:yaw|pitch)", "entities/locations/vectors");
    }
}

