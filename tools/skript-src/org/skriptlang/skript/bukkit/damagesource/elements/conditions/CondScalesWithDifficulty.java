/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.damage.DamageSource
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.damagesource.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Damage Source - Does Scale With Difficulty")
@Description(value={"Whether the damage from a damage source scales with the difficulty of the server."})
@Example(value="on death:\n\tif event-damage source scales damage with difficulty:\n")
@Since(value={"2.12"})
public class CondScalesWithDifficulty
extends PropertyCondition<DamageSource> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(CondScalesWithDifficulty.class).addPatterns("%damagesources% ((does|do) scale|scales) damage with difficulty", "%damagesources% (do not|don't|does not|doesn't) scale damage with difficulty", "%damagesources%'[s] damage ((does|do) scale|scales) with difficulty", "%damagesources%'[s] damage (do not|don't|does not|doesn't) scale with difficulty").supplier(CondScalesWithDifficulty::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(matchedPattern % 2 == 1);
        return true;
    }

    @Override
    public boolean check(DamageSource damageSource) {
        return damageSource.scalesWithDifficulty();
    }

    @Override
    protected String getPropertyName() {
        return "scales with difficulty";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)this.getExpr());
        if (this.isNegated()) {
            builder.append((Object)"does not scale");
        } else {
            builder.append((Object)"scales");
        }
        builder.append((Object)"with difficulty");
        return builder.toString();
    }
}

