/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityShootBowEvent
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventConverter;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.slot.EquipmentSlot;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EvtEntityShootBow
extends SkriptEvent {
    private Literal<EntityData<?>> entityDatas;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        LiteralList list;
        this.entityDatas = args[0];
        Literal<EntityData<?>> literal = this.entityDatas;
        if (literal instanceof LiteralList && (list = (LiteralList)literal).getAnd()) {
            list.invertAnd();
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof EntityShootBowEvent)) {
            return false;
        }
        EntityShootBowEvent shootBowEvent = (EntityShootBowEvent)event;
        LivingEntity eventEntity = shootBowEvent.getEntity();
        return this.entityDatas.check(event, entityData -> entityData.isInstance((Entity)eventEntity));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.entityDatas.toString(event, debug) + " shoot bow";
    }

    static {
        Skript.registerEvent("Entity Shoot Bow", EvtEntityShootBow.class, EntityShootBowEvent.class, "%entitydatas% shoot[ing] (bow|projectile)").description("Called when an entity shoots a bow.\nevent-entity refers to the shot projectile/entity.\n").examples("on player shoot bow:\n\tchance of 30%:\n\t\tdamage event-slot by 10\n\t\tsend \"Your bow has taken increased damage!\" to shooter\n\non stray shooting bow:\n\tset {_e} to event-entity\n\tspawn a cow at {_e}:\n\t\tset velocity of entity to velocity of {_e}\n\tset event-entity to last spawned entity\n").since("2.11");
        EventValues.registerEventValue(EntityShootBowEvent.class, ItemStack.class, EntityShootBowEvent::getBow);
        EventValues.registerEventValue(EntityShootBowEvent.class, Entity.class, new EventConverter<EntityShootBowEvent, Entity>(){

            @Override
            public void set(EntityShootBowEvent event, @Nullable Entity entity) {
                if (entity == null) {
                    return;
                }
                event.setProjectile(entity);
            }

            @Override
            @NotNull
            public Entity convert(EntityShootBowEvent from) {
                return from.getProjectile();
            }
        });
        EventValues.registerEventValue(EntityShootBowEvent.class, Slot.class, event -> {
            EntityEquipment equipment = event.getEntity().getEquipment();
            if (equipment == null) {
                return null;
            }
            return new EquipmentSlot(equipment, event.getHand());
        });
    }
}

