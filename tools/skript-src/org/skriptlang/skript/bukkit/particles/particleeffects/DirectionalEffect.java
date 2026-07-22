/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.joml.Vector3d
 */
package org.skriptlang.skript.bukkit.particles.particleeffects;

import org.bukkit.Particle;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;

public class DirectionalEffect
extends ParticleEffect {
    @ApiStatus.Internal
    public DirectionalEffect(Particle particle) {
        super(particle);
    }

    public boolean hasVelocity() {
        return this.count() == 0;
    }

    public Vector3d velocity() {
        return this.offset();
    }

    public DirectionalEffect velocity(Vector3d velocity) {
        this.count(0);
        this.offset(velocity);
        return this;
    }

    @Override
    public DirectionalEffect copy() {
        return (DirectionalEffect)super.copy();
    }
}

