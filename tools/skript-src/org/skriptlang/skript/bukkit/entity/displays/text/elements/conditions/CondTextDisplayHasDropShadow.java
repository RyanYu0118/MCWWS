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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Text Display Has Drop Shadow")
@Description(value={"Returns whether the text of a display has drop shadow applied."})
@Example(value="if {_display} has drop shadow:\n\tremove drop shadow from the text of {_display}\n")
@Since(value={"2.10"})
public class CondTextDisplayHasDropShadow
extends PropertyCondition<Display> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(CondTextDisplayHasDropShadow.class).addPatterns("[[the] text of] %displays% (has|have) [a] (drop|text) shadow", "%displays%'[s] text (has|have) [a] (drop|text) shadow", "[[the] text of] %displays% (doesn't|does not|do not|don't) have [a] (drop|text) shadow", "%displays%'[s] text (doesn't|does not|do not|don't) have [a] (drop|text) shadow").supplier(CondTextDisplayHasDropShadow::new).priority(DEFAULT_PRIORITY).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!super.init(expressions, matchedPattern, isDelayed, parseResult)) {
            return false;
        }
        this.setNegated(matchedPattern > 1);
        return true;
    }

    @Override
    public boolean check(Display value) {
        TextDisplay textDisplay;
        return value instanceof TextDisplay && (textDisplay = (TextDisplay)value).isShadowed();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "drop shadow";
    }
}

