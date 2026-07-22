/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.damage.DamageSource
 *  org.bukkit.event.Event
 */
package org.skriptlang.skript.bukkit.damagesource.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.Event;
import org.skriptlang.skript.bukkit.damagesource.elements.expressions.ExprSecDamageSource;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Created Damage Source")
@Description(value={"Get the created damage source being created/modified in a 'custom damage source' section."})
@Example(value="set {_source} to a custom damage source:\n\tset the damage type of the created damage source to magic\n")
@Since(value={"2.12"})
public class ExprCreatedDamageSource
extends EventValueExpression<DamageSource>
implements EventRestrictedSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprCreatedDamageSource.infoBuilder(ExprCreatedDamageSource.class, DamageSource.class, "created damage source").supplier(ExprCreatedDamageSource::new)).build());
    }

    public ExprCreatedDamageSource() {
        super(DamageSource.class);
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(ExprSecDamageSource.DamageSourceSectionEvent.class);
    }
}

