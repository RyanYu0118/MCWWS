/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.TextDisplay
 */
package org.skriptlang.skript.bukkit.entity.displays.text.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Text Display Visible Through Blocks")
@Description(value={"Returns whether text displays can be seen through blocks or not."})
@Example(value="if last spawned text display is visible through walls:\n\tprevent last spawned text display from being visible through walls\n")
@Since(value={"2.10"})
public class CondTextDisplaySeeThroughBlocks
extends PropertyCondition<Display> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondTextDisplaySeeThroughBlocks.infoBuilder(CondTextDisplaySeeThroughBlocks.class, PropertyCondition.PropertyType.BE, "visible through (blocks|walls)", "displays").supplier(CondTextDisplaySeeThroughBlocks::new).build());
    }

    @Override
    public boolean check(Display value) {
        TextDisplay textDisplay;
        return value instanceof TextDisplay && (textDisplay = (TextDisplay)value).isSeeThrough();
    }

    @Override
    protected String getPropertyName() {
        return "visible through blocks";
    }
}

