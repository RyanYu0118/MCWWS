/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.PrePlayerAttackEntityEvent
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EvtAttemptAttack
extends SkriptEvent {
    private EntityData<?> @Nullable [] types;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.types = args.length == 0 ? null : (EntityData[])args[0].getAll();
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.types == null) {
            return true;
        }
        if (!(event instanceof PrePlayerAttackEntityEvent)) {
            return false;
        }
        PrePlayerAttackEntityEvent preEvent = (PrePlayerAttackEntityEvent)event;
        Entity entity = preEvent.getAttacked();
        for (EntityData<?> data : this.types) {
            if (!data.isInstance(entity)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.types == null) {
            builder.append((Object)"attack attempt");
        } else {
            builder.append("attempting to attack", Classes.toString(this.types, debug ? StringMode.DEBUG : StringMode.MESSAGE));
        }
        return builder.toString();
    }

    static {
        Skript.registerEvent("Attempt Attack", EvtAttemptAttack.class, PrePlayerAttackEntityEvent.class, "attack attempt", "attempt[ing] to attack %entitydatas%").description("Called when a player attempts to attack an entity.\nThe event will be cancelled as soon as it is fired for non-living entities.\nCancelling this event will prevent the attack and any sounds from being played when attacking.\nAny damage events will not be called if this is cancelled.\n").examples("on attack attempt:\n    if event is cancelled:\n        broadcast \"%attacker% failed to attack %victim%!\"\n    else:\n        broadcast \"%attacker% damaged %victim%!\"\n", "on attempt to attack an animal:\n    cancel event\n", "on attempting to attack an entity:\n    if victim is a creeper:\n        cancel event\n", "on attempt to attack a zombie or creeper:\n    attacker isn't holding a diamond sword\n    cancel event\n").since("2.15");
    }
}

