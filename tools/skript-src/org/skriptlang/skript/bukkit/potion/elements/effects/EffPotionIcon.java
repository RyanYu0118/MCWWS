/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.potion.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.skriptlang.skript.bukkit.potion.elements.effects.PotionPropertyEffect;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Potion Effect - Icon")
@Description(value={"Modify whether a potion effect shows an icon."})
@Example(value="hide the icon for the player's potion effects")
@Since(value={"2.14"})
public class EffPotionIcon
extends PotionPropertyEffect {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffPotionIcon.class).supplier(EffPotionIcon::new).addPatterns(EffPotionIcon.getPatterns(PotionPropertyEffect.Type.SHOW, "icon[s]")).build());
    }

    @Override
    public void modify(SkriptPotionEffect effect, boolean isNegated) {
        effect.icon(!isNegated);
    }

    @Override
    public PotionPropertyEffect.Type getPropertyType() {
        return PotionPropertyEffect.Type.SHOW;
    }

    @Override
    public String getPropertyName() {
        return "icon";
    }
}

