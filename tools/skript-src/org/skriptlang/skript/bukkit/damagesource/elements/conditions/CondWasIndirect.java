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

@Name(value="Damage Source - Was Indirectly Caused")
@Description(value={"Whether the damage from a damage source was indirectly caused.", "Vanilla damage sources are considered indirect if the 'causing entity' and the 'direct entity' are not the same. For example, taking damage from an arrow that was shot by an entity."})
@Example(value="on damage:\n\tif event-damage source was indirectly caused:\n")
@Since(value={"2.12"})
public class CondWasIndirect
extends PropertyCondition<DamageSource> {
    private boolean indirect;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(CondWasIndirect.class).addPatterns("%damagesources% (was|were) ([:in]directly caused|caused [:in]directly)", "%damagesources% (was not|wasn't|were not|weren't) ([:in]directly caused|caused [:in]directly)").supplier(CondWasIndirect::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.indirect = parseResult.hasTag("in");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(DamageSource damageSource) {
        return damageSource.isIndirect() == this.indirect;
    }

    @Override
    protected String getPropertyName() {
        return this.indirect ? "indirectly caused" : "directly caused";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)this.getExpr());
        if (this.getExpr().isSingle()) {
            builder.append((Object)"was");
        } else {
            builder.append((Object)"were");
        }
        if (this.isNegated()) {
            builder.append((Object)"not");
        }
        if (this.indirect) {
            builder.append((Object)"indirectly");
        } else {
            builder.append((Object)"directly");
        }
        builder.append((Object)"caused");
        return builder.toString();
    }
}

