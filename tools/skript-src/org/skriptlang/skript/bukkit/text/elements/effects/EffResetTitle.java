/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.audience.Audience
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.text.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Title - Clear/Reset")
@Description(value={"Clears or resets the title of an audience to the default values.", "While both actions remove the title being displayed, <code>reset</code> will also reset the title timings."})
@Example.Examples(value={@Example(value="reset the titles of all players"), @Example(value="clear the title")})
@Since(value={"2.3, 2.15 (clearing the title)"})
public class EffResetTitle
extends Effect {
    private Expression<Audience> audiences;
    private boolean reset;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffResetTitle.class).supplier(EffResetTitle::new).addPatterns("(clear|delete|:reset) [the] title[s] [of %audiences%]", "(clear|delete|:reset) [the] %audiences%'[s] title[s]").build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.audiences = exprs[0];
        this.reset = parseResult.hasTag("reset");
        return true;
    }

    @Override
    protected void execute(Event event) {
        Audience audience = Audience.audience((Audience[])this.audiences.getArray(event));
        if (this.reset) {
            audience.resetTitle();
        } else {
            audience.clearTitle();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.reset) {
            builder.append((Object)"reset");
        } else {
            builder.append((Object)"clear");
        }
        builder.append((Object)"the");
        if (this.audiences.isSingle()) {
            builder.append((Object)"title");
        } else {
            builder.append((Object)"titles");
        }
        builder.append("of", this.audiences);
        return builder.toString();
    }
}

