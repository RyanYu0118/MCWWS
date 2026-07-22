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

@Name(value="Potion Effect - Ambient")
@Description(value={"Modify whether a potion effect is ambient.", "That is, whether the potion effect produces more, translucent, particles."})
@Example(value="make the player's potion effects ambient")
@Since(value={"2.14"})
public class EffPotionAmbient
extends PotionPropertyEffect {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffPotionAmbient.class).supplier(EffPotionAmbient::new).addPatterns(EffPotionAmbient.getPatterns(PotionPropertyEffect.Type.MAKE, "ambient")).build());
    }

    @Override
    public void modify(SkriptPotionEffect effect, boolean isNegated) {
        effect.ambient(!isNegated);
    }

    @Override
    public PotionPropertyEffect.Type getPropertyType() {
        return PotionPropertyEffect.Type.MAKE;
    }

    @Override
    public String getPropertyName() {
        return "ambient";
    }
}

