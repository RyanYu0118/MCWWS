/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityRegainHealthEvent
 *  org.bukkit.event.entity.EntityRegainHealthEvent$RegainReason
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.Nullable;

public class EvtHealing
extends SkriptEvent {
    @Nullable
    private Literal<EntityData<?>> entityDatas;
    @Nullable
    private Literal<EntityRegainHealthEvent.RegainReason> healReasons;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.entityDatas = args[0];
        this.healReasons = args[1];
        return true;
    }

    @Override
    public boolean check(Event event) {
        boolean result;
        Entity compare;
        if (!(event instanceof EntityRegainHealthEvent)) {
            return false;
        }
        EntityRegainHealthEvent healthEvent = (EntityRegainHealthEvent)event;
        if (this.entityDatas != null) {
            compare = healthEvent.getEntity();
            result = false;
            for (EntityData<?> entityData : this.entityDatas.getAll()) {
                if (!entityData.isInstance(compare)) continue;
                result = true;
                break;
            }
            if (!result) {
                return false;
            }
        }
        if (this.healReasons != null) {
            compare = healthEvent.getRegainReason();
            result = false;
            for (EntityData<?> entityData : this.healReasons.getAll()) {
                if (entityData != compare) continue;
                result = true;
                break;
            }
            if (!result) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "heal" + (String)(this.entityDatas != null ? " of " + this.entityDatas.toString(event, debug) : "") + (String)(this.healReasons != null ? " by " + this.healReasons.toString(event, debug) : "");
    }

    static {
        Skript.registerEvent("Heal", EvtHealing.class, EntityRegainHealthEvent.class, "heal[ing] [of %-entitydatas%] [(from|due to|by) %-healreasons%]", "%entitydatas% heal[ing] [(from|due to|by) %-healreasons%]").description("Called when an entity is healed, e.g. by eating (players), being fed (pets), or by the effect of a potion of healing (overworld mobs) or harm (nether mobs).").examples("on heal:", "on player healing from a regeneration potion:", "on healing of a zombie, cow or a wither:", "\theal reason is healing potion", "\tcancel event").since("1.0, 2.9.0 (by reason)");
    }
}

