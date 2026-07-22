/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.particles.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Particle with Speed/Extra Value")
@Description(value={"Applies a specific 'speed' or 'extra' value to a particle.\nThis value is used in different ways depending on the particle, but in general it:\n* acts as the speed at which the particle moves if the particle count is greater than 0.\n* acts as a multiplier to the particle's offset if the particle count is 0.\n\nMore detailed information on particle behavior can be found at <a href=\"https://docs.papermc.io/paper/dev/particles/#count-argument-behavior\">Paper's particle documentation</a>.\n"})
@Example.Examples(value={@Example(value="draw an electric spark particle with a particle speed of 0 at player"), @Example(value="draw 12 red dust particles with an extra value of 0.4 at player's head location")})
@Since(value={"2.14"})
public class ExprParticleWithSpeed
extends PropertyExpression<ParticleEffect, ParticleEffect> {
    private Expression<Number> speed;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprParticleWithSpeed.class, ParticleEffect.class).addPatterns("%particles% with ([a] particle speed [value]|[an] extra value) [of] %number%")).supplier(ExprParticleWithSpeed::new)).priority(SyntaxInfo.COMBINED)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(expressions[0]);
        this.speed = expressions[1];
        return true;
    }

    protected ParticleEffect[] get(Event event, ParticleEffect[] source) {
        Number speed = this.speed.getSingle(event);
        if (speed == null) {
            return new ParticleEffect[0];
        }
        double speedValue = speed.doubleValue();
        ParticleEffect[] results = (ParticleEffect[])Array.newInstance(this.getExpr().getReturnType(), source.length);
        for (int i = 0; i < source.length; ++i) {
            results[i] = source[i].copy().extra(speedValue);
        }
        return results;
    }

    @Override
    public Class<? extends ParticleEffect> getReturnType() {
        return this.getExpr().getReturnType();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return new SyntaxStringBuilder(event, debug).append(this.getExpr(), "with a speed value of", this.speed).toString();
    }
}

