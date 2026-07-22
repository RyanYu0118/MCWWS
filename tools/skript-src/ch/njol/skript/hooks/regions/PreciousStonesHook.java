/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones
 *  net.sacredlabyrinth.Phaed.PreciousStones.field.Field
 *  net.sacredlabyrinth.Phaed.PreciousStones.field.FieldFlag
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

import ch.njol.skript.hooks.regions.RegionsPlugin;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.util.AABB;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilID;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.field.Field;
import net.sacredlabyrinth.Phaed.PreciousStones.field.FieldFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PreciousStonesHook
extends RegionsPlugin<PreciousStones> {
    @Override
    protected boolean init() {
        return super.init();
    }

    @Override
    public String getName() {
        return "PreciousStones";
    }

    @Override
    public boolean canBuild_i(Player p, Location l) {
        return PreciousStones.API().canBreak(p, l) && PreciousStones.API().canPlace(p, l);
    }

    @Override
    public Collection<? extends Region> getRegionsAt_i(Location l) {
        Set collect = PreciousStones.API().getFieldsProtectingArea(FieldFlag.ALL, l).stream().map(x$0 -> new PreciousStonesRegion((Field)x$0)).collect(Collectors.toSet());
        assert (collect != null);
        return collect;
    }

    @Override
    @Nullable
    public Region getRegion_i(World world, String name) {
        return null;
    }

    @Override
    public boolean hasMultipleOwners_i() {
        return true;
    }

    @Override
    protected Class<? extends Region> getRegionClass() {
        return PreciousStonesRegion.class;
    }

    @YggdrasilID(value="PreciousStonesRegion")
    public final class PreciousStonesRegion
    extends Region {
        private transient Field field;

        public PreciousStonesRegion(Field field) {
            this.field = field;
        }

        @Override
        public boolean contains(Location l) {
            return this.field.envelops(l);
        }

        @Override
        public boolean isMember(OfflinePlayer p) {
            return this.field.isInAllowedList(p.getName());
        }

        @Override
        public Collection<OfflinePlayer> getMembers() {
            Set<OfflinePlayer> collect = this.field.getAllAllowed().stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
            assert (collect != null);
            return collect;
        }

        @Override
        public boolean isOwner(OfflinePlayer p) {
            return this.field.isOwner(p.getName());
        }

        @Override
        public Collection<OfflinePlayer> getOwners() {
            Set<OfflinePlayer> collect = Stream.of(Bukkit.getOfflinePlayer((String)this.field.getOwner())).collect(Collectors.toSet());
            assert (collect != null);
            return collect;
        }

        @Override
        public Iterator<Block> getBlocks() {
            List vectors = this.field.getCorners();
            return new AABB(Bukkit.getWorld((String)this.field.getWorld()), (Vector)vectors.get(0), (Vector)vectors.get(7)).iterator();
        }

        @Override
        public String toString() {
            return this.field.getName() + " in world " + this.field.getWorld();
        }

        @Override
        public RegionsPlugin<?> getPlugin() {
            return PreciousStonesHook.this;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            PreciousStonesRegion that = (PreciousStonesRegion)o;
            return Objects.equals(this.field, that.field);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.field);
        }

        @Override
        public Fields serialize() throws NotSerializableException {
            return new Fields(this);
        }

        @Override
        public void deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
            new Fields(fields).setFields(this);
        }
    }
}

