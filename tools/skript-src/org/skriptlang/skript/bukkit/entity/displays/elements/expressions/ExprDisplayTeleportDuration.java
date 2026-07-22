/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.displays.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Display;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Display Teleport Duration")
@Description(value={"The teleport duration of displays is the amount of time it takes to get between locations.", "0 means that updates are applied immediately.", "1 means that the display entity will move from current position to the updated one over one tick.", "Higher values spread the movement over multiple ticks. Max of 59 ticks."})
@Example(value="set teleport duration of the last spawned text display to 2 ticks\nteleport last spawned text display to {_location}\nwait 2 ticks\nmessage \"display entity has arrived at %{_location}%\"\n")
@RequiredPlugins(value={"Spigot 1.20.4+"})
@Since(value={"2.10"})
public class ExprDisplayTeleportDuration
extends SimplePropertyExpression<Display, Timespan> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDisplayTeleportDuration.infoBuilder(ExprDisplayTeleportDuration.class, Timespan.class, "teleport[ation] duration[s]", "displays", true).supplier(ExprDisplayTeleportDuration::new)).build());
    }

    @Override
    @Nullable
    public Timespan convert(Display display) {
        return new Timespan(Timespan.TimePeriod.TICK, display.getTeleportDuration());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET -> CollectionUtils.array(Timespan.class);
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Display[] displays = (Display[])this.getExpr().getArray(event);
        long ticks = delta == null ? 0L : ((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        switch (mode) {
            case REMOVE: {
                ticks = -ticks;
            }
            case ADD: {
                for (Display display : displays) {
                    int value = (int)Math2.fit(0L, (long)display.getTeleportDuration() + ticks, 59L);
                    display.setTeleportDuration(value);
                }
                break;
            }
            case SET: 
            case RESET: {
                ticks = Math2.fit(0L, ticks, 59L);
                for (Display display : displays) {
                    display.setTeleportDuration((int)ticks);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "teleport duration";
    }
}

