/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.addon;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;

public abstract class HierarchicalAddonModule
implements AddonModule {
    @Nullable
    private final AddonModule parentModule;
    private final List<AddonModule> loadableChildren = new ArrayList<AddonModule>();

    protected HierarchicalAddonModule() {
        this.parentModule = null;
    }

    protected HierarchicalAddonModule(@Nullable AddonModule parentModule) {
        this.parentModule = parentModule;
    }

    @Nullable
    public AddonModule parent() {
        return this.parentModule;
    }

    public Iterable<AddonModule> children() {
        return List.of();
    }

    private List<AddonModule> moduleChain() {
        ArrayList<AddonModule> chain = new ArrayList<AddonModule>();
        AddonModule current = this;
        while (current != null) {
            chain.add(current);
            if (!(current instanceof HierarchicalAddonModule)) break;
            HierarchicalAddonModule hierarchical = current;
            current = hierarchical.parent();
        }
        return chain;
    }

    protected boolean canLoadSelf(SkriptAddon addon) {
        return true;
    }

    @Override
    public final boolean canLoad(SkriptAddon addon) {
        if (!this.canLoadSelf(addon)) {
            return false;
        }
        this.loadableChildren.clear();
        for (AddonModule child : this.children()) {
            if (!child.canLoad(addon)) continue;
            this.loadableChildren.add(child);
        }
        return true;
    }

    protected void initSelf(SkriptAddon addon) {
    }

    @Override
    public final void init(SkriptAddon addon) {
        this.initSelf(addon);
        for (AddonModule child : this.loadableChildren) {
            child.init(addon);
        }
    }

    protected abstract void loadSelf(SkriptAddon var1);

    @Override
    public final void load(SkriptAddon addon) {
        this.loadSelf(addon);
        for (AddonModule child : this.loadableChildren) {
            child.load(addon);
        }
    }

    @Override
    public final AddonModule.ModuleOrigin origin(SkriptAddon addon) {
        AddonModule[] modules = this.moduleChain().toArray(new AddonModule[0]);
        return AddonModule.origin(addon, modules);
    }
}

