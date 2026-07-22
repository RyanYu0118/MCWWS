/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.common.properties;

import ch.njol.skript.SkriptConfig;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.common.properties.elements.conditions.PropCondContains;
import org.skriptlang.skript.common.properties.elements.conditions.PropCondIsEmpty;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprAmount;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprCustomName;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprName;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprNumber;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprScale;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprSize;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprValueOf;
import org.skriptlang.skript.common.properties.elements.expressions.PropExprWXYZ;

public class PropertiesModule
extends HierarchicalAddonModule {
    public PropertiesModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, PropExprScale::register);
        if (SkriptConfig.useTypeProperties.value().booleanValue()) {
            this.register(addon, PropCondContains::register, PropCondIsEmpty::register, PropExprAmount::register, PropExprCustomName::register, PropExprName::register, PropExprNumber::register, PropExprSize::register, PropExprValueOf::register, PropExprWXYZ::register);
        }
    }

    @Override
    public String name() {
        return "type properties";
    }
}

