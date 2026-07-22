/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package org.skriptlang.skript.bukkit.particles.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Particle Offset")
@Description(value={"Determines the offset value for a particle.\nOffsets are treated as distributions if particle count is greater than 0.\nOffsets are treated as velocity or some other special behavior if particle count is 0.\nSetting distribution/velocity with this method may change the particle count to 1/0 respectively.\n\nMore detailed information on particle behavior can be found at <a href=\"https://docs.papermc.io/paper/dev/particles/#count-argument-behavior\">Paper's particle documentation</a>.\n"})
@Example(value="set the particle offset of {_my-particle} to vector(1, 2, 1)")
@Since(value={"2.14"})
public class ExprParticleOffset
extends SimplePropertyExpression<ParticleEffect, Vector> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprParticleOffset.infoBuilder(ExprParticleOffset.class, Vector.class, "particle offset", "particles", false).supplier(ExprParticleOffset::new)).build());
    }

    @Override
    @Nullable
    public Vector convert(ParticleEffect from) {
        return Vector.fromJOML((Vector3d)from.offset());
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
                classArray2[0] = Vector.class;
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
        Vector3d vectorDelta = delta == null ? new Vector3d() : ((Vector)delta[0]).toVector3d();
        switch (mode) {
            case REMOVE: {
                vectorDelta.mul(-1.0);
            }
            case ADD: {
                for (ParticleEffect effect : particleEffect) {
                    effect.offset(vectorDelta.add((Vector3dc)effect.offset()));
                }
                break;
            }
            case SET: 
            case RESET: {
                for (ParticleEffect effect : particleEffect) {
                    effect.offset(vectorDelta);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Vector> getReturnType() {
        return Vector.class;
    }

    @Override
    protected String getPropertyName() {
        return "particle offset";
    }
}

