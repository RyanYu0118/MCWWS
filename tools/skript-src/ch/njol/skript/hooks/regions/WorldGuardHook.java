/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sk89q.worldedit.bukkit.BukkitAdapter
 *  com.sk89q.worldedit.math.BlockVector3
 *  com.sk89q.worldguard.WorldGuard
 *  com.sk89q.worldguard.bukkit.WorldGuardPlugin
 *  com.sk89q.worldguard.internal.platform.WorldGuardPlatform
 *  com.sk89q.worldguard.protection.ApplicableRegionSet
 *  com.sk89q.worldguard.protection.flags.StateFlag
 *  com.sk89q.worldguard.protection.managers.RegionManager
 *  com.sk89q.worldguard.protection.regions.ProtectedRegion
 *  com.sk89q.worldguard.protection.regions.RegionQuery
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.regions.RegionsPlugin;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.util.AABB;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilID;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class WorldGuardHook
extends RegionsPlugin<WorldGuardPlugin> {
    @Override
    protected boolean init() {
        if (!Skript.classExists("com.sk89q.worldedit.math.BlockVector3")) {
            Skript.error("WorldEdit you're using is not compatible with Skript. Disabling WorldGuard support!");
            return false;
        }
        return super.init();
    }

    @Override
    public String getName() {
        return "WorldGuard";
    }

    @Override
    public boolean canBuild_i(Player p, Location l) {
        if (p.hasPermission("worldguard.region.bypass." + l.getWorld().getName())) {
            return true;
        }
        WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
        RegionQuery query = platform.getRegionContainer().createQuery();
        return query.testBuild(BukkitAdapter.adapt((Location)l), ((WorldGuardPlugin)this.plugin).wrapPlayer(p), new StateFlag[0]);
    }

    @Override
    public Collection<? extends Region> getRegionsAt_i(@Nullable Location l) {
        ArrayList<WorldGuardRegion> r = new ArrayList<WorldGuardRegion>();
        if (l == null) {
            return Collections.emptyList();
        }
        if (l.getWorld() == null) {
            return Collections.emptyList();
        }
        WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
        RegionManager manager = platform.getRegionContainer().get(BukkitAdapter.adapt((World)l.getWorld()));
        if (manager == null) {
            return r;
        }
        ApplicableRegionSet applicable = manager.getApplicableRegions(BukkitAdapter.asBlockVector((Location)l));
        if (applicable == null) {
            return r;
        }
        for (ProtectedRegion region : applicable) {
            r.add(new WorldGuardRegion(l.getWorld(), region));
        }
        return r;
    }

    @Override
    @Nullable
    public Region getRegion_i(World world, String name) {
        WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
        RegionManager manager = platform.getRegionContainer().get(BukkitAdapter.adapt((World)world));
        if (manager == null) {
            return null;
        }
        ProtectedRegion region = manager.getRegion(name);
        if (region != null) {
            return new WorldGuardRegion(world, region);
        }
        return null;
    }

    @Override
    public boolean hasMultipleOwners_i() {
        return true;
    }

    @Override
    protected Class<? extends Region> getRegionClass() {
        return WorldGuardRegion.class;
    }

    static {
        Variables.yggdrasil.registerSingleClass(WorldGuardRegion.class);
    }

    @YggdrasilID(value="WorldGuardRegion")
    public final class WorldGuardRegion
    extends Region {
        final World world;
        private transient ProtectedRegion region;

        private WorldGuardRegion() {
            this.world = null;
        }

        public WorldGuardRegion(World w, ProtectedRegion r) {
            this.world = w;
            this.region = r;
        }

        @Override
        public boolean contains(Location l) {
            return l.getWorld().equals((Object)this.world) && this.region.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        }

        @Override
        public boolean isMember(OfflinePlayer p) {
            return this.region.isMember(((WorldGuardPlugin)WorldGuardHook.this.plugin).wrapOfflinePlayer(p));
        }

        @Override
        public Collection<OfflinePlayer> getMembers() {
            Set ids = this.region.getMembers().getUniqueIds();
            ArrayList<OfflinePlayer> r = new ArrayList<OfflinePlayer>(ids.size());
            for (UUID id : ids) {
                r.add(Bukkit.getOfflinePlayer((UUID)id));
            }
            return r;
        }

        @Override
        public boolean isOwner(OfflinePlayer p) {
            return this.region.isOwner(((WorldGuardPlugin)WorldGuardHook.this.plugin).wrapOfflinePlayer(p));
        }

        @Override
        public Collection<OfflinePlayer> getOwners() {
            Set ids = this.region.getOwners().getUniqueIds();
            ArrayList<OfflinePlayer> r = new ArrayList<OfflinePlayer>(ids.size());
            for (UUID id : ids) {
                r.add(Bukkit.getOfflinePlayer((UUID)id));
            }
            return r;
        }

        @Override
        public Iterator<Block> getBlocks() {
            BlockVector3 min = this.region.getMinimumPoint();
            BlockVector3 max = this.region.getMaximumPoint();
            return new AABB(this.world, new Vector(min.getBlockX(), min.getBlockY(), min.getBlockZ()), new Vector(max.getBlockX(), max.getBlockY(), max.getBlockZ())).iterator();
        }

        @Override
        public Fields serialize() throws NotSerializableException {
            Fields f = new Fields(this);
            f.putObject("region", this.region.getId());
            return f;
        }

        @Override
        public void deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
            String r = fields.getAndRemoveObject("region", String.class);
            fields.setFields(this);
            WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
            ProtectedRegion region = platform.getRegionContainer().get(BukkitAdapter.adapt((World)this.world)).getRegion(r);
            if (region == null) {
                throw new StreamCorruptedException("Invalid region " + r + " in world " + String.valueOf(this.world));
            }
            this.region = region;
        }

        @Override
        public String toString() {
            return this.region.getId() + " in world " + this.world.getName();
        }

        @Override
        public RegionsPlugin<?> getPlugin() {
            return WorldGuardHook.this;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof WorldGuardRegion)) {
                return false;
            }
            return this.world.equals((Object)((WorldGuardRegion)o).world) && this.region.equals(((WorldGuardRegion)o).region);
        }

        @Override
        public int hashCode() {
            return this.world.hashCode() * 31 + this.region.hashCode();
        }
    }
}

