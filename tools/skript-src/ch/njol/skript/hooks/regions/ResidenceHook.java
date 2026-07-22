/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bekvon.bukkit.residence.Residence
 *  com.bekvon.bukkit.residence.containers.Flags
 *  com.bekvon.bukkit.residence.protection.ClaimedResidence
 *  com.bekvon.bukkit.residence.protection.ResidencePermissions
 *  com.google.common.base.Objects
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions;

import ch.njol.skript.hooks.regions.RegionsPlugin;
import ch.njol.skript.hooks.regions.WorldGuardHook;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilID;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.google.common.base.Objects;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ResidenceHook
extends RegionsPlugin<Residence> {
    @Override
    protected boolean init() {
        return super.init();
    }

    @Override
    public String getName() {
        return "Residence";
    }

    @Override
    public boolean canBuild_i(Player p, Location l) {
        ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(l);
        if (res == null) {
            return true;
        }
        ResidencePermissions perms = res.getPermissions();
        return perms.playerHas(p, Flags.build, true);
    }

    @Override
    public Collection<? extends Region> getRegionsAt_i(Location l) {
        ArrayList<ResidenceRegion> ress = new ArrayList<ResidenceRegion>();
        ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(l);
        if (res == null) {
            return Collections.emptyList();
        }
        ress.add(new ResidenceRegion(l.getWorld(), res));
        return ress;
    }

    @Override
    @Nullable
    public Region getRegion_i(World world, String name) {
        ClaimedResidence res = Residence.getInstance().getResidenceManager().getByName(name);
        if (res == null) {
            return null;
        }
        return new ResidenceRegion(world, res);
    }

    @Override
    public boolean hasMultipleOwners_i() {
        return true;
    }

    @Override
    protected Class<? extends Region> getRegionClass() {
        return WorldGuardHook.WorldGuardRegion.class;
    }

    static {
        Variables.yggdrasil.registerSingleClass(ResidenceRegion.class);
    }

    @YggdrasilID(value="ResidenceRegion")
    public class ResidenceRegion
    extends Region {
        private transient ClaimedResidence res;
        final World world;

        private ResidenceRegion() {
            this.world = null;
        }

        public ResidenceRegion(World w, ClaimedResidence r) {
            this.res = r;
            this.world = w;
        }

        @Override
        public Fields serialize() throws NotSerializableException {
            Fields f = new Fields(this);
            f.putObject("region", this.res.getName());
            return f;
        }

        @Override
        public void deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
            Object region = fields.getObject("region");
            if (!(region instanceof String)) {
                throw new StreamCorruptedException("Tried to deserialize Residence region with no valid name!");
            }
            fields.setFields(this);
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByName((String)region);
            if (res == null) {
                throw new StreamCorruptedException("Invalid region " + String.valueOf(region) + " in world " + String.valueOf(this.world));
            }
            this.res = res;
        }

        @Override
        public boolean contains(Location l) {
            return this.res.containsLoc(l);
        }

        @Override
        public boolean isMember(OfflinePlayer p) {
            return this.res.getPermissions().playerHas(p.getName(), Flags.build, false);
        }

        @Override
        public Collection<OfflinePlayer> getMembers() {
            return Collections.emptyList();
        }

        @Override
        public boolean isOwner(OfflinePlayer p) {
            return Objects.equal((Object)this.res.getPermissions().getOwnerUUID(), (Object)p.getUniqueId());
        }

        @Override
        public Collection<OfflinePlayer> getOwners() {
            return Collections.singleton(Residence.getInstance().getOfflinePlayer(this.res.getPermissions().getOwner()));
        }

        @Override
        public Iterator<Block> getBlocks() {
            return Collections.emptyIterator();
        }

        @Override
        public String toString() {
            return this.res.getName() + " in world " + this.world.getName();
        }

        @Override
        public RegionsPlugin<?> getPlugin() {
            return ResidenceHook.this;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ResidenceRegion)) {
                return false;
            }
            return o.hashCode() == this.hashCode();
        }

        @Override
        public int hashCode() {
            return this.res.getName().hashCode();
        }
    }
}

