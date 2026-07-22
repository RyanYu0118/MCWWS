/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.item.book;

import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.item.book.elements.expressions.ExprBookAuthor;
import org.skriptlang.skript.bukkit.item.book.elements.expressions.ExprBookPages;
import org.skriptlang.skript.bukkit.item.book.elements.expressions.ExprBookTitle;

public class BookModule
extends HierarchicalAddonModule {
    public BookModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, ExprBookAuthor::register, ExprBookPages::register, ExprBookTitle::register);
    }

    @Override
    public String name() {
        return "book";
    }
}

