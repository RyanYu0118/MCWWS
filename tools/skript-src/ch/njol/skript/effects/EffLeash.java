/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Leash entities")
@Description(value={"Leash living entities to other entities. When trying to leash an Ender Dragon, Wither, Player, or a Bat, this effect will not work.", "See <a href=\"https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/LivingEntity.html#setLeashHolder(org.bukkit.entity.Entity)\">Spigot's Javadocs for more info</a>."})
@Example(value="on right click:\n\tleash event-entity to player\n\tsend \"&aYou leashed &2%event-entity%!\" to player\n")
@Since(value={"2.3"})
public class EffLeash
extends Effect {
    private Expression<Entity> holder;
    private Expression<LivingEntity> targets;
    private boolean leash;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean bl = this.leash = matchedPattern != 2;
        if (this.leash) {
            this.holder = exprs[1 - matchedPattern];
            this.targets = exprs[matchedPattern];
        } else {
            this.targets = exprs[0];
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        if (this.leash) {
            Entity holder = this.holder.getSingle(e);
            if (holder == null) {
                return;
            }
            for (LivingEntity target : this.targets.getArray(e)) {
                target.setLeashHolder(holder);
            }
        } else {
            for (LivingEntity target : this.targets.getArray(e)) {
                target.setLeashHolder(null);
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (this.leash) {
            return "leash " + this.targets.toString(e, debug) + " to " + this.holder.toString(e, debug);
        }
        return "unleash " + this.targets.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffLeash.class, "(leash|lead) %livingentities% to %entity%", "make %entity% (leash|lead) %livingentities%", "un(leash|lead) [holder of] %livingentities%");
    }
}

