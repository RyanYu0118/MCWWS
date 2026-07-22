/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Ageable
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
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Make Adult/Baby")
@Description(value={"Force a animal to become an adult or baby."})
@Example(value="on spawn of mob:\n\tentity is not an adult\n\tmake entity an adult\n")
@Since(value={"2.10"})
public class EffMakeAdultOrBaby
extends Effect {
    private boolean adult;
    private Expression<LivingEntity> entities;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffMakeAdultOrBaby.class).addPatterns("make %livingentities% [a[n]] (:adult|baby|child)", "force %livingentities% to be[come] a[n] (:adult|baby|child)").supplier(EffMakeAdultOrBaby::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.adult = parseResult.hasTag("adult");
        this.entities = expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Ageable)) continue;
            Ageable ageable = (Ageable)entity;
            if (this.adult) {
                ageable.setAdult();
                continue;
            }
            ageable.setBaby();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + String.valueOf(this.entities) + (this.adult ? " an adult" : " a baby");
    }
}

