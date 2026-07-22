/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.weather.ThunderChangeEvent
 *  org.bukkit.event.weather.WeatherChangeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.WeatherType;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.jetbrains.annotations.Nullable;

public class EvtWeatherChange
extends SkriptEvent {
    @Nullable
    private Literal<WeatherType> types;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.types = args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (this.types == null) {
            return true;
        }
        if (!(e instanceof WeatherChangeEvent) && !(e instanceof ThunderChangeEvent)) {
            return false;
        }
        boolean rain = e instanceof WeatherChangeEvent ? ((WeatherChangeEvent)e).toWeatherState() : ((ThunderChangeEvent)e).getWorld().hasStorm();
        boolean thunder = e instanceof ThunderChangeEvent ? ((ThunderChangeEvent)e).toThunderState() : ((WeatherChangeEvent)e).getWorld().isThundering();
        return this.types.check(e, t -> t.isWeather(rain, thunder));
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "weather change" + (String)(this.types == null ? "" : " to " + String.valueOf(this.types));
    }

    static {
        Skript.registerEvent("Weather Change", EvtWeatherChange.class, CollectionUtils.array(WeatherChangeEvent.class, ThunderChangeEvent.class), "weather change [to %-weathertypes%]").description("Called when a world's weather changes.").examples("on weather change:", "on weather change to sunny:").since("1.0");
    }
}

