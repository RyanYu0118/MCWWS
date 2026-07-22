/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Effect
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.particles;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.EnumParser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameEffect {
    private static final GameEffectParser ENUM_PARSER = new GameEffectParser();
    private final Effect effect;
    @Nullable
    private Object data;

    public GameEffect(Effect effect) {
        this.effect = effect;
    }

    public static GameEffect parse(String input) {
        Effect effect = (Effect)ENUM_PARSER.parse(input.toLowerCase(Locale.ENGLISH), ParseContext.DEFAULT);
        if (effect == null) {
            return null;
        }
        if (effect.getData() != null) {
            Skript.error("The effect " + Classes.toString(effect) + " requires data and cannot be parsed directly. Use the Game Effect expression instead.");
            return null;
        }
        return new GameEffect(effect);
    }

    public Effect getEffect() {
        return this.effect;
    }

    @Nullable
    public Object getData() {
        return this.data;
    }

    public boolean setData(Object data) {
        if (this.effect.getData() != null && this.effect.getData().isInstance(data)) {
            this.data = data;
            return true;
        }
        if (this.effect == Effect.ELECTRIC_SPARK && data == null) {
            this.data = null;
            return true;
        }
        return false;
    }

    public void draw(@NotNull Location location, @Nullable Number radius) {
        if (this.effect.getData() != null && this.data == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (radius == null) {
            location.getWorld().playEffect(location, this.effect, this.data);
        } else {
            location.getWorld().playEffect(location, this.effect, this.data, radius.intValue());
        }
    }

    public void drawForPlayer(Location location, @NotNull Player player) {
        player.playEffect(location, this.effect, this.data);
    }

    public String toString(int flags) {
        return ENUM_PARSER.toString(this.getEffect(), flags);
    }

    public String toString() {
        return this.toString(0);
    }

    public static String[] getAllNamesWithoutData() {
        return ENUM_PARSER.getPatternsWithoutData();
    }

    private static class GameEffectParser
    extends EnumParser<Effect> {
        public GameEffectParser() {
            super(Effect.class, "game effect");
        }

        public String @NotNull [] getPatternsWithoutData() {
            return (String[])this.parseMap.entrySet().stream().filter(entry -> {
                Effect effect = (Effect)entry.getValue();
                return effect.getData() == null;
            }).map(Map.Entry::getKey).toArray(String[]::new);
        }
    }
}

