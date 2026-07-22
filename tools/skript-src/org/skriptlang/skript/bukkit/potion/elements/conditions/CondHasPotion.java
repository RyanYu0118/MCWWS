/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.potion.PotionEffect
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Has Potion Effect")
@Description(value={"Checks whether an entity has a potion effect with certain properties.", "An entity is considered having a potion effect if it has a potion effect with at least the specified properties.", "For example, if an entity has an 'ambient speed 5' effect, they would be considered as having 'speed 5'.", "For exact comparisons, consider using the <a href='./expressions.html#ExprPotionEffect'>Potion Effect of Entity/Item</a> expression in an 'is' comparison."})
@Example.Examples(value={@Example(value="if the player has a potion effect of speed:\n\tmessage \"You are sonic!\"\n"), @Example(value="if all players have speed and haste active:\n\tbroadcast \"This server is ready to mine!\"\n")})
@Since(value={"2.6.1", "2.14 (support for potion effects)"})
public class CondHasPotion
extends Condition {
    private Expression<LivingEntity> entities;
    @Nullable
    private Expression<SkriptPotionEffect> effects;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, PropertyCondition.infoBuilder(CondHasPotion.class, PropertyCondition.PropertyType.HAVE, "([any|a[n]] [active] potion effect[s]|[any|a] potion effect[s] active)", "livingentities").addPatterns(PropertyCondition.getPatterns(PropertyCondition.PropertyType.HAVE, "%skriptpotioneffects% [active]", "livingentities")).supplier(CondHasPotion::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        if (exprs.length == 2) {
            this.effects = exprs[1];
        }
        this.setNegated(matchedPattern % 2 != 0);
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.effects == null) {
            return this.entities.check(event, entity -> !entity.getActivePotionEffects().isEmpty(), this.isNegated());
        }
        SkriptPotionEffect[] effects = this.effects.getArray(event);
        return this.entities.check(event, entity -> SimpleExpression.check(effects, base -> {
            for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
                if (!base.matchesQualities(potionEffect)) continue;
                return true;
            }
            return false;
        }, this.isNegated(), this.effects.getAnd()));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String property = this.effects != null ? this.effects.toString(event, debug) : "active potion effects";
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, event, debug, this.entities, property);
    }
}

