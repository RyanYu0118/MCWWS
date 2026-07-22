/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Damageable
 *  org.bukkit.entity.EnderDragon
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

public class EvtDamage
extends SkriptEvent {
    @Nullable
    private Literal<EntityData<?>> ofTypes;
    @Nullable
    private Literal<EntityData<?>> byTypes;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.ofTypes = args[0];
        this.byTypes = args[1];
        return true;
    }

    @Override
    public boolean check(Event evt) {
        EntityDamageByEntityEvent event;
        EntityDamageEvent e = (EntityDamageEvent)evt;
        if (evt instanceof EntityDamageByEntityEvent ? !this.checkDamager((event = (EntityDamageByEntityEvent)evt).getDamager()) : this.byTypes != null) {
            return false;
        }
        if (!this.checkDamaged(e.getEntity())) {
            return false;
        }
        if (e instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent)e).getDamager() instanceof EnderDragon && ((EntityDamageByEntityEvent)e).getEntity() instanceof EnderDragon) {
            return false;
        }
        return EvtDamage.checkDamage(e);
    }

    private boolean checkDamager(Entity e) {
        if (this.byTypes != null) {
            for (EntityData<?> d : this.byTypes.getAll()) {
                if (!d.isInstance(e)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean checkDamaged(Entity e) {
        if (this.ofTypes != null) {
            for (EntityData<?> d : this.ofTypes.getAll()) {
                if (!d.isInstance(e)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "damage" + (String)(this.ofTypes != null ? " of " + this.ofTypes.toString(e, debug) : "") + (String)(this.byTypes != null ? " by " + this.byTypes.toString(e, debug) : "");
    }

    private static boolean checkDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) {
            return true;
        }
        LivingEntity en = (LivingEntity)e.getEntity();
        return !(HealthUtils.getHealth((Damageable)en) <= 0.0);
    }

    static {
        Skript.registerEvent("Damage", EvtDamage.class, EntityDamageEvent.class, "damag(e|ing) [of %-entitydata%] [by %-entitydata%]").description("Called when an entity receives damage, e.g. by an attack from another entity, lava, fire, drowning, fall, suffocation, etc.").examples("on damage:", "on damage of a player:", "on damage of player by zombie:").since("1.0, 2.7 (by entity)");
    }
}

