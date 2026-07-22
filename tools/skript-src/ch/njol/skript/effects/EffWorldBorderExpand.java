/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.WorldBorder
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import org.bukkit.WorldBorder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Expand/Shrink World Border")
@Description(value={"Expand or shrink the size of a world border.", "Using `by` adds/subtracts from the current size of the world border.", "Using `to` sets to the specified size."})
@Example.Examples(value={@Example(value="expand world border of player by 100 over 5 seconds"), @Example(value="shrink world border of world \"world\" to 100 over 10 seconds")})
@Since(value={"2.11"})
public class EffWorldBorderExpand
extends Effect {
    private boolean shrink;
    private boolean radius;
    private boolean to;
    private Expression<WorldBorder> worldBorders;
    private Expression<Number> numberExpr;
    @Nullable
    private Expression<Timespan> timespan;
    private static final double MAX_WORLDBORDER_SIZE = 5.9999968E7;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.worldBorders = exprs[0];
        this.numberExpr = exprs[1];
        this.timespan = exprs[2];
        this.shrink = matchedPattern > 1;
        this.radius = parseResult.hasTag("radius");
        this.to = parseResult.hasTag("to");
        return true;
    }

    @Override
    protected void execute(Event event) {
        Timespan timespan;
        Number number = this.numberExpr.getSingle(event);
        if (number == null) {
            return;
        }
        double input = number.doubleValue();
        if (Double.isNaN(input)) {
            this.error("You can't " + (this.shrink ? "shrink" : "grow") + " a world border " + (this.to ? "to" : "by") + " NaN.");
            return;
        }
        if (this.radius) {
            input *= 2.0;
        }
        long speed = 0L;
        if (this.timespan != null && (timespan = this.timespan.getSingle(event)) != null) {
            speed = timespan.getAs(Timespan.TimePeriod.SECOND);
        }
        WorldBorder[] worldBorders = this.worldBorders.getArray(event);
        if (this.to) {
            input = Math2.fit(1.0, input, 5.9999968E7);
            for (WorldBorder worldBorder : worldBorders) {
                worldBorder.setSize(input, speed);
            }
        } else {
            if (this.shrink) {
                input = -input;
            }
            for (WorldBorder worldBorder : worldBorders) {
                double size = worldBorder.getSize();
                size = Math2.fit(1.0, size + input, 5.9999968E7);
                worldBorder.setSize(size, speed);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)(this.shrink ? "shrink" : "expand"));
        builder.append((Object)(this.radius ? "radius" : "diameter"));
        builder.append("of", this.worldBorders);
        builder.append((Object)(this.to ? "to" : "by"));
        builder.append((Object)this.numberExpr);
        if (this.timespan != null) {
            builder.append("over", this.timespan);
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffWorldBorderExpand.class, "(expand|grow) [[the] (diameter|:radius) of] %worldborders% (by|:to) %number% [over [a period of] %-timespan%]", "(expand|grow) %worldborders%['s (diameter|:radius)] (by|:to) %number% [over [a period of] %-timespan%]", "(contract|shrink) [[the] (diameter|:radius) of] %worldborders% (by|:to) %number% [over [a period of] %-timespan%]", "(contract|shrink) %worldborders%['s (diameter|:radius)] (by|:to) %number% [over [a period of] %-timespan%]");
    }
}

