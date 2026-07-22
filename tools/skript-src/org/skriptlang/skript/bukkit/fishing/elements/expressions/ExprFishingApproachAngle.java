/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.FishHook
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.fishing.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.entity.FishHook;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Fishing Approach Angle")
@Description(value={"Returns the angle at which the fish will approach the fishing hook, after the wait time.", "The angle is in degrees, with 0 being positive Z, 90 being negative X, 180 being negative Z, and 270 being positive X.", "By default, returns a value between 0 and 360 degrees."})
@Example(value="on fish approach:\n\tif any:\n\t\tmaximum fishing approach angle is bigger than 300.5 degrees\n\t\tmin fishing approach angle is smaller than 59.5 degrees\n\tthen:\n\t\tcancel event\n")
@Events(value={"Fishing"})
@Since(value={"2.10"})
public class ExprFishingApproachAngle
extends SimpleExpression<Float> {
    private static final float DEFAULT_MINIMUM_DEGREES = 0.0f;
    private static final float DEFAULT_MAXIMUM_DEGREES = 360.0f;
    private boolean isMin;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprFishingApproachAngle.class, Float.class).addPatterns("(min:min[imum]|max[imum]) fish[ing] approach[ing] angle")).supplier(ExprFishingApproachAngle::new)).priority(EventValueExpression.DEFAULT_PRIORITY)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerFishEvent.class)) {
            Skript.error("The 'fishing approach angle' expression can only be used in a fishing event.");
            return false;
        }
        this.isMin = parseResult.hasTag("min");
        return true;
    }

    protected Float @Nullable [] get(Event event) {
        if (!(event instanceof PlayerFishEvent)) {
            return null;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        if (this.isMin) {
            return new Float[]{Float.valueOf(fishEvent.getHook().getMinLureAngle())};
        }
        return new Float[]{Float.valueOf(fishEvent.getHook().getMaxLureAngle())};
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        switch (mode) {
            case SET: 
            case RESET: 
            case ADD: 
            case REMOVE: {
                Class[] classArray2 = new Class[1];
                classArray = classArray2;
                classArray2[0] = Float.class;
                break;
            }
            default: {
                classArray = null;
            }
        }
        return classArray;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof PlayerFishEvent)) {
            return;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        FishHook hook = fishEvent.getHook();
        float angle = mode == Changer.ChangeMode.RESET ? (this.isMin ? 0.0f : 360.0f) : ((Float)delta[0]).floatValue();
        switch (mode) {
            case SET: 
            case RESET: {
                if (this.isMin) {
                    hook.setMinLureAngle(this.clamp(angle));
                    break;
                }
                hook.setMaxLureAngle(this.clamp(angle));
                break;
            }
            case ADD: {
                if (this.isMin) {
                    hook.setMinLureAngle(this.clamp(hook.getMinLureAngle() + angle));
                    break;
                }
                hook.setMaxLureAngle(this.clamp(hook.getMaxLureAngle() + angle));
                break;
            }
            case REMOVE: {
                if (this.isMin) {
                    hook.setMinLureAngle(this.clamp(hook.getMinLureAngle() - angle));
                    break;
                }
                hook.setMaxLureAngle(this.clamp(hook.getMaxLureAngle() - angle));
            }
        }
    }

    private float clamp(float value) {
        return Math.min(Math.max(value, 0.0f), 360.0f);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Float> getReturnType() {
        return Float.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.isMin ? "minimum" : "maximum") + " fishing approach angle";
    }
}

