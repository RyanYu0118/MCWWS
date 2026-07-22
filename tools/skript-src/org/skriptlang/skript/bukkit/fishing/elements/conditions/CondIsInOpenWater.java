/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.FishHook
 */
package org.skriptlang.skript.bukkit.fishing.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Is Fish Hook in Open Water")
@Description(value={"Checks whether the fish hook is in open water.", "Open water is defined by a 5x4x5 area of water, air and lily pads. If in open water, treasure items may be caught."})
@Example(value="on fish catch:\n\tif fish hook is in open water:\n\t\tsend \"You will catch a shark soon!\"\n")
@Events(value={"Fishing"})
@Since(value={"2.10"})
public class CondIsInOpenWater
extends PropertyCondition<Entity> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondIsInOpenWater.infoBuilder(CondIsInOpenWater.class, PropertyCondition.PropertyType.BE, "in open water[s]", "entities").supplier(CondIsInOpenWater::new).build());
    }

    @Override
    public boolean check(Entity entity) {
        if (!(entity instanceof FishHook)) {
            return false;
        }
        FishHook hook = (FishHook)entity;
        return hook.isInOpenWater();
    }

    @Override
    protected String getPropertyName() {
        return "in open water";
    }
}

