/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.entity.EntitySpawnEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.StringUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.jetbrains.annotations.Nullable;

public final class EvtEntity
extends SkriptEvent {
    @Nullable
    private EntityData<?>[] types;
    private boolean spawn;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        block4: {
            this.types = args[0] == null ? null : (EntityData[])args[0].getAll();
            this.spawn = StringUtils.startsWithIgnoreCase(parser.expr, "spawn");
            if (this.types == null) break block4;
            if (this.spawn) {
                for (EntityData<?> d : this.types) {
                    if (!HumanEntity.class.isAssignableFrom(d.getType())) continue;
                    Skript.error("The spawn event does not work for human entities", ErrorQuality.SEMANTIC_ERROR);
                    return false;
                }
            } else {
                for (EntityData<?> d : this.types) {
                    if (LivingEntity.class.isAssignableFrom(d.getType())) continue;
                    Skript.error("The death event only works for living entities", ErrorQuality.SEMANTIC_ERROR);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (this.types == null) {
            return true;
        }
        LivingEntity en = e instanceof EntityDeathEvent ? ((EntityDeathEvent)e).getEntity() : ((EntitySpawnEvent)e).getEntity();
        for (EntityData<?> d : this.types) {
            if (!d.isInstance((Entity)en)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.spawn ? "spawn" : "death") + (String)(this.types != null ? " of " + Classes.toString(this.types, false) : "");
    }

    static {
        Skript.registerEvent("Death", EvtEntity.class, EntityDeathEvent.class, "death [of %-entitydatas%]").description("Called when a living entity (including players) dies.").examples("on death:", "on death of player:", "on death of a wither or ender dragon:", "\tbroadcast \"A %entity% has been slain in %world%!\"").since("1.0");
        Skript.registerEvent("Spawn", EvtEntity.class, EntitySpawnEvent.class, "spawn[ing] [of %-entitydatas%]").description("Called when an entity spawns (excluding players).").examples("on spawn of a zombie:", "on spawn of an ender dragon:", "\tbroadcast \"A dragon has been sighted in %world%!\"").since("1.0, 2.5.1 (non-living entities)");
    }
}

