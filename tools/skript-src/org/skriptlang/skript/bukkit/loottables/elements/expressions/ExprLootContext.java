/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.loot.LootContext
 */
package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.loot.LootContext;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Loot Context")
@Description(value={"The loot context involved in the context create section."})
@Example(value="set {_context} to a new loot context at {_location}:\n\tbroadcast loot context\n")
@Since(value={"2.10"})
public class ExprLootContext
extends EventValueExpression<LootContext> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprLootContext.infoBuilder(ExprLootContext.class, LootContext.class, "loot[ ]context").supplier(ExprLootContext::new)).build());
    }

    public ExprLootContext() {
        super(LootContext.class);
    }

    @Override
    public String toString() {
        return "the loot context";
    }
}

