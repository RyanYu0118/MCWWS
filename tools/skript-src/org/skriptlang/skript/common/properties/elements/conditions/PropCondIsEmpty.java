/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.common.properties.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RelatedProperty;
import ch.njol.skript.doc.Since;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyBaseCondition;
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Is Empty")
@Description(value={"Checks whether something is empty."})
@Example(value="player's inventory is empty")
@Since(value={"unknown (before 2.1)"})
@RelatedProperty(value="empty")
public class PropCondIsEmpty
extends PropertyBaseCondition<ConditionPropertyHandler<?>> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, PropertyCondition.infoBuilder(PropCondIsEmpty.class, PropertyCondition.PropertyType.BE, "empty", "objects").supplier(PropCondIsEmpty::new).build());
    }

    @Override
    @NotNull
    public Property<ConditionPropertyHandler<?>> getProperty() {
        return Property.IS_EMPTY;
    }
}

