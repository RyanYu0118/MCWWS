/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.Locale;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@NoDoc
public class CondHasDelayBefore
extends Condition {
    private Kleenean expected;
    private boolean success;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(parseResult.hasTag("negated"));
        if (parseResult.hasTag("true")) {
            this.expected = Kleenean.TRUE;
        } else if (parseResult.hasTag("false")) {
            this.expected = Kleenean.FALSE;
        } else if (parseResult.hasTag("unknown")) {
            this.expected = Kleenean.UNKNOWN;
        } else {
            throw new IllegalStateException("missing kleenean type parse tag");
        }
        this.success = this.getParser().getHasDelayBefore() == this.expected ^ this.isNegated();
        return !parseResult.hasTag("init") || this.success;
    }

    @Override
    public boolean check(Event event) {
        return this.success;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "has delay before is " + (this.isNegated() ? "not " : "") + this.expected.name().toLowerCase(Locale.ENGLISH);
    }

    static {
        Skript.registerCondition(CondHasDelayBefore.class, "has delay before is[negated: not|negated:n't] (:true|:false|:unknown) [init:failing if wrong]");
    }
}

