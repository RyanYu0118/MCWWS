/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.potion.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Potion Effect - Has Icon")
@Description(value={"Checks whether a potion effect has an icon."})
@Example(value="on entity potion effect modification:\n\tif the potion effect has an icon:\n\t\thide the icon of event-potioneffecttype for event-entity\n")
@Since(value={"2.14"})
public class CondPotionHasIcon
extends PropertyCondition<SkriptPotionEffect> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondPotionHasIcon.infoBuilder(CondPotionHasIcon.class, PropertyCondition.PropertyType.HAVE, "([an] icon|icons)", "skriptpotioneffects").supplier(CondPotionHasIcon::new).build());
    }

    @Override
    public boolean check(SkriptPotionEffect potionEffect) {
        return potionEffect.icon();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "an icon";
    }
}

