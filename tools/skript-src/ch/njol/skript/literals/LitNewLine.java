/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.literals;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="New Line")
@Description(value={"Returns a line break separator."})
@Example(value="send \"Hello%nl%Goodbye!\" to player")
@Since(value={"2.5"})
public class LitNewLine
extends SimpleLiteral<String> {
    public LitNewLine() {
        super("\n", false);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult result) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "new line";
    }

    static {
        Skript.registerExpression(LitNewLine.class, String.class, ExpressionType.SIMPLE, "nl", "new[ ]line", "line[ ]break");
    }
}

