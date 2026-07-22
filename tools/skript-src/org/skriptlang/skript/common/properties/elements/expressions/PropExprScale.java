/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.common.properties.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RelatedProperty;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyBaseExpression;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Scale")
@Description(value={"Represents the physical size/scale of something.", "For example, the scale of a display entity would be a vector containing multipliers on its size in the x, y, and z axis.", "For a particle effect like the sweeping edge particle, scale is a number determining how large the particle should be."})
@Example.Examples(value={@Example(value="set the scale of {_display} to vector(0,2,0)"), @Example(value="set the scale of {_particle} to 1.5")})
@Since(value={"2.14"})
@RelatedProperty(value="scale")
public class PropExprScale
extends PropertyBaseExpression<ExpressionPropertyHandler<?, ?>> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)PropertyExpression.infoBuilder(PropExprScale.class, Object.class, "scale[s]", "objects", false).supplier(PropExprScale::new)).build());
    }

    @Override
    public Property<ExpressionPropertyHandler<?, ?>> getProperty() {
        return Property.SCALE;
    }
}

