/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.common;

import ch.njol.skript.registrations.Classes;
import java.util.List;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.common.elements.expressions.ExprColorFromHexCode;
import org.skriptlang.skript.common.elements.expressions.ExprHexCode;
import org.skriptlang.skript.common.elements.expressions.ExprRecursiveSize;
import org.skriptlang.skript.common.properties.PropertiesModule;
import org.skriptlang.skript.common.types.QuaternionClassInfo;
import org.skriptlang.skript.common.types.QueueClassInfo;
import org.skriptlang.skript.common.types.ScriptClassInfo;

public class CommonModule
extends HierarchicalAddonModule {
    @Override
    public Iterable<AddonModule> children() {
        return List.of(new PropertiesModule(this));
    }

    @Override
    protected void initSelf(SkriptAddon addon) {
        Classes.registerClass(new ScriptClassInfo());
        Classes.registerClass(new QuaternionClassInfo());
        Classes.registerClass(new QueueClassInfo());
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, ExprColorFromHexCode::register, ExprHexCode::register, ExprRecursiveSize::register);
    }

    @Override
    public String name() {
        return "common";
    }
}

