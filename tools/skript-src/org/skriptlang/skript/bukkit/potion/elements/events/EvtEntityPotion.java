/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityPotionEffectEvent
 *  org.bukkit.event.entity.EntityPotionEffectEvent$Action
 *  org.bukkit.event.entity.EntityPotionEffectEvent$Cause
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.events;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.EventValues;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EvtEntityPotion
extends SkriptEvent {
    private Literal<EntityPotionEffectEvent.Action> actions;
    private Expression<PotionEffectType> types;
    private Literal<EntityPotionEffectEvent.Cause> causes;

    public static void register(SyntaxRegistry registry) {
        registry.register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(EvtEntityPotion.class, "Entity Potion Effect").supplier(EvtEntityPotion::new)).addEvent(EntityPotionEffectEvent.class).addPattern("entity potion effect [modif[y|ication]] [[of] %-potioneffecttypes%] [%-potionactions%] [due to %-potioncauses%]")).addDescription("Called when an entity's potion effect is modified.").addExample("on entity potion effect modification:\n\tbroadcast \"A potion effect was added to %event-entity%!\"\n").addExample("on entity potion effect of night vision added:\n\tmessage \"You can now see in the dark!\"\n").addExample("on entity potion effect of strength removed:\n\tmessage \"You're now weaker!\"\n").addSince("2.10", "2.14 (action support)").build());
        EventValues.registerEventValue(EntityPotionEffectEvent.class, PotionEffect.class, EntityPotionEffectEvent::getOldEffect, EventValues.TIME_PAST);
        EventValues.registerEventValue(EntityPotionEffectEvent.class, PotionEffect.class, EntityPotionEffectEvent::getNewEffect);
        EventValues.registerEventValue(EntityPotionEffectEvent.class, PotionEffectType.class, EntityPotionEffectEvent::getModifiedType);
        EventValues.registerEventValue(EntityPotionEffectEvent.class, EntityPotionEffectEvent.Cause.class, EntityPotionEffectEvent::getCause);
        EventValues.registerEventValue(EntityPotionEffectEvent.class, EntityPotionEffectEvent.Action.class, EntityPotionEffectEvent::getAction);
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.types = args[0];
        this.actions = args[1];
        this.causes = args[2];
        return true;
    }

    @Override
    public boolean check(Event event) {
        EntityPotionEffectEvent potionEvent = (EntityPotionEffectEvent)event;
        if (this.actions != null && Arrays.stream(this.actions.getAll()).noneMatch(action -> action == potionEvent.getAction())) {
            return false;
        }
        if (this.types != null) {
            PotionEffectType newType;
            PotionEffectType oldType = potionEvent.getOldEffect() != null ? potionEvent.getOldEffect().getType() : null;
            PotionEffectType potionEffectType = newType = potionEvent.getNewEffect() != null ? potionEvent.getNewEffect().getType() : null;
            if (!this.types.check(event, type -> type.equals(oldType) || type.equals(newType))) {
                return false;
            }
        }
        return this.causes == null || Arrays.stream(this.causes.getAll()).anyMatch(cause -> cause == potionEvent.getCause());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"on entity potion effect modification");
        if (this.types != null) {
            builder.append("of", this.types);
        }
        if (this.actions != null) {
            builder.append((Object)this.actions);
        }
        if (this.causes != null) {
            builder.append("due to", this.causes);
        }
        return builder.toString();
    }
}

