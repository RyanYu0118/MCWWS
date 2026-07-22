/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.Leaves
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;

@Name(value="Is Persistent")
@Description(value={"Whether entities, players, or leaves are persistent.", "Persistence of entities is whether they are retained through server restarts.", "Persistence of leaves is whether they should decay when not connected to a log block within 6 meters.", "Persistence of players is if the player's playerdata should be saved when they leave the server. Players' persistence is reset back to 'true' when they join the server.", "Passengers inherit the persistence of their vehicle, meaning a persistent zombie put on a non-persistent chicken will become non-persistent. This does not apply to players.", "By default, all entities are persistent."})
@Example(value="on spawn:\n\tif event-entity is persistent:\n\t\tmake event-entity not persistent\n")
@Since(value={"2.11"})
public class CondIsPersistent
extends PropertyCondition<Object> {
    @Override
    public boolean check(Object object) {
        Block block;
        BlockData blockData;
        if (object instanceof Entity) {
            Entity entity = (Entity)object;
            return entity.isPersistent();
        }
        if (object instanceof Block && (blockData = (block = (Block)object).getBlockData()) instanceof Leaves) {
            Leaves leaves = (Leaves)blockData;
            return leaves.isPersistent();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "persistent";
    }

    static {
        CondIsPersistent.register(CondIsPersistent.class, "persistent", "entities/blocks");
    }
}

