/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.FireworkEffect
 *  org.bukkit.FireworkEffect$Builder
 *  org.bukkit.FireworkEffect$Type
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.FireworkEffect;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Firework Effect")
@Description(value={"Represents a 'firework effect' which can be used in the <a href='#EffFireworkLaunch'>launch firework</a> effect."})
@Example.Examples(value={@Example(value="launch flickering trailing burst firework colored blue and green at player"), @Example(value="launch trailing flickering star colored purple, yellow, blue, green and red fading to pink at target entity"), @Example(value="launch ball large colored red, purple and white fading to light green and black at player's location with duration 1")})
@Since(value={"2.4"})
public class ExprFireworkEffect
extends SimpleExpression<FireworkEffect> {
    private Expression<FireworkEffect.Type> type;
    private Expression<Color> color;
    private Expression<Color> fade;
    private boolean flicker;
    private boolean trail;
    private boolean hasFade;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.flicker = parseResult.mark == 2 || parseResult.mark > 3;
        this.trail = parseResult.mark >= 3;
        this.hasFade = matchedPattern == 1;
        this.type = exprs[0];
        this.color = exprs[1];
        if (this.hasFade) {
            this.fade = exprs[2];
        }
        return true;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends FireworkEffect> getReturnType() {
        return FireworkEffect.class;
    }

    @Nullable
    protected FireworkEffect[] get(Event e) {
        FireworkEffect.Type type = this.type.getSingle(e);
        if (type == null) {
            return null;
        }
        FireworkEffect.Builder builder = FireworkEffect.builder().with(type);
        for (Color colour : this.color.getArray(e)) {
            if (colour instanceof ColorRGB) {
                builder.withColor(colour.asBukkitColor());
                continue;
            }
            builder.withColor(colour.asDyeColor().getFireworkColor());
        }
        if (this.hasFade) {
            for (Color colour : this.fade.getArray(e)) {
                if (colour instanceof ColorRGB) {
                    builder.withFade(colour.asBukkitColor());
                    continue;
                }
                builder.withFade(colour.asDyeColor().getFireworkColor());
            }
        }
        builder.flicker(this.flicker);
        builder.trail(this.trail);
        return CollectionUtils.array(builder.build());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "Firework effect " + this.type.toString(e, debug) + " with color " + this.color.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprFireworkEffect.class, FireworkEffect.class, ExpressionType.COMBINED, "(1\u00a6|2\u00a6flickering|3\u00a6trailing|4\u00a6flickering trailing|5\u00a6trailing flickering) %fireworktype% [firework [effect]] colo[u]red %colors%", "(1\u00a6|2\u00a6flickering|3\u00a6trailing|4\u00a6flickering trailing|5\u00a6trailing flickering) %fireworktype% [firework [effect]] colo[u]red %colors% fad(e|ing) [to] %colors%");
    }
}

