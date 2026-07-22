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

@Name(value="Potion Effect - Particles")
@Description(value={"Modify whether a potion effect shows particles."})
@Example(value="hide the particles for the player's potion effects")
@Since(value={"2.14"})
public class EffPotionParticles
extends PotionPropertyEffect {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffPotionParticles.class).supplier(EffPotionParticles::new).addPatterns(EffPotionParticles.getPatterns(PotionPropertyEffect.Type.SHOW, "particles")).build());
    }

    @Override
    public void modify(SkriptPotionEffect effect, boolean isNegated) {
        effect.particles(!isNegated);
    }

    @Override
    public PotionPropertyEffect.Type getPropertyType() {
        return PotionPropertyEffect.Type.SHOW;
    }

    @Override
    public String getPropertyName() {
        return "particles";
    }
}

