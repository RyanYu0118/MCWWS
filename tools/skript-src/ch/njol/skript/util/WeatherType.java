/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.WeatherType
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.event.weather.ThunderChangeEvent
 *  org.bukkit.event.weather.WeatherChangeEvent
 *  org.bukkit.event.weather.WeatherEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.localization.Language;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.jetbrains.annotations.Nullable;

public final class WeatherType
extends Enum<WeatherType> {
    public static final /* enum */ WeatherType CLEAR = new WeatherType(new String[0]);
    public static final /* enum */ WeatherType RAIN = new WeatherType(new String[0]);
    public static final /* enum */ WeatherType THUNDER = new WeatherType(new String[0]);
    String[] names;
    @Nullable
    String adjective;
    static final Map<String, WeatherType> byName;
    private static final /* synthetic */ WeatherType[] $VALUES;

    public static WeatherType[] values() {
        return (WeatherType[])$VALUES.clone();
    }

    public static WeatherType valueOf(String name) {
        return Enum.valueOf(WeatherType.class, name);
    }

    private WeatherType(String ... names) {
        this.names = names;
    }

    @Nullable
    public static WeatherType parse(String s) {
        return byName.get(s);
    }

    public static WeatherType fromWorld(World world) {
        assert (world != null);
        if (world.isThundering() && world.hasStorm()) {
            return THUNDER;
        }
        if (world.hasStorm()) {
            return RAIN;
        }
        return CLEAR;
    }

    public static WeatherType fromEvent(WeatherEvent e) {
        if (e instanceof WeatherChangeEvent) {
            return WeatherType.fromEvent((WeatherChangeEvent)e);
        }
        if (e instanceof ThunderChangeEvent) {
            return WeatherType.fromEvent((ThunderChangeEvent)e);
        }
        assert (false);
        return CLEAR;
    }

    public static WeatherType fromEvent(WeatherChangeEvent e) {
        assert (e != null);
        if (!e.toWeatherState()) {
            return CLEAR;
        }
        if (e.getWorld().isThundering()) {
            return THUNDER;
        }
        return RAIN;
    }

    public static WeatherType fromEvent(ThunderChangeEvent e) {
        assert (e != null);
        if (e.toThunderState()) {
            return THUNDER;
        }
        if (e.getWorld().hasStorm()) {
            return RAIN;
        }
        return CLEAR;
    }

    @Nullable
    public static WeatherType fromPlayer(Player player) {
        org.bukkit.WeatherType weather = player.getPlayerWeather();
        if (weather == null) {
            return null;
        }
        switch (weather) {
            case DOWNFALL: {
                return RAIN;
            }
            case CLEAR: {
                return CLEAR;
            }
        }
        return null;
    }

    public void setWeather(Player player) {
        switch (this.ordinal()) {
            case 1: 
            case 2: {
                player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
                break;
            }
            case 0: {
                player.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
                break;
            }
            default: {
                player.resetPlayerWeather();
            }
        }
    }

    public String toString() {
        return this.names[0];
    }

    public String toString(int flags) {
        return this.names[0];
    }

    @Nullable
    public String adjective() {
        return this.adjective;
    }

    public boolean isWeather(World w) {
        return this.isWeather(w.hasStorm(), w.isThundering());
    }

    public boolean isWeather(boolean rain, boolean thunder) {
        switch (this.ordinal()) {
            case 0: {
                return !thunder && !rain;
            }
            case 1: {
                return !thunder && rain;
            }
            case 2: {
                return thunder && rain;
            }
        }
        assert (false);
        return false;
    }

    public void setWeather(World w) {
        if (w.isThundering() != (this == THUNDER)) {
            w.setThundering(this == THUNDER);
        }
        if (w.hasStorm() == (this == CLEAR)) {
            w.setStorm(this != CLEAR);
        }
    }

    private static /* synthetic */ WeatherType[] $values() {
        return new WeatherType[]{CLEAR, RAIN, THUNDER};
    }

    static {
        $VALUES = WeatherType.$values();
        byName = new HashMap<String, WeatherType>();
        Language.addListener(() -> {
            byName.clear();
            for (WeatherType t : WeatherType.values()) {
                t.names = Language.getList("weather." + t.name() + ".name");
                t.adjective = Language.get("weather." + t.name() + ".adjective");
                for (String name : t.names) {
                    byName.put(name, t);
                }
            }
        });
    }
}

