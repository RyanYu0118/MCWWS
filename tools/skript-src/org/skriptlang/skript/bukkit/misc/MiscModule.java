/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.misc;

import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.misc.elements.effects.EffRotate;
import org.skriptlang.skript.bukkit.misc.elements.expressions.ExprBroadcastMessage;
import org.skriptlang.skript.bukkit.misc.elements.expressions.ExprItemOfEntity;
import org.skriptlang.skript.bukkit.misc.elements.expressions.ExprMOTD;
import org.skriptlang.skript.bukkit.misc.elements.expressions.ExprQuaternionAxisAngle;
import org.skriptlang.skript.bukkit.misc.elements.expressions.ExprRotate;
import org.skriptlang.skript.bukkit.misc.elements.expressions.ExprTextOf;
import org.skriptlang.skript.bukkit.misc.elements.expressions.ExprWithYawPitch;

public class MiscModule
extends HierarchicalAddonModule {
    public MiscModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, EffRotate::register, ExprBroadcastMessage::register, ExprItemOfEntity::register, ExprMOTD::register, ExprQuaternionAxisAngle::register, ExprRotate::register, ExprTextOf::register, ExprWithYawPitch::register);
    }

    @Override
    public String name() {
        return "misc";
    }
}

