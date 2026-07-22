/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.PrePlayerAttackEntityEvent
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.vehicle.VehicleDamageEvent
 *  org.bukkit.event.vehicle.VehicleDestroyEvent
 *  org.bukkit.projectiles.ProjectileSource
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

@Name(value="Attacker")
@Description(value={"The attacker of a damage event, e.g. when a player attacks a zombie this expression represents the player.\",\nPlease note that the attacker can also be a block, e.g. a cactus or lava, but this expression will not be set in these cases.\n"})
@Example(value="on damage:\n\tattacker is a player\n\thealth of attacker is less than or equal to 2\n\tdamage victim by 1 heart\n")
@Since(value={"1.3"})
@Events(value={"damage", "death", "vehicle destroy", "attempt attack"})
public class ExprAttacker
extends SimpleExpression<Entity>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityDamageEvent.class, EntityDeathEvent.class, VehicleDamageEvent.class, VehicleDestroyEvent.class, PrePlayerAttackEntityEvent.class);
    }

    protected Entity[] get(Event e) {
        return new Entity[]{ExprAttacker.getAttacker(e)};
    }

    @Nullable
    static Entity getAttacker(@Nullable Event event) {
        if (event == null) {
            return null;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent)event;
            Entity damager = damageEvent.getDamager();
            if (damager instanceof Projectile) {
                Projectile projectile = (Projectile)damager;
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Entity) {
                    Entity shooterEntity = (Entity)shooter;
                    return shooterEntity;
                }
                return null;
            }
            return damager;
        }
        if (event instanceof EntityDeathEvent) {
            EntityDeathEvent deathEvent = (EntityDeathEvent)event;
            return ExprAttacker.getAttacker((Event)deathEvent.getEntity().getLastDamageCause());
        }
        if (event instanceof VehicleDamageEvent) {
            VehicleDamageEvent vehicleDamageEvent = (VehicleDamageEvent)event;
            return vehicleDamageEvent.getAttacker();
        }
        if (event instanceof VehicleDestroyEvent) {
            VehicleDestroyEvent vehicleDestroyEvent = (VehicleDestroyEvent)event;
            return vehicleDestroyEvent.getAttacker();
        }
        if (event instanceof PrePlayerAttackEntityEvent) {
            PrePlayerAttackEntityEvent preAttackEvent = (PrePlayerAttackEntityEvent)event;
            return preAttackEvent.getPlayer();
        }
        return null;
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return Entity.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (e == null) {
            return "the attacker";
        }
        return Classes.getDebugMessage(this.getSingle(e));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    static {
        Skript.registerExpression(ExprAttacker.class, Entity.class, ExpressionType.SIMPLE, "[the] (attacker|damager)");
    }
}

