/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityShootBowEvent
 *  org.bukkit.projectiles.ProjectileSource
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.sections.EffSecShoot;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

@Name(value="Shooter")
@Description(value={"The shooter of a projectile."})
@Example(value="shooter is a skeleton")
@Since(value={"1.3.7, 2.11 (entity shoot bow event)"})
public class ExprShooter
extends PropertyExpression<Projectile, LivingEntity> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected LivingEntity @Nullable [] get(Event event, Projectile[] source) {
        if (event instanceof EffSecShoot.ShootEvent) {
            EffSecShoot.ShootEvent shootEvent = (EffSecShoot.ShootEvent)event;
            if (this.getExpr().isDefault() && !(shootEvent.getProjectile() instanceof Projectile)) {
                return new LivingEntity[]{shootEvent.getShooter()};
            }
        }
        if (event instanceof EntityShootBowEvent) {
            EntityShootBowEvent shootBowEvent = (EntityShootBowEvent)event;
            if (this.getExpr().isDefault()) {
                return new LivingEntity[]{shootBowEvent.getEntity()};
            }
        }
        return this.get(source, projectile -> {
            ProjectileSource shooter;
            ProjectileSource projectileSource = shooter = projectile != null ? projectile.getShooter() : null;
            if (shooter instanceof LivingEntity) {
                LivingEntity livingShooter = (LivingEntity)shooter;
                return livingShooter;
            }
            return null;
        });
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return new Class[]{LivingEntity.class};
        }
        return super.acceptChange(mode);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            assert (delta != null);
            ProjectileSource source = (ProjectileSource)delta[0];
            for (Projectile projectile : (Projectile[])this.getExpr().getArray(event)) {
                assert (projectile != null) : this.getExpr();
                projectile.setShooter(source);
            }
        } else {
            super.change(event, delta, mode);
        }
    }

    @Override
    public Class<LivingEntity> getReturnType() {
        return LivingEntity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the shooter" + (String)(this.getExpr().isDefault() ? "" : " of " + this.getExpr().toString(event, debug));
    }

    static {
        Skript.registerExpression(ExprShooter.class, LivingEntity.class, ExpressionType.SIMPLE, "[the] shooter [of %projectile%]");
    }
}

