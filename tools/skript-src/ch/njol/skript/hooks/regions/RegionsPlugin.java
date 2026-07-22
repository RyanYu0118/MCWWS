/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions;

import ch.njol.skript.Skript;
import ch.njol.skript.hooks.Hook;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.ClassResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class RegionsPlugin<P extends Plugin>
extends Hook<P> {
    public static final String DEPRECATION_MESSAGE = "Skript's region syntaxes are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard";
    public static Collection<RegionsPlugin<?>> plugins = new ArrayList(2);

    @Override
    protected boolean init() {
        plugins.add(this);
        Skript.warning("Loaded regions hook for " + this.getName() + ". Please note that Skript's region hooks are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard");
        return true;
    }

    public abstract boolean canBuild_i(Player var1, Location var2);

    public static boolean canBuild(Player p, Location l) {
        for (RegionsPlugin<?> pl : plugins) {
            if (pl.canBuild_i(p, l)) continue;
            return false;
        }
        return true;
    }

    public abstract Collection<? extends Region> getRegionsAt_i(Location var1);

    public static Set<? extends Region> getRegionsAt(Location l) {
        HashSet<Region> r = new HashSet<Region>();
        Iterator<RegionsPlugin<?>> it = plugins.iterator();
        while (it.hasNext()) {
            RegionsPlugin<?> pl = it.next();
            try {
                r.addAll(pl.getRegionsAt_i(l));
            }
            catch (Throwable e) {
                Skript.error(pl.getName() + " hook crashed and was removed to prevent future errors.");
                e.printStackTrace();
                it.remove();
            }
        }
        return r;
    }

    @Nullable
    public abstract Region getRegion_i(World var1, String var2);

    @Nullable
    public static Region getRegion(World world, String name) {
        Iterator<RegionsPlugin<?>> iterator = plugins.iterator();
        if (iterator.hasNext()) {
            RegionsPlugin<?> pl = iterator.next();
            return pl.getRegion_i(world, name);
        }
        return null;
    }

    public abstract boolean hasMultipleOwners_i();

    public static boolean hasMultipleOwners() {
        for (RegionsPlugin<?> pl : plugins) {
            if (!pl.hasMultipleOwners_i()) continue;
            return true;
        }
        return false;
    }

    protected abstract Class<? extends Region> getRegionClass();

    @Nullable
    public static RegionsPlugin<?> getPlugin(String name) {
        for (RegionsPlugin<?> pl : plugins) {
            if (!pl.getName().equalsIgnoreCase(name)) continue;
            return pl;
        }
        return null;
    }

    static {
        Variables.yggdrasil.registerClassResolver(new ClassResolver(){

            @Override
            @Nullable
            public String getID(Class<?> c) {
                for (RegionsPlugin<?> p : plugins) {
                    if (p.getRegionClass() != c) continue;
                    return c.getClass().getSimpleName();
                }
                return null;
            }

            @Override
            @Nullable
            public Class<?> getClass(String id) {
                for (RegionsPlugin<?> p : plugins) {
                    if (!id.equals(p.getRegionClass().getSimpleName())) continue;
                    return p.getRegionClass();
                }
                return null;
            }
        });
    }
}

