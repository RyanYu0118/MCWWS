/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.jetbrains.annotations.ApiStatus$Internal
 */
package org.skriptlang.skript.bukkit.particles.particleeffects;

import org.bukkit.Particle;
import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;

public class ConvergingEffect
extends ParticleEffect {
    @ApiStatus.Internal
    public ConvergingEffect(Particle particle) {
        super(particle);
    }

    @Override
    public ConvergingEffect copy() {
        return (ConvergingEffect)super.copy();
    }
}

