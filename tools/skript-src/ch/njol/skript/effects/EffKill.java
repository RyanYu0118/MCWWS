/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Damageable
 *  org.bukkit.entity.EnderDragonPart
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.DamageUtils;
import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Kill")
@Description(value={"Kills an entity."})
@Example.Examples(value={@Example(value="kill the player"), @Example(value="kill all creepers in the player's world"), @Example(value="kill all endermen, witches and bats")})
@Since(value={"1.0, 2.10 (ignoring totem of undying)"})
public class EffKill
extends Effect {
    private static final boolean SUPPORTS_DAMAGE_SOURCE = Skript.classExists("org.bukkit.damage.DamageSource");
    private Expression<Entity> entities;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.entities = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Entity entity : this.entities.getArray(event)) {
            if (entity instanceof EnderDragonPart) {
                EnderDragonPart part = (EnderDragonPart)entity;
                entity = part.getParent();
            }
            if (entity instanceof Damageable) {
                Damageable damageable = (Damageable)entity;
                if (SUPPORTS_DAMAGE_SOURCE) {
                    EntityDamageEvent.DamageCause cause = EntityDamageEvent.DamageCause.KILL;
                    HealthUtils.damage(damageable, Double.POSITIVE_INFINITY, DamageUtils.getDamageSourceFromCause(cause));
                } else {
                    HealthUtils.setHealth(damageable, 0.0);
                    HealthUtils.damage(damageable, 1.0);
                }
            }
            if (!entity.isValid() || entity instanceof Player) continue;
            entity.remove();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "kill " + this.entities.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffKill.class, "kill %entities%");
    }
}

