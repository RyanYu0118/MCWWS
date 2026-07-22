/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.FireworkEffect
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.FireworkExplodeEvent
 *  org.bukkit.inventory.meta.FireworkMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.FireworkEffect;
import org.bukkit.event.Event;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.Nullable;

public class EvtFirework
extends SkriptEvent {
    @Nullable
    private Literal<Color> colors;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (args[0] != null) {
            this.colors = args[0];
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof FireworkExplodeEvent)) {
            return false;
        }
        FireworkExplodeEvent fireworkExplodeEvent = (FireworkExplodeEvent)event;
        if (this.colors == null) {
            return true;
        }
        Set colours = this.colors.stream(event).map(color -> {
            if (color instanceof ColorRGB) {
                return color.asBukkitColor();
            }
            return color.asDyeColor().getFireworkColor();
        }).collect(Collectors.toSet());
        FireworkMeta meta = fireworkExplodeEvent.getEntity().getFireworkMeta();
        for (FireworkEffect effect : meta.getEffects()) {
            if (!colours.containsAll(effect.getColors())) continue;
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"firework explode");
        if (this.colors != null) {
            builder.append((Object)"with colors").append((Object)this.colors);
        }
        return builder.toString();
    }

    static {
        if (Skript.classExists("org.bukkit.event.entity.FireworkExplodeEvent")) {
            Skript.registerEvent("Firework Explode", EvtFirework.class, FireworkExplodeEvent.class, "[a] firework explo(d(e|ing)|sion) [colo[u]red %-colors%]").description("Called when a firework explodes.").examples("on firework explode:", "\tif event-colors contains red:", "on firework exploding colored red, light green and black:", "on firework explosion colored rgb 0, 255, 0:", "\tbroadcast \"A firework colored %colors% was exploded at %location%!\"").since("2.4");
        }
    }
}

