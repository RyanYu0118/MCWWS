/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Breedable
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.breeding.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Make Breedable")
@Description(value={"Sets whether or not entities will be able to breed. Only works on animals."})
@Example(value="on spawn of animal:\n\tmake entity unbreedable\n")
@Since(value={"2.10"})
public class EffBreedable
extends Effect {
    private boolean sterilize;
    private Expression<LivingEntity> entities;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffBreedable.class).addPatterns("make %livingentities% breedable", "unsterilize %livingentities%", "make %livingentities% (not |non(-| )|un)breedable", "sterilize %livingentities%").supplier(EffBreedable::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.sterilize = matchedPattern > 1;
        this.entities = expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Breedable)) continue;
            Breedable breedable = (Breedable)entity;
            breedable.setBreed(!this.sterilize);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.entities.toString(event, debug) + (this.sterilize ? " non-" : " ") + "breedable";
    }
}

