/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Check JUnit")
@Description(value={"Returns true if the test runner is currently running a JUnit.", "Useful for the EvtTestCase of JUnit exclusive syntaxes registered from within the test packages."})
@NoDoc
public class CondRunningJUnit
extends Condition {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return TestMode.JUNIT;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "running JUnit";
    }

    static {
        Skript.registerCondition(CondRunningJUnit.class, "running junit");
    }
}

