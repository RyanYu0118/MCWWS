/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package ch.njol.skript.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class BlockLineIterator
implements Iterator<Block> {
    private final Vector3d current;
    private final Vector3d end;
    private final Vector3d centeredEnd;
    final Vector3d step;
    private final World world;
    private boolean finished;

    public BlockLineIterator(@NotNull Location start, @NotNull Location end) {
        this.current = start.toVector().toVector3d();
        this.world = start.getWorld();
        this.end = end.toVector().toVector3d();
        this.centeredEnd = BlockLineIterator.centered(this.end);
        this.step = this.end.sub((Vector3dc)this.current, new Vector3d()).normalize();
    }

    public BlockLineIterator(@NotNull Block start, @NotNull Block end) {
        this(start.getLocation().toCenterLocation(), end.getLocation().toCenterLocation());
    }

    public BlockLineIterator(Location start, @NotNull Vector direction, double distance) {
        this(start, start.clone().add(direction.clone().normalize().multiply(distance)));
    }

    public BlockLineIterator(@NotNull Block start, Vector direction, double distance) {
        this(start.getLocation().toCenterLocation(), direction, distance);
    }

    @Override
    public boolean hasNext() {
        return !this.finished;
    }

    @Override
    public Block next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("Reached the final block destination");
        }
        Vector3d vector3d = new Vector3d();
        if (this.end.sub((Vector3dc)this.current, vector3d).dot((Vector3dc)this.step) < -1.0) {
            throw new NoSuchElementException("Overshot the final block!");
        }
        Vector3d center = BlockLineIterator.centered(this.current);
        Block block = BlockLineIterator.getBlock(center, this.world);
        if (center.equals((Object)this.centeredEnd)) {
            this.finished = true;
        }
        double t = BlockLineIterator.stepsToNextFace(this.current, this.step, center) + (double)Math.ulp(1.0f);
        this.current.fma(t, (Vector3dc)this.step);
        return block;
    }

    static double stepsToNextFace(Vector3d start, @NotNull Vector3d step, Vector3d center) {
        Vector3d neededSteps = new Vector3d(Math.signum(step.x), Math.signum(step.y), Math.signum(step.z)).mulAdd(0.5, (Vector3dc)center).sub((Vector3dc)start).div((Vector3dc)step, new Vector3d());
        if (Double.isNaN(neededSteps.x)) {
            neededSteps.x = Double.POSITIVE_INFINITY;
        }
        if (Double.isNaN(neededSteps.y)) {
            neededSteps.y = Double.POSITIVE_INFINITY;
        }
        if (Double.isNaN(neededSteps.z)) {
            neededSteps.z = Double.POSITIVE_INFINITY;
        }
        return neededSteps.get(neededSteps.minComponent());
    }

    @Contract(value="_ -> new")
    private static Vector3d centered(@NotNull Vector3d vector) {
        return vector.floor(new Vector3d()).add(0.5, 0.5, 0.5);
    }

    @NotNull
    private static Block getBlock(@NotNull Vector3d vector, @NotNull World world) {
        return Vector.fromJOML((Vector3d)vector).toLocation(world).getBlock();
    }
}

