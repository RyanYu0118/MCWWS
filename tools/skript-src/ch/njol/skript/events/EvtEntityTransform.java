/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityTransformEvent
 *  org.bukkit.event.entity.EntityTransformEvent$TransformReason
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTransformEvent;
import org.jetbrains.annotations.Nullable;

public class EvtEntityTransform
extends SkriptEvent {
    @Nullable
    private Literal<EntityTransformEvent.TransformReason> reasons;
    @Nullable
    private Literal<EntityData<?>> datas;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.datas = args[0];
        this.reasons = args[1];
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof EntityTransformEvent)) {
            return false;
        }
        EntityTransformEvent transformEvent = (EntityTransformEvent)event;
        if (this.reasons != null && !this.reasons.check(event, reason -> transformEvent.getTransformReason().equals(reason))) {
            return false;
        }
        return this.datas == null || this.datas.check(event, data -> data.isInstance(transformEvent.getEntity()));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.datas == null) {
            return "entities transforming" + (String)(this.reasons == null ? "" : " due to " + this.reasons.toString(event, debug));
        }
        return this.datas.toString(event, debug) + " transforming" + (String)(this.reasons == null ? "" : " due to " + this.reasons.toString(event, debug));
    }

    static {
        Skript.registerEvent("Entity Transform", EvtEntityTransform.class, EntityTransformEvent.class, "(entit(y|ies)|%*-entitydatas%) transform[ing] [due to %-transformreasons%]").description("Called when an entity is about to be replaced by another entity.", "Examples when it's called include; when a zombie gets cured and a villager spawns, an entity drowns in water like a zombie that turns to a drown, an entity that gets frozen in powder snow, a mooshroom that when sheared, spawns a new cow.").examples("on a zombie transforming due to curing:", "on mooshroom transforming:", "on zombie, skeleton or slime transform:").keywords("entity transform").since("2.8.0");
    }
}

