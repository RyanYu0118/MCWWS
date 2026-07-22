/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.HumanEntity
 */
package ch.njol.skript.paperlib.environments;

import ch.njol.skript.paperlib.environments.SpigotEnvironment;
import ch.njol.skript.paperlib.features.asyncchunks.AsyncChunksPaper_13;
import ch.njol.skript.paperlib.features.asyncchunks.AsyncChunksPaper_15;
import ch.njol.skript.paperlib.features.asyncchunks.AsyncChunksPaper_9_12;
import ch.njol.skript.paperlib.features.asyncteleport.AsyncTeleportPaper;
import ch.njol.skript.paperlib.features.asyncteleport.AsyncTeleportPaper_13;
import ch.njol.skript.paperlib.features.bedspawnlocation.BedSpawnLocationPaper;
import ch.njol.skript.paperlib.features.blockstatesnapshot.BlockStateSnapshotOptionalSnapshots;
import ch.njol.skript.paperlib.features.chunkisgenerated.ChunkIsGeneratedApiExists;
import ch.njol.skript.paperlib.features.inventoryholdersnapshot.InventoryHolderSnapshotOptionalSnapshots;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;

public class PaperEnvironment
extends SpigotEnvironment {
    public PaperEnvironment() {
        if (this.isVersion(13, 1)) {
            this.asyncChunksHandler = new AsyncChunksPaper_13();
            this.asyncTeleportHandler = new AsyncTeleportPaper_13();
        } else if (this.isVersion(9) && !this.isVersion(13)) {
            this.asyncChunksHandler = new AsyncChunksPaper_9_12();
            this.asyncTeleportHandler = new AsyncTeleportPaper();
        }
        if (this.isVersion(12)) {
            this.isGeneratedHandler = new ChunkIsGeneratedApiExists();
            this.blockStateSnapshotHandler = new BlockStateSnapshotOptionalSnapshots();
        }
        if (this.isVersion(15, 2)) {
            try {
                World.class.getDeclaredMethod("getChunkAtAsyncUrgently", Location.class);
                this.asyncChunksHandler = new AsyncChunksPaper_15();
                HumanEntity.class.getDeclaredMethod("getPotentialBedLocation", new Class[0]);
                this.bedSpawnLocationHandler = new BedSpawnLocationPaper();
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
        }
        if (this.isVersion(16)) {
            this.inventoryHolderSnapshotHandler = new InventoryHolderSnapshotOptionalSnapshots();
        }
    }

    @Override
    public String getName() {
        return "Paper";
    }

    @Override
    public boolean isPaper() {
        return true;
    }
}

