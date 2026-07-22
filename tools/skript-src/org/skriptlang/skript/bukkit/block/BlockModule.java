/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.block;

import java.util.List;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.block.furnace.FurnaceModule;
import org.skriptlang.skript.bukkit.block.sign.SignModule;

public class BlockModule
extends HierarchicalAddonModule {
    public BlockModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    public Iterable<AddonModule> children() {
        return List.of(new FurnaceModule(this), new SignModule(this));
    }

    @Override
    public void loadSelf(SkriptAddon addon) {
    }

    @Override
    public String name() {
        return "block";
    }
}

