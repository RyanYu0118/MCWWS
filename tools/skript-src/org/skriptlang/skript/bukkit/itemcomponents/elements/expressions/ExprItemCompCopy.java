/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.SyntaxStringBuilder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.ComponentWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Item Component - Copy")
@Description(value={"Grab a copy of an item component of an item. Any changes made to the copy will not be present on the item."})
@Example(value="set {_component} to the item component copy of (the equippable component of {_item})")
@Since(value={"2.13"})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
public class ExprItemCompCopy
extends SimplePropertyExpression<ComponentWrapper, ComponentWrapper> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprItemCompCopy.class, ComponentWrapper.class).addPatterns("[the|a[n]] [item] component copy of %itemcomponents%", "[the] [item] component copies of %itemcomponents%")).supplier(ExprItemCompCopy::new)).build());
    }

    @Override
    @Nullable
    public ComponentWrapper convert(ComponentWrapper wrapper) {
        return wrapper.clone();
    }

    @Override
    public Class<ComponentWrapper> getReturnType() {
        return ComponentWrapper.class;
    }

    @Override
    protected String getPropertyName() {
        return "the item component copies";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"the item component");
        if (this.isSingle()) {
            builder.append((Object)"copy");
        } else {
            builder.append((Object)"copies");
        }
        builder.append("of", this.getExpr());
        return builder.toString();
    }
}

