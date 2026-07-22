/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.util.AABB;
import ch.njol.util.NullableChecker;
import ch.njol.util.coll.iterator.CheckedIterator;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

public class BlockSphereIterator
extends CheckedIterator<Block> {
    public BlockSphereIterator(final Location center, final double radius) {
        super(new AABB(center, radius + 0.5001, radius + 0.5001, radius + 0.5001).iterator(), new NullableChecker<Block>(){
            private final double rSquared;
            {
                this.rSquared = radius * radius * 1.00001;
            }

            @Override
            public boolean check(@Nullable Block b) {
                return b != null && center.distanceSquared(b.getLocation().add(0.5, 0.5, 0.5)) < this.rSquared;
            }
        });
    }
}

