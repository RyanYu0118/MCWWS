/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import java.lang.reflect.Method;
import org.bukkit.entity.Entity;

public abstract class PassengerUtils {
    private static Method getPassenger = null;
    private static Method setPassenger = null;

    private PassengerUtils() {
    }

    public static Entity[] getPassenger(Entity e) {
        if (PassengerUtils.hasMultiplePassenger()) {
            return e.getPassengers().toArray(new Entity[0]);
        }
        try {
            return new Entity[]{(Entity)getPassenger.invoke((Object)e, new Object[0])};
        }
        catch (Exception ex) {
            Skript.exception((Throwable)ex, "A error occured while trying to get a passenger in version lower than 1.11.2.");
            return null;
        }
    }

    public static void addPassenger(Entity vehicle, Entity passenger) {
        if (vehicle == null || passenger == null) {
            return;
        }
        if (PassengerUtils.hasMultiplePassenger()) {
            vehicle.addPassenger(passenger);
        } else {
            try {
                vehicle.eject();
                setPassenger.invoke((Object)vehicle, passenger);
            }
            catch (Exception ex) {
                Skript.exception((Throwable)ex, "A error occured while trying to set a passenger in version lower than 1.11.2.");
            }
        }
    }

    public static void removePassenger(Entity vehicle, Entity passenger) {
        if (vehicle == null || passenger == null) {
            return;
        }
        if (PassengerUtils.hasMultiplePassenger()) {
            vehicle.removePassenger(passenger);
        } else {
            vehicle.eject();
        }
    }

    public static boolean hasMultiplePassenger() {
        return setPassenger == null;
    }

    static {
        if (!Skript.methodExists(Entity.class, "getPassengers", new Class[0])) {
            try {
                getPassenger = Entity.class.getDeclaredMethod("getPassenger", new Class[0]);
                setPassenger = Entity.class.getDeclaredMethod("setPassenger", Entity.class);
            }
            catch (NoSuchMethodException ex) {
                Skript.outdatedError(ex);
            }
            catch (Exception ex) {
                Skript.exception((Throwable)ex, new String[0]);
            }
        }
    }
}

