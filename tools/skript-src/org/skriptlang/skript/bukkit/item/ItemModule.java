/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.item;

import java.util.List;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.item.book.BookModule;
import org.skriptlang.skript.bukkit.item.elements.ExprItemWithLore;
import org.skriptlang.skript.bukkit.item.elements.ExprLore;

public class ItemModule
extends HierarchicalAddonModule {
    public ItemModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    public Iterable<AddonModule> children() {
        return List.of(new BookModule(this));
    }

    @Override
    public void loadSelf(SkriptAddon addon) {
        this.register(addon, ExprItemWithLore::register, ExprLore::register);
    }

    @Override
    public String name() {
        return "item";
    }
}

