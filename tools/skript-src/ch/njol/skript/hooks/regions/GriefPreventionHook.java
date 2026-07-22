/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.ryanhamshire.GriefPrevention.Claim
 *  me.ryanhamshire.GriefPrevention.DataStore
 *  me.ryanhamshire.GriefPrevention.GriefPrevention
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.regions.RegionsPlugin;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.util.AABB;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.iterator.EmptyIterator;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilID;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class GriefPreventionHook
extends RegionsPlugin<GriefPrevention> {
    boolean supportsUUIDs;
    @Nullable
    Method getClaim;
    @Nullable
    Field claimsField;

    @Override
    protected boolean init() {
        this.supportsUUIDs = Skript.fieldExists(Claim.class, "ownerID");
        try {
            this.getClaim = DataStore.class.getDeclaredMethod("getClaim", Long.TYPE);
            this.getClaim.setAccessible(true);
            if (!Claim.class.isAssignableFrom(this.getClaim.getReturnType())) {
                this.getClaim = null;
            }
        }
        catch (NoSuchMethodException noSuchMethodException) {
        }
        catch (SecurityException securityException) {
            // empty catch block
        }
        try {
            this.claimsField = DataStore.class.getDeclaredField("claims");
            this.claimsField.setAccessible(true);
            if (!List.class.isAssignableFrom(this.claimsField.getType())) {
                this.claimsField = null;
            }
        }
        catch (NoSuchFieldException noSuchFieldException) {
        }
        catch (SecurityException securityException) {
            // empty catch block
        }
        if (this.getClaim == null && this.claimsField == null) {
            Skript.error("Skript " + String.valueOf(Skript.getVersion()) + " is not compatible with GriefPrevention " + ((GriefPrevention)this.plugin).getDescription().getVersion() + ". Please report this at https://github.com/SkriptLang/Skript/issues/ if this error occurred after you updated GriefPrevention.");
            return false;
        }
        return super.init();
    }

    @Nullable
    Claim getClaim(long id) {
        block13: {
            if (this.getClaim != null) {
                try {
                    return (Claim)this.getClaim.invoke((Object)((GriefPrevention)this.plugin).dataStore, id);
                }
                catch (IllegalAccessException e) {
                    assert (false) : e;
                    break block13;
                }
                catch (IllegalArgumentException e) {
                    assert (false) : e;
                    break block13;
                }
                catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
            assert (this.claimsField != null);
            try {
                List claims = (List)this.claimsField.get(((GriefPrevention)this.plugin).dataStore);
                for (Object claim : claims) {
                    if (!(claim instanceof Claim) || ((Claim)claim).getID() != id) continue;
                    return (Claim)claim;
                }
            }
            catch (IllegalArgumentException e) {
                assert (false) : e;
            }
            catch (IllegalAccessException e) {
                if ($assertionsDisabled) break block13;
                throw new AssertionError((Object)e);
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "GriefPrevention";
    }

    @Override
    public boolean canBuild_i(Player p, Location l) {
        return ((GriefPrevention)this.plugin).allowBuild(p, l) == null;
    }

    @Override
    public Collection<? extends Region> getRegionsAt_i(Location l) {
        Claim c = ((GriefPrevention)this.plugin).dataStore.getClaimAt(l, false, null);
        if (c != null) {
            return Arrays.asList(new GriefPreventionRegion(c));
        }
        return Collections.emptySet();
    }

    @Override
    @Nullable
    public Region getRegion_i(World world, String name) {
        try {
            Claim c = this.getClaim(Long.parseLong(name));
            if (c != null && world.equals((Object)c.getLesserBoundaryCorner().getWorld())) {
                return new GriefPreventionRegion(c);
            }
            return null;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean hasMultipleOwners_i() {
        return false;
    }

    @Override
    protected Class<? extends Region> getRegionClass() {
        return GriefPreventionRegion.class;
    }

    static {
        Variables.yggdrasil.registerSingleClass(GriefPreventionRegion.class);
    }

    @YggdrasilID(value="GriefPreventionRegion")
    public final class GriefPreventionRegion
    extends Region {
        private transient Claim claim;

        private GriefPreventionRegion() {
        }

        public GriefPreventionRegion(Claim c) {
            this.claim = c;
        }

        @Override
        public boolean contains(Location l) {
            return this.claim.contains(l, false, false);
        }

        @Override
        public boolean isMember(OfflinePlayer p) {
            return this.isOwner(p);
        }

        @Override
        public Collection<OfflinePlayer> getMembers() {
            return this.getOwners();
        }

        @Override
        public boolean isOwner(OfflinePlayer p) {
            String name = p.getName();
            if (name != null) {
                return name.equalsIgnoreCase(this.claim.getOwnerName());
            }
            return false;
        }

        @Override
        public Collection<OfflinePlayer> getOwners() {
            if (this.claim.isAdminClaim() || GriefPreventionHook.this.supportsUUIDs && this.claim.ownerID == null) {
                return Collections.emptyList();
            }
            if (GriefPreventionHook.this.supportsUUIDs) {
                return Arrays.asList(Bukkit.getOfflinePlayer((UUID)this.claim.ownerID));
            }
            return Arrays.asList(Bukkit.getOfflinePlayer((String)this.claim.getOwnerName()));
        }

        @Override
        public Iterator<Block> getBlocks() {
            Location lower = this.claim.getLesserBoundaryCorner();
            Location upper = this.claim.getGreaterBoundaryCorner();
            if (lower == null || upper == null || lower.getWorld() == null || upper.getWorld() == null || lower.getWorld() != upper.getWorld()) {
                return EmptyIterator.get();
            }
            upper.setY((double)(upper.getWorld().getMaxHeight() - 1));
            upper.setX((double)upper.getBlockX());
            upper.setZ((double)upper.getBlockZ());
            return new AABB(lower, upper).iterator();
        }

        @Override
        public String toString() {
            return "Claim #" + this.claim.getID();
        }

        @Override
        public Fields serialize() {
            Fields f = new Fields();
            f.putPrimitive("id", this.claim.getID());
            return f;
        }

        @Override
        public void deserialize(Fields fields) throws StreamCorruptedException {
            long id = fields.getPrimitive("id", Long.TYPE);
            Claim c = GriefPreventionHook.this.getClaim(id);
            if (c == null) {
                throw new StreamCorruptedException("Invalid claim " + id);
            }
            this.claim = c;
        }

        @Override
        public RegionsPlugin<?> getPlugin() {
            return GriefPreventionHook.this;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof GriefPreventionRegion)) {
                return false;
            }
            return this.claim.equals(((GriefPreventionRegion)o).claim);
        }

        @Override
        public int hashCode() {
            return this.claim.hashCode();
        }
    }
}

