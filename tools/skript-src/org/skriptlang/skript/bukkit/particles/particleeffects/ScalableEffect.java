/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Function
 *  org.bukkit.Particle
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.bukkit.particles.particleeffects;

import com.google.common.base.Function;
import org.bukkit.Particle;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;

public class ScalableEffect
extends ParticleEffect {
    private double scale = 1.0;
    private final ScalingFunction scalingFunction;

    private ScalingFunction getScalingFunction(@NotNull Particle particle) {
        return switch (particle) {
            case Particle.SWEEP_ATTACK -> ScalingFunction.SWEEP;
            case Particle.EXPLOSION -> ScalingFunction.EXPLOSION;
            default -> throw new IllegalArgumentException("Particle " + particle.name() + " is not a scalable effect.");
        };
    }

    @ApiStatus.Internal
    public ScalableEffect(Particle particle) {
        super(particle);
        this.scalingFunction = this.getScalingFunction(particle);
    }

    public boolean hasScale() {
        return this.count() == 0;
    }

    public ParticleEffect scale(double scale) {
        this.scale = scale;
        this.count(0);
        this.offset(this.scalingFunction.apply(scale), 0.0, 0.0);
        return this;
    }

    public double scale() {
        return this.scale;
    }

    @Override
    public ScalableEffect copy() {
        ScalableEffect copy = (ScalableEffect)super.copy();
        copy.scale = this.scale;
        return copy;
    }

    private static enum ScalingFunction {
        SWEEP((Function<Double, Double>)((Function)scale -> 2.0 - 2.0 * scale)),
        EXPLOSION((Function<Double, Double>)((Function)scale -> 2.0 - scale));

        private final Function<Double, Double> scaleToOffsetX;

        private ScalingFunction(Function<Double, Double> scalingFunction) {
            this.scaleToOffsetX = scalingFunction;
        }

        public double apply(double scale) {
            return (Double)this.scaleToOffsetX.apply((Object)scale);
        }
    }
}

