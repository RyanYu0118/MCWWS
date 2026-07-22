/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Beehive
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.EntityBlockStorage
 *  org.bukkit.entity.Bee
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EntityBlockStorage;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Insert Entity Storage")
@Description(value={"Add an entity into the entity storage of a block (e.g. beehive).", "The entity must be of the right type for the block (e.g. bee for beehive).", "Due to unstable behavior on older versions, adding entities to an entity storage requires Minecraft version 1.21+."})
@Example(value="add last spawned bee into the entity storage of {_beehive}")
@RequiredPlugins(value={"Minecraft 1.21+"})
@Since(value={"2.11"})
public class EffInsertEntityStorage
extends Effect {
    private static final Map<Class<? extends BlockState>, Class<? extends Entity>> STORAGES = new HashMap<Class<? extends BlockState>, Class<? extends Entity>>();
    private Expression<? extends Entity> entities;
    private Expression<Block> block;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.block = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        BlockState blockState;
        Block block = this.block.getSingle(event);
        if (block == null || !((blockState = block.getState()) instanceof EntityBlockStorage)) {
            return;
        }
        EntityBlockStorage blockStorage = (EntityBlockStorage)blockState;
        Class<? extends Entity> entityClass = this.getEntityClass((BlockState)blockStorage);
        if (entityClass == null) {
            return;
        }
        this.addEntities(entityClass, (BlockState)blockStorage, this.entities.getArray(event));
    }

    private <T extends EntityBlockStorage<R>, R extends Entity> void addEntities(Class<R> entityClass, BlockState blockState, Entity[] entities) {
        EntityBlockStorage typedStorage = (EntityBlockStorage)blockState;
        for (Entity entity : entities) {
            if (!entityClass.isInstance(entity)) continue;
            if (typedStorage.getEntityCount() >= typedStorage.getMaxEntities()) break;
            Entity typedEntity = entity;
            typedStorage.addEntity(typedEntity);
        }
        typedStorage.update(true, false);
    }

    @Nullable
    private Class<? extends Entity> getEntityClass(BlockState blockState) {
        for (Class<? extends BlockState> stateClass : STORAGES.keySet()) {
            if (!stateClass.isInstance(blockState)) continue;
            return STORAGES.get(stateClass);
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "add " + this.entities.toString(event, debug) + " into the entity storage of " + this.block.toString(event, debug);
    }

    static {
        if (Skript.isRunningMinecraft(1, 21, 0)) {
            Skript.registerEffect(EffInsertEntityStorage.class, "(add|insert) %livingentities% [in[ ]]to [the] (stored entities|entity storage) of %block%");
            STORAGES.put(Beehive.class, Bee.class);
        }
    }
}

