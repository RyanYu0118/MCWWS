/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.vehicle.VehicleBlockCollisionEvent
 *  org.bukkit.event.vehicle.VehicleCollisionEvent
 *  org.bukkit.event.vehicle.VehicleEntityCollisionEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.EventValues;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleCollisionEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.jetbrains.annotations.Nullable;

public class EvtVehicleCollision
extends SkriptEvent {
    private Literal<?> expr;
    private boolean blockCollision;
    private boolean entityCollision;
    private final List<ItemType> itemTypes = new ArrayList<ItemType>();
    private final List<BlockData> blockDatas = new ArrayList<BlockData>();
    private final List<EntityData<?>> entityDatas = new ArrayList();

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (args[0] != null) {
            this.expr = args[0];
            for (Object object : this.expr.getAll()) {
                if (object instanceof ItemType) {
                    ItemType itemType = (ItemType)object;
                    this.itemTypes.add(itemType);
                    continue;
                }
                if (object instanceof BlockData) {
                    BlockData blockData = (BlockData)object;
                    this.blockDatas.add(blockData);
                    continue;
                }
                if (!(object instanceof EntityData)) continue;
                EntityData entityData = (EntityData)object;
                this.entityDatas.add(entityData);
            }
        }
        this.blockCollision = matchedPattern == 1;
        this.entityCollision = matchedPattern == 2;
        return true;
    }

    @Override
    public boolean check(Event event) {
        block8: {
            VehicleCollisionEvent collisionEvent;
            block7: {
                if (!(event instanceof VehicleCollisionEvent)) {
                    return false;
                }
                collisionEvent = (VehicleCollisionEvent)event;
                if (this.expr == null) {
                    if (this.blockCollision && !(event instanceof VehicleBlockCollisionEvent)) {
                        return false;
                    }
                    return !this.entityCollision || event instanceof VehicleEntityCollisionEvent;
                }
                if (!(collisionEvent instanceof VehicleBlockCollisionEvent)) break block7;
                VehicleBlockCollisionEvent blockCollisionEvent = (VehicleBlockCollisionEvent)collisionEvent;
                if (this.itemTypes.isEmpty() && this.blockDatas.isEmpty()) break block7;
                Block eventBlock = blockCollisionEvent.getBlock();
                ItemType eventItemType = new ItemType(eventBlock.getType());
                BlockData blockData = eventBlock.getBlockData();
                for (ItemType itemType : this.itemTypes) {
                    if (!itemType.isSupertypeOf(eventItemType)) continue;
                    return true;
                }
                for (BlockData blockData2 : this.blockDatas) {
                    if (!blockData2.matches(blockData)) continue;
                    return true;
                }
                break block8;
            }
            if (!(collisionEvent instanceof VehicleEntityCollisionEvent)) break block8;
            VehicleEntityCollisionEvent entityCollisionEvent = (VehicleEntityCollisionEvent)collisionEvent;
            if (!this.entityDatas.isEmpty()) {
                EntityData<Entity> eventEntityData = EntityData.fromEntity(entityCollisionEvent.getEntity());
                for (EntityData<Entity> entityData : this.entityDatas) {
                    if (!entityData.isSupertypeOf(eventEntityData)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"vehicle");
        if (this.blockCollision) {
            builder.append((Object)"block");
        } else if (this.entityCollision) {
            builder.append((Object)"entity");
        }
        builder.append((Object)"collision");
        if (this.expr != null) {
            builder.append("of", this.expr);
        }
        return builder.toString();
    }

    static {
        Skript.registerEvent("Vehicle Collision", EvtVehicleCollision.class, new Class[]{VehicleBlockCollisionEvent.class, VehicleEntityCollisionEvent.class}, "vehicle collision [(with|of) [a[n]] %-itemtypes/blockdatas/entitydatas%]", "vehicle block collision [(with|of) [a[n]] %-itemtypes/blockdatas%]", "vehicle entity collision [(with|of) [a[n]] %-entitydatas%]").description("Called when a vehicle collides with a block or entity.").examples("on vehicle collision:", "on vehicle collision with obsidian:", "on vehicle collision with a zombie:").since("2.10");
        EventValues.registerEventValue(VehicleBlockCollisionEvent.class, Block.class, VehicleBlockCollisionEvent::getBlock);
        EventValues.registerEventValue(VehicleEntityCollisionEvent.class, Entity.class, VehicleEntityCollisionEvent::getEntity);
    }
}

