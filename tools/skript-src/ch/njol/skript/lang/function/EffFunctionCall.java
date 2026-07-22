/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionReference;

public class EffFunctionCall
extends Effect {
    private final FunctionReference<?> reference;

    public EffFunctionCall(FunctionReference<?> reference) {
        this.reference = reference;
    }

    public static EffFunctionCall parse(String line) {
        FunctionReference function = new SkriptParser(line, 3, ParseContext.DEFAULT).parseFunctionReference();
        if (function != null) {
            return new EffFunctionCall(function);
        }
        return null;
    }

    @Override
    protected void execute(Event event) {
        this.reference.execute(event);
        if (this.reference.function() != null) {
            this.reference.function().resetReturnValue();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.reference.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        assert (false);
        return false;
    }
}

