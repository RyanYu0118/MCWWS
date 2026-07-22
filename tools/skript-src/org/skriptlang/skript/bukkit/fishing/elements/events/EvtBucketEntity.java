/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerBucketEntityEvent
 *  org.bukkit.event.player.PlayerEvent
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.fishing.elements.events;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.StringUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Bucket Catch Entity")
@Description(value={"Called when a player catches an entity in a bucket."})
@Example(value="on bucket catch of a puffer fish:\n\tsend \"You caught a fish with a %future event-item%!\" to player\n")
@Since(value={"2.10"})
public class EvtBucketEntity
extends SkriptEvent {
    private EntityData<?>[] entities;

    public static void register(SyntaxRegistry registry) {
        registry.register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(EvtBucketEntity.class, "Bucket Catch Entity").addPatterns("bucket (catch[ing]|captur(e|ing)) [[of] %-entitydatas%]")).supplier(EvtBucketEntity::new)).addEvent(PlayerBucketEntityEvent.class).build());
        EventValues.registerEventValue(PlayerBucketEntityEvent.class, ItemStack.class, PlayerBucketEntityEvent::getOriginalBucket);
        EventValues.registerEventValue(PlayerBucketEntityEvent.class, ItemStack.class, PlayerBucketEntityEvent::getEntityBucket, EventValues.TIME_FUTURE);
        EventValues.registerEventValue(PlayerBucketEntityEvent.class, Player.class, PlayerEvent::getPlayer);
        EventValues.registerEventValue(PlayerBucketEntityEvent.class, Entity.class, PlayerBucketEntityEvent::getEntity);
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (args[0] != null) {
            this.entities = (EntityData[])args[0].getAll();
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof PlayerBucketEntityEvent)) {
            return false;
        }
        PlayerBucketEntityEvent bucketEvent = (PlayerBucketEntityEvent)event;
        return this.entities == null || this.entities.length == 0 || Arrays.stream(this.entities).map(EntityData::getType).anyMatch(it -> it.isInstance(bucketEvent.getEntity()));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "bucket catch" + (String)(this.entities.length == 0 ? "" : " of " + StringUtils.join(List.of(this.entities), ", ", " and "));
    }
}

