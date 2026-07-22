/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Poison/Cure")
@Description(value={"Poison or cure an entity. If the entity is already poisoned, the duration may be overwritten."})
@Example.Examples(value={@Example(value="poison the player"), @Example(value="poison the victim for 20 seconds"), @Example(value="cure the player from of poison")})
@Since(value={"1.3.2"})
public class EffPoison
extends Effect {
    private Expression<LivingEntity> entities;
    @Nullable
    private Expression<Timespan> duration;
    private boolean cure;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffPoison.class).supplier(EffPoison::new).addPatterns("poison %livingentities% [for %-timespan%]", "(cure|unpoison) %livingentities% [(from|of) poison]").build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        if (matchedPattern == 0) {
            this.duration = exprs[1];
        }
        this.cure = matchedPattern == 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (this.cure) {
            for (LivingEntity entity : this.entities.getArray(event)) {
                entity.removePotionEffect(PotionEffectType.POISON);
            }
        } else {
            Timespan timespan;
            int duration = 600;
            if (this.duration != null && (timespan = this.duration.getSingle(event)) != null) {
                duration = (int)Math2.fit(0L, timespan.getAs(Timespan.TimePeriod.TICK), Integer.MAX_VALUE);
            }
            for (LivingEntity livingEntity : this.entities.getArray(event)) {
                int specificDuration = duration;
                if (livingEntity.hasPotionEffect(PotionEffectType.POISON)) {
                    int existingDuration = livingEntity.getPotionEffect(PotionEffectType.POISON).getDuration();
                    specificDuration = Math2.fit(0, specificDuration + existingDuration, Integer.MAX_VALUE);
                }
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, specificDuration, 0));
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.cure) {
            builder.append((Object)"cure");
        } else {
            builder.append((Object)"poison");
        }
        builder.append((Object)this.entities);
        if (this.duration != null) {
            builder.append("for", this.duration);
        }
        return builder.toString();
    }
}

