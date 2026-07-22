/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.ContextlessEvent;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.script.ScriptWarning;

public class SimplifiedCondition
extends Condition {
    private final Condition source;
    private final boolean result;

    public static Condition fromCondition(Condition original) {
        return SimplifiedCondition.fromCondition(original, !TestMode.ENABLED || TestMode.DEV_MODE);
    }

    public static Condition fromCondition(Condition original, boolean warn) {
        if (original instanceof SimplifiedCondition) {
            SimplifiedCondition simplifiedCondition = (SimplifiedCondition)original;
            return simplifiedCondition;
        }
        ContextlessEvent event = ContextlessEvent.get();
        boolean result = original.check(event);
        if (warn) {
            Script script;
            ParserInstance parser = ParserInstance.get();
            Script script2 = script = parser.isActive() ? parser.getCurrentScript() : null;
            if (script != null && !script.suppressesWarning(ScriptWarning.CONSTANT_CONDITION)) {
                Skript.warning("The condition '" + original.toString(event, Skript.debug()) + "' will always be " + (result ? "true" : "false") + ".");
            }
        }
        return new SimplifiedCondition(original, result);
    }

    private SimplifiedCondition(Condition source, boolean result) {
        this.source = source;
        this.result = result;
    }

    public Condition getSource() {
        return this.source;
    }

    public boolean getResult() {
        return this.result;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean check(Event event) {
        return this.result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.source.toString(event, debug);
    }
}

