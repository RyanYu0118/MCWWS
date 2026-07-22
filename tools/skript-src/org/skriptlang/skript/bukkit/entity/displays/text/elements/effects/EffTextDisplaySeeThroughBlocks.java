/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.displays.text.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Text Display See Through Blocks")
@Description(value={"Forces a text display to either be or not be visible through blocks."})
@Example.Examples(value={@Example(value="force last spawned text display to be visible through walls"), @Example(value="prevent all text displays from being visible through walls")})
@Since(value={"2.10"})
public class EffTextDisplaySeeThroughBlocks
extends Effect {
    Expression<Display> displays;
    boolean canSee;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffTextDisplaySeeThroughBlocks.class).addPatterns("make %displays% visible through (blocks|walls)", "force %displays% to be visible through (blocks|walls)", "(prevent|block) %displays% from being (visible|seen) through (blocks|walls)").supplier(EffTextDisplaySeeThroughBlocks::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.displays = expressions[0];
        this.canSee = matchedPattern != 2;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Display display : this.displays.getArray(event)) {
            if (!(display instanceof TextDisplay)) continue;
            TextDisplay textDisplay = (TextDisplay)display;
            textDisplay.setSeeThrough(this.canSee);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.canSee) {
            return "force " + this.displays.toString(event, debug) + " to be visible through blocks";
        }
        return "prevent " + this.displays.toString(event, debug) + " from being visible through blocks";
    }
}

