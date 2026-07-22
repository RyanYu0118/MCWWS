/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.registrations;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class DefaultClasses {
    public static ClassInfo<Object> OBJECT = DefaultClasses.getClassInfo(Object.class);
    public static ClassInfo<Number> NUMBER = DefaultClasses.getClassInfo(Number.class);
    public static ClassInfo<Long> LONG = DefaultClasses.getClassInfo(Long.class);
    public static ClassInfo<Boolean> BOOLEAN = DefaultClasses.getClassInfo(Boolean.class);
    public static ClassInfo<String> STRING = DefaultClasses.getClassInfo(String.class);
    public static ClassInfo<OfflinePlayer> OFFLINE_PLAYER = DefaultClasses.getClassInfo(OfflinePlayer.class);
    public static ClassInfo<Location> LOCATION = DefaultClasses.getClassInfo(Location.class);
    public static ClassInfo<Vector> VECTOR = DefaultClasses.getClassInfo(Vector.class);
    public static ClassInfo<Player> PLAYER = DefaultClasses.getClassInfo(Player.class);
    public static ClassInfo<World> WORLD = DefaultClasses.getClassInfo(World.class);
    public static ClassInfo<Color> COLOR = DefaultClasses.getClassInfo(Color.class);
    public static ClassInfo<Date> DATE = DefaultClasses.getClassInfo(Date.class);
    public static ClassInfo<Timespan> TIMESPAN = DefaultClasses.getClassInfo(Timespan.class);

    @NotNull
    private static <T> ClassInfo<T> getClassInfo(Class<T> type) {
        ClassInfo<T> classInfo = Classes.getExactClassInfo(type);
        if (classInfo == null) {
            throw new NullPointerException();
        }
        return classInfo;
    }
}

