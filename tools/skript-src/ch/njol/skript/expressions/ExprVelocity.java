/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.skriptlang.skript.bukkit.particles.particleeffects.DirectionalEffect;

@Name(value="Velocity")
@Description(value={"Gets or changes velocity of an entity or particle.", "Setting the velocity of a particle will remove its random dispersion and force it to be a single particle."})
@Example.Examples(value={@Example(value="set player's velocity to {_v}"), @Example(value="set the velocity of {_particle} to vector(0, 1, 0)"), @Example(value="if the vector length of the player's velocity is greater than 5:\n    send \"You're moving fast!\" to the player\n")})
@Since(value={"2.2-dev31"})
public class ExprVelocity
extends SimplePropertyExpression<Object, Vector> {
    @Override
    @Nullable
    public Vector convert(Object object) {
        DirectionalEffect particleEffect;
        if (object instanceof Entity) {
            Entity entity = (Entity)object;
            return entity.getVelocity();
        }
        if (object instanceof DirectionalEffect && (particleEffect = (DirectionalEffect)object).hasVelocity()) {
            return Vector.fromJOML((Vector3d)particleEffect.velocity());
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Vector.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET || delta != null);
        block14: for (Object object : this.getExpr().getArray(event)) {
            if (object instanceof Entity) {
                Entity entity = (Entity)object;
                switch (mode) {
                    case ADD: {
                        entity.setVelocity(entity.getVelocity().add((Vector)delta[0]));
                        break;
                    }
                    case REMOVE: {
                        entity.setVelocity(entity.getVelocity().subtract((Vector)delta[0]));
                        break;
                    }
                    case REMOVE_ALL: {
                        break;
                    }
                    case RESET: 
                    case DELETE: {
                        entity.setVelocity(new Vector());
                        break;
                    }
                    case SET: {
                        entity.setVelocity((Vector)delta[0]);
                    }
                }
                continue;
            }
            if (!(object instanceof DirectionalEffect)) continue;
            DirectionalEffect particleEffect = (DirectionalEffect)object;
            switch (mode) {
                case ADD: {
                    if (!particleEffect.hasVelocity()) continue block14;
                    particleEffect.velocity(particleEffect.velocity().add((Vector3dc)((Vector)delta[0]).toVector3d()));
                    continue block14;
                }
                case REMOVE: {
                    if (!particleEffect.hasVelocity()) continue block14;
                    particleEffect.velocity(particleEffect.velocity().sub((Vector3dc)((Vector)delta[0]).toVector3d()));
                    continue block14;
                }
                case REMOVE_ALL: {
                    continue block14;
                }
                case RESET: 
                case DELETE: {
                    if (!particleEffect.hasVelocity()) continue block14;
                    particleEffect.velocity(new Vector3d());
                    continue block14;
                }
                case SET: {
                    particleEffect.velocity(((Vector)delta[0]).toVector3d());
                }
            }
        }
    }

    @Override
    protected String getPropertyName() {
        return "velocity";
    }

    @Override
    public Class<Vector> getReturnType() {
        return Vector.class;
    }

    static {
        ExprVelocity.register(ExprVelocity.class, Vector.class, "velocit(y|ies)", "entities/directionalparticles");
    }
}

