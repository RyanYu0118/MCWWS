/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.WorldBorder
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.WorldBorder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Warning Time of World Border")
@Description(value={"The warning time of a world border. If the border is shrinking, the player's screen will be tinted red once the border will catch the player within this time period."})
@Example(value="set world border warning time of {_worldborder} to 1 second")
@Since(value={"2.11"})
public class ExprWorldBorderWarningTime
extends SimplePropertyExpression<WorldBorder, Timespan> {
    @Override
    @Nullable
    public Timespan convert(WorldBorder worldBorder) {
        return new Timespan(Timespan.TimePeriod.SECOND, worldBorder.getWarningTime());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        long input = delta == null ? 15L : ((Timespan)delta[0]).getAs(Timespan.TimePeriod.SECOND);
        for (WorldBorder worldBorder : (WorldBorder[])this.getExpr().getArray(event)) {
            long warningTime = switch (mode) {
                case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> input;
                case Changer.ChangeMode.ADD -> Math2.addClamped(worldBorder.getWarningTime(), input);
                case Changer.ChangeMode.REMOVE -> Math2.addClamped(worldBorder.getWarningTime(), -input);
                default -> throw new IllegalStateException();
            };
            ExprWorldBorderWarningTime.setWarningTime(worldBorder, warningTime);
        }
    }

    private static void setWarningTime(WorldBorder worldBorder, long inputTime) {
        long time = Math2.multiplyClamped(inputTime, 20L);
        int warningTime = (int)Math2.fit(0L, time, Integer.MAX_VALUE) / 20;
        worldBorder.setWarningTime(warningTime);
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "world border warning time";
    }

    static {
        ExprWorldBorderWarningTime.registerDefault(ExprWorldBorderWarningTime.class, Timespan.class, "world[ ]border warning time", "worldborders");
    }
}

