/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.classes.YggdrasilSerializer;
import ch.njol.skript.hooks.regions.RegionsPlugin;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.util.Collection;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

public abstract class Region
implements YggdrasilSerializable.YggdrasilExtendedSerializable {
    @Nullable
    private static Region parse(String s, boolean error) {
        Region r = null;
        for (World w : Bukkit.getWorlds()) {
            Region r2 = RegionsPlugin.getRegion(w, s);
            if (r2 == null) continue;
            if (r != null) {
                if (error) {
                    Skript.error("Multiple regions with the name '" + s + "' exist");
                }
                return null;
            }
            r = r2;
        }
        if (r == null) {
            if (error) {
                Skript.error("Region '" + s + "' could not be found. Ensure that the region name is correct, in the proper world, and that the region data for that world is loaded.");
            }
            return null;
        }
        return r;
    }

    public abstract boolean contains(Location var1);

    public abstract boolean isMember(OfflinePlayer var1);

    public abstract Collection<OfflinePlayer> getMembers();

    public abstract boolean isOwner(OfflinePlayer var1);

    public abstract Collection<OfflinePlayer> getOwners();

    public abstract Iterator<Block> getBlocks();

    public abstract String toString();

    public abstract RegionsPlugin<?> getPlugin();

    public abstract boolean equals(@Nullable Object var1);

    public abstract int hashCode();

    static {
        Classes.registerClass(new ClassInfo<Region>(Region.class, "region").name("Region").description("A region of a regions plugin. Skript currently supports WorldGuard, Factions, GriefPrevention and PreciousStones.", "Please note that some regions plugins do not have named regions, some use numerical ids to identify regions, and some may have regions with the same name in different worlds, thus using regions like \"region name\" in scripts may or may not work.").usage("\"region name\"").examples("").after("string", "world", "offlineplayer", "player").since("2.1").user("regions?").requiredPlugins("Supported regions plugin").parser(new Parser<Region>(){

            @Override
            @Nullable
            public Region parse(String s, ParseContext context) {
                boolean quoted;
                switch (context) {
                    case DEFAULT: 
                    case EVENT: 
                    case SCRIPT: {
                        quoted = true;
                        break;
                    }
                    case COMMAND: 
                    case PARSE: 
                    case CONFIG: {
                        quoted = false;
                        break;
                    }
                    default: {
                        assert (false);
                        return null;
                    }
                }
                if (!VariableString.isQuotedCorrectly(s, quoted)) {
                    return null;
                }
                s = VariableString.unquote(s, quoted);
                return Region.parse(s, true);
            }

            @Override
            public String toString(Region r, int flags) {
                return r.toString();
            }

            @Override
            public String toVariableNameString(Region r) {
                return r.toString();
            }
        }).serializer((Serializer<Region>)new YggdrasilSerializer<Region>(){

            @Override
            public boolean mustSyncDeserialization() {
                return true;
            }
        }));
        Converters.registerConverter(String.class, Region.class, s -> Region.parse(s, ParserInstance.get().isActive()));
    }
}

