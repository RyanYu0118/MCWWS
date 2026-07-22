/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntitySnapshot
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SpawnEggMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Spawn Egg Entity")
@Description(value={"Gets or sets the entity snapshot that the provided spawn eggs will spawn when used."})
@Example.Examples(value={@Example(value="set {_item} to a zombie spawn egg"), @Example(value="broadcast the spawn egg entity of {_item}"), @Example(value="spawn a pig at location(0,0,0):\n\tset the max health of entity to 20\n\tset the health of entity to 20\n\tset {_snapshot} to the entity snapshot of entity\n\tclear entity\nset the spawn egg entity of {_item} to {_snapshot}\n"), @Example(value="if the spawn egg entity of {_item} is {_snapshot}: # Minecraft 1.20.5+\n\tset the spawn egg entity of {_item} to (random element out of all entities)\n"), @Example(value="set the spawn egg entity of {_item} to a zombie")})
@RequiredPlugins(value={"Minecraft 1.20.2+, Minecraft 1.20.5+ (comparisons)"})
@Since(value={"2.10"})
public class ExprSpawnEggEntity
extends SimplePropertyExpression<Object, EntitySnapshot> {
    @Override
    @Nullable
    public EntitySnapshot convert(Object object) {
        ItemMeta itemMeta;
        ItemStack itemStack = ItemUtils.asItemStack(object);
        if (itemStack == null || !((itemMeta = itemStack.getItemMeta()) instanceof SpawnEggMeta)) {
            return null;
        }
        SpawnEggMeta eggMeta = (SpawnEggMeta)itemMeta;
        return eggMeta.getSpawnedEntity();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(EntitySnapshot.class, Entity.class, EntityData.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        EntitySnapshot snapshot = null;
        Object object = delta[0];
        if (object instanceof EntitySnapshot) {
            EntitySnapshot entitySnapshot;
            snapshot = entitySnapshot = (EntitySnapshot)object;
        } else {
            object = delta[0];
            if (object instanceof Entity) {
                Entity entity = (Entity)object;
                snapshot = entity.createSnapshot();
            } else {
                object = delta[0];
                if (object instanceof EntityData) {
                    EntityData entityData = (EntityData)object;
                    Object entity = entityData.create();
                    snapshot = entity.createSnapshot();
                    entity.remove();
                }
            }
        }
        if (snapshot == null) {
            return;
        }
        for (Object object2 : this.getExpr().getArray(event)) {
            ItemMeta itemMeta;
            ItemStack item = ItemUtils.asItemStack(object2);
            if (item == null || !((itemMeta = item.getItemMeta()) instanceof SpawnEggMeta)) continue;
            SpawnEggMeta eggMeta = (SpawnEggMeta)itemMeta;
            eggMeta.setSpawnedEntity(snapshot);
            if (object2 instanceof Slot) {
                Slot slot = (Slot)object2;
                item.setItemMeta((ItemMeta)eggMeta);
                slot.setItem(item);
                continue;
            }
            if (object2 instanceof ItemType) {
                ItemType itemType = (ItemType)object2;
                itemType.setItemMeta((ItemMeta)eggMeta);
                continue;
            }
            if (!(object2 instanceof ItemStack)) continue;
            ItemStack itemStack = (ItemStack)object2;
            itemStack.setItemMeta((ItemMeta)eggMeta);
        }
    }

    @Override
    public Class<EntitySnapshot> getReturnType() {
        return EntitySnapshot.class;
    }

    @Override
    protected String getPropertyName() {
        return "spawn egg entity";
    }

    static {
        if (Skript.classExists("org.bukkit.entity.EntitySnapshot")) {
            ExprSpawnEggEntity.register(ExprSpawnEggEntity.class, EntitySnapshot.class, "spawn egg entity", "itemstacks/itemtypes/slots");
        }
    }
}

