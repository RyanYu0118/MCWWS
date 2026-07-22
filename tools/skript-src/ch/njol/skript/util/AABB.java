/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.util.Math2;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AABB
implements Iterable<Block> {
    final World world;
    final Vector lowerBound;
    final Vector upperBound;

    public AABB(Location l1, Location l2) {
        if (l1.getWorld() != l2.getWorld()) {
            throw new IllegalArgumentException("Locations must be in the same world");
        }
        this.world = l1.getWorld();
        this.lowerBound = new Vector(Math.min(l1.getBlockX(), l2.getBlockX()), Math.min(l1.getBlockY(), l2.getBlockY()), Math.min(l1.getBlockZ(), l2.getBlockZ()));
        this.upperBound = new Vector(Math.max(l1.getBlockX(), l2.getBlockX()), Math.max(l1.getBlockY(), l2.getBlockY()), Math.max(l1.getBlockZ(), l2.getBlockZ()));
    }

    public AABB(Block b1, Block b2) {
        if (b1.getWorld() != b2.getWorld()) {
            throw new IllegalArgumentException("Blocks must be in the same world");
        }
        this.world = b1.getWorld();
        this.lowerBound = new Vector(Math.min(b1.getX(), b2.getX()), Math.min(b1.getY(), b2.getY()), Math.min(b1.getZ(), b2.getZ()));
        this.upperBound = new Vector(Math.max(b1.getX(), b2.getX()), Math.max(b1.getY(), b2.getY()), Math.max(b1.getZ(), b2.getZ()));
    }

    public AABB(Location center, double rX, double rY, double rZ) {
        assert (rX >= 0.0 && rY >= 0.0 && rZ >= 0.0) : rX + "," + rY + "," + rY;
        this.world = center.getWorld();
        int min = this.world.getMinHeight();
        this.lowerBound = new Vector(center.getX() - rX, Math.max(center.getY() - rY, (double)min), center.getZ() - rZ);
        this.upperBound = new Vector(center.getX() + rX, Math.min(center.getY() + rY, (double)(this.world.getMaxHeight() - 1)), center.getZ() + rZ);
    }

    public AABB(World w, Vector v1, Vector v2) {
        this.world = w;
        this.lowerBound = new Vector(Math.min(v1.getX(), v2.getX()), Math.min(v1.getY(), v2.getY()), Math.min(v1.getZ(), v2.getZ()));
        this.upperBound = new Vector(Math.max(v1.getX(), v2.getX()), Math.max(v1.getY(), v2.getY()), Math.max(v1.getZ(), v2.getZ()));
    }

    public AABB(Chunk c) {
        this.world = c.getWorld();
        int min = this.world.getMinHeight();
        this.lowerBound = c.getBlock(0, min, 0).getLocation().toVector();
        this.upperBound = c.getBlock(15, this.world.getMaxHeight() - 1, 15).getLocation().toVector();
    }

    public boolean contains(Location l) {
        if (l.getWorld() != this.world) {
            return false;
        }
        return this.lowerBound.getX() - 1.0E-10 < l.getX() && l.getX() < this.upperBound.getX() + 1.0E-10 && this.lowerBound.getY() - 1.0E-10 < l.getY() && l.getY() < this.upperBound.getY() + 1.0E-10 && this.lowerBound.getZ() - 1.0E-10 < l.getZ() && l.getZ() < this.upperBound.getZ() + 1.0E-10;
    }

    public boolean contains(Block b) {
        return this.contains(b.getLocation()) && this.contains(b.getLocation().add(1.0, 1.0, 1.0));
    }

    public Vector getDimensions() {
        return this.upperBound.clone().subtract(this.lowerBound);
    }

    public World getWorld() {
        return this.world;
    }

    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>(){
            private final int minX;
            private final int minY;
            private final int minZ;
            private final int maxX;
            private final int maxY;
            private final int maxZ;
            private int x;
            private int y;
            private int z;
            {
                this.minX = (int)Math2.ceil(AABB.this.lowerBound.getX());
                this.minY = (int)Math2.ceil(AABB.this.lowerBound.getY());
                this.minZ = (int)Math2.ceil(AABB.this.lowerBound.getZ());
                this.maxX = (int)Math2.floor(AABB.this.upperBound.getX());
                this.maxY = (int)Math2.floor(AABB.this.upperBound.getY());
                this.maxZ = (int)Math2.floor(AABB.this.upperBound.getZ());
                this.x = this.minX - 1;
                this.y = this.minY;
                this.z = this.minZ;
            }

            @Override
            public boolean hasNext() {
                return this.y <= this.maxY && (this.x != this.maxX || this.y != this.maxY || this.z != this.maxZ);
            }

            @Override
            public Block next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                ++this.x;
                if (this.x > this.maxX) {
                    this.x = this.minX;
                    ++this.z;
                    if (this.z > this.maxZ) {
                        this.z = this.minZ;
                        ++this.y;
                    }
                }
                if (this.y > this.maxY) {
                    throw new NoSuchElementException();
                }
                return AABB.this.world.getBlockAt(this.x, this.y, this.z);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + this.lowerBound.hashCode();
        result = 31 * result + this.upperBound.hashCode();
        result = 31 * result + this.world.hashCode();
        return result;
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AABB)) {
            return false;
        }
        AABB other = (AABB)obj;
        if (!this.lowerBound.equals((Object)other.lowerBound)) {
            return false;
        }
        if (!this.upperBound.equals((Object)other.upperBound)) {
            return false;
        }
        return this.world.equals((Object)other.world);
    }
}

