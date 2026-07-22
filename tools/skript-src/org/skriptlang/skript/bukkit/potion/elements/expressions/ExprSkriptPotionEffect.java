/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.EventRestrictedSyntax;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprSecPotionEffect;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Created Potion Effect")
@Description(value={"An expression to obtain the potion effect being made in a potion effect creation section."})
@Example(value="set {_potion} to a potion effect of speed 2 for 10 minutes:\n\thide the effect's icon\n\thide the effect's particles\n")
@Since(value={"2.14"})
public class ExprSkriptPotionEffect
extends EventValueExpression<SkriptPotionEffect>
implements EventRestrictedSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprSkriptPotionEffect.infoBuilder(ExprSkriptPotionEffect.class, SkriptPotionEffect.class, "[created] [potion] effect").supplier(ExprSkriptPotionEffect::new)).build());
    }

    public ExprSkriptPotionEffect() {
        super(SkriptPotionEffect.class);
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return new Class[]{ExprSecPotionEffect.PotionEffectSectionEvent.class};
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the created potion effect";
    }
}

