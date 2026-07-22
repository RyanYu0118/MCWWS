/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.weather.ThunderChangeEvent
 *  org.bukkit.event.weather.WeatherChangeEvent
 *  org.bukkit.event.weather.WeatherEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.WeatherType;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Weather")
@Description(value={"The weather of a world or player.", "Clearing or resetting the weather of a player will make the player's weather match the weather of the world.", "Clearing or resetting the weather of a world will make the weather clear."})
@Example.Examples(value={@Example(value="set weather to clear"), @Example(value="weather in \"world\" is rainy"), @Example(value="reset custom weather of player"), @Example(value="set weather of player to clear")})
@Since(value={"1.0"})
@Events(value={"weather change"})
public class ExprWeather
extends PropertyExpression<Object, WeatherType> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected WeatherType @Nullable [] get(Event event, Object[] source) {
        World world;
        if (event instanceof WeatherEvent) {
            WeatherEvent weatherEvent = (WeatherEvent)event;
            world = weatherEvent.getWorld();
        } else {
            world = null;
        }
        World eventWorld = world;
        return this.get(source, object -> {
            if (object instanceof Player) {
                Player player = (Player)object;
                return WeatherType.fromPlayer(player);
            }
            if (object instanceof World) {
                Cancellable cancellable;
                World world = (World)object;
                if (!(eventWorld == null || !world.equals((Object)eventWorld) || this.getTime() < 0 || event instanceof Cancellable && (cancellable = (Cancellable)event).isCancelled())) {
                    return WeatherType.fromEvent((WeatherEvent)event);
                }
                return WeatherType.fromWorld(world);
            }
            return null;
        });
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
            return CollectionUtils.array(WeatherType.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        World world;
        WeatherType worldWeather;
        WeatherType playerWeather = delta != null ? (WeatherType)((Object)delta[0]) : null;
        WeatherType weatherType = worldWeather = playerWeather != null ? playerWeather : WeatherType.CLEAR;
        if (event instanceof WeatherEvent) {
            WeatherEvent weatherEvent = (WeatherEvent)event;
            world = weatherEvent.getWorld();
        } else {
            world = null;
        }
        World eventWorld = world;
        for (Object object : this.getExpr().getArray(event)) {
            if (object instanceof Player) {
                Player player = (Player)object;
                if (playerWeather != null) {
                    playerWeather.setWeather(player);
                    continue;
                }
                player.resetPlayerWeather();
                continue;
            }
            if (!(object instanceof World)) continue;
            World world2 = (World)object;
            if (eventWorld != null && world2.equals((Object)eventWorld) && this.getTime() >= 0) {
                if (event instanceof WeatherChangeEvent) {
                    WeatherChangeEvent weatherChangeEvent = (WeatherChangeEvent)event;
                    if (weatherChangeEvent.toWeatherState() && worldWeather == WeatherType.CLEAR) {
                        weatherChangeEvent.setCancelled(true);
                    } else if (!weatherChangeEvent.toWeatherState() && worldWeather == WeatherType.RAIN) {
                        eventWorld.setStorm(true);
                    }
                    if (eventWorld.isThundering() == (worldWeather == WeatherType.THUNDER)) continue;
                    eventWorld.setThundering(worldWeather == WeatherType.THUNDER);
                    continue;
                }
                if (!(event instanceof ThunderChangeEvent)) continue;
                ThunderChangeEvent thunderChangeEvent = (ThunderChangeEvent)event;
                if (thunderChangeEvent.toThunderState() && worldWeather != WeatherType.THUNDER) {
                    thunderChangeEvent.setCancelled(true);
                } else if (!thunderChangeEvent.toThunderState() && worldWeather == WeatherType.THUNDER) {
                    eventWorld.setThundering(true);
                }
                if (eventWorld.hasStorm() != (worldWeather == WeatherType.CLEAR)) continue;
                eventWorld.setStorm(worldWeather != WeatherType.CLEAR);
                continue;
            }
            worldWeather.setWeather(world2);
        }
    }

    @Override
    public Class<WeatherType> getReturnType() {
        return WeatherType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "weather of " + this.getExpr().toString(event, debug);
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, this.getExpr(), WeatherChangeEvent.class, ThunderChangeEvent.class);
    }

    static {
        Skript.registerExpression(ExprWeather.class, WeatherType.class, ExpressionType.PROPERTY, "[the] weather [(in|of) %players/worlds%]", "[the] (custom|client) weather [of %players%]", "%players/worlds%'[s] weather", "%players%'[s] (custom|client) weather");
    }
}

