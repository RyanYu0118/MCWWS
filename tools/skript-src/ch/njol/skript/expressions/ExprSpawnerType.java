/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.CreatureSpawner
 *  org.bukkit.block.TrialSpawner
 *  org.bukkit.entity.EntityType
 *  org.bukkit.event.Event
 *  org.bukkit.spawner.TrialSpawnerConfiguration
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TrialSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.spawner.TrialSpawnerConfiguration;
import org.jetbrains.annotations.Nullable;

@Name(value="Spawner Type")
@Description(value={"The entity type of a spawner (mob spawner).\nChange the entity type, reset it (pig) or clear it (Minecraft 1.20.0+).\n"})
@Example.Examples(value={@Example(value="on right click:\n\tif event-block is a spawner:\n\t\tsend \"Spawner's type if %spawner type of event-block%\" to player\n"), @Example(value="set the creature type of {_spawner} to a trader llama"), @Example(value="reset {_spawner}'s entity type # Pig"), @Example(value="clear the spawner type of {_spawner} # Minecraft 1.20.0+")})
@Since(value={"2.4, 2.9.2 (trial spawner), 2.12 (delete)"})
@RequiredPlugins(value={"Minecraft 1.20.0+ (delete)"})
public class ExprSpawnerType
extends SimplePropertyExpression<Block, EntityData> {
    private static final boolean HAS_TRIAL_SPAWNER = Skript.classExists("org.bukkit.block.TrialSpawner");
    private static final boolean RUNNING_1_20_0 = Skript.isRunningMinecraft(1, 20, 0);

    @Override
    @Nullable
    public EntityData convert(Block block) {
        BlockState type;
        BlockState blockState = block.getState();
        if (blockState instanceof CreatureSpawner) {
            CreatureSpawner creatureSpawner = (CreatureSpawner)blockState;
            type = creatureSpawner.getSpawnedType();
            if (type == null) {
                return null;
            }
            return EntityUtils.toSkriptEntityData((EntityType)type);
        }
        if (HAS_TRIAL_SPAWNER && (type = block.getState()) instanceof TrialSpawner) {
            TrialSpawner trialSpawner = (TrialSpawner)type;
            if ((type = trialSpawner.isOminous() ? trialSpawner.getOminousConfiguration().getSpawnedType() : trialSpawner.getNormalConfiguration().getSpawnedType()) == null) {
                return null;
            }
            return EntityUtils.toSkriptEntityData((EntityType)type);
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
            return CollectionUtils.array(EntityData.class);
        }
        if (mode == Changer.ChangeMode.DELETE) {
            if (RUNNING_1_20_0) {
                return CollectionUtils.array(EntityData.class);
            }
            Skript.error("You can only delete the spawner type of a spawner on Minecraft 1.20.0 or newer");
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        EntityType entityType = null;
        if (delta != null) {
            entityType = EntityUtils.toBukkitEntityType((EntityData)delta[0]);
        } else if (mode == Changer.ChangeMode.RESET) {
            entityType = EntityType.PIG;
        }
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockState blockState = block.getState();
            if (blockState instanceof CreatureSpawner) {
                CreatureSpawner creatureSpawner = (CreatureSpawner)blockState;
                creatureSpawner.setSpawnedType(entityType);
                creatureSpawner.update();
                continue;
            }
            if (!HAS_TRIAL_SPAWNER || !((blockState = block.getState()) instanceof TrialSpawner)) continue;
            TrialSpawner trialSpawner = (TrialSpawner)blockState;
            TrialSpawnerConfiguration config = trialSpawner.isOminous() ? trialSpawner.getOminousConfiguration() : trialSpawner.getNormalConfiguration();
            config.setSpawnedType(entityType);
            trialSpawner.update();
        }
    }

    @Override
    public Class<EntityData> getReturnType() {
        return EntityData.class;
    }

    @Override
    protected String getPropertyName() {
        return "entity type";
    }

    static {
        ExprSpawnerType.register(ExprSpawnerType.class, EntityData.class, "(spawner|entity|creature) type[s]", "blocks");
    }
}

