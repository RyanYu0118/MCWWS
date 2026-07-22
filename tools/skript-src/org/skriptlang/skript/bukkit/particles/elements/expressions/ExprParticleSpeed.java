/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.particles.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Particle Speed/Extra Value")
@Description(value={"Determines the specific 'speed' or 'extra' value of a particle.\nThis value is used in different ways depending on the particle, but in general it:\n* acts as the speed at which the particle moves if the particle count is greater than 0.\n* acts as a multiplier to the particle's offset if the particle count is 0.\n\nMore detailed information on particle behavior can be found at <a href=\"https://docs.papermc.io/paper/dev/particles/#count-argument-behavior\">Paper's particle documentation</a>.\n"})
@Example.Examples(value={@Example(value="set the extra value of {_my-flame-particle} to 2"), @Example(value="set the particle speed of {_my-flame-particle} to 0")})
@Since(value={"2.14"})
public class ExprParticleSpeed
extends SimplePropertyExpression<ParticleEffect, Number> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprParticleSpeed.infoBuilder(ExprParticleSpeed.class, Number.class, "(particle speed [value]|extra value)", "particles", false).supplier(ExprParticleSpeed::new)).build());
    }

    @Override
    @Nullable
    public Number convert(ParticleEffect from) {
        return from.extra();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: 
            case RESET: {
                Class[] classArray2 = new Class[1];
                classArray = classArray2;
                classArray2[0] = Number.class;
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
        ParticleEffect[] particleEffect = (ParticleEffect[])this.getExpr().getArray(event);
        if (particleEffect.length == 0) {
            return;
        }
        double extraDelta = delta == null ? 0.0 : ((Number)delta[0]).doubleValue();
        switch (mode) {
            case REMOVE: {
                extraDelta = -extraDelta;
            }
            case ADD: {
                for (ParticleEffect effect : particleEffect) {
                    effect.extra(effect.extra() + extraDelta);
                }
                break;
            }
            case SET: 
            case RESET: {
                for (ParticleEffect effect : particleEffect) {
                    effect.extra(extraDelta);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "particle speed";
    }
}

