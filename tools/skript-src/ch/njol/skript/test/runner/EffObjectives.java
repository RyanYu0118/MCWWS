/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Multimap
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.test.runner.TestTracker;
import ch.njol.util.Kleenean;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Objectives")
@Description(value={"An effect to setup required objectives for JUnit tests to complete."})
@NoDoc
public class EffObjectives
extends Effect {
    private static final Multimap<String, String> requirements;
    private static final Multimap<String, String> completeness;
    private Expression<String> junit;
    private Expression<String> objectives;
    private boolean setup;
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.objectives = exprs[matchedPattern ^ 1];
        this.junit = exprs[matchedPattern];
        this.setup = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        String junit = this.junit.getSingle(event);
        if (!$assertionsDisabled && junit == null) {
            throw new AssertionError();
        }
        Object[] objectives = this.objectives.getArray(event);
        if (!$assertionsDisabled && objectives.length <= 0) {
            throw new AssertionError();
        }
        if (this.setup) {
            requirements.putAll((Object)junit, (Iterable)Lists.newArrayList((Object[])objectives));
        } else {
            completeness.putAll((Object)junit, (Iterable)Lists.newArrayList((Object[])objectives));
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.setup) {
            return "ensure junit test " + this.junit.toString(event, debug) + " completes objectives " + this.objectives.toString(event, debug);
        }
        return "complete objectives " + this.objectives.toString(event, debug) + " on junit test " + this.junit.toString(event, debug);
    }

    public static boolean isJUnitComplete() {
        if (!$assertionsDisabled && completeness.isEmpty() && requirements.isEmpty()) {
            throw new AssertionError();
        }
        return completeness.equals(requirements);
    }

    public static void fail() {
        for (String test : requirements.keySet()) {
            if (!completeness.containsKey((Object)test)) {
                TestTracker.JUnitTestFailed("JUnit: '" + test + "'", "didn't complete any objectives.");
                continue;
            }
            ArrayList failures = Lists.newArrayList((Iterable)requirements.get((Object)test));
            failures.removeAll(completeness.get((Object)test));
            if (failures.isEmpty()) continue;
            TestTracker.JUnitTestFailed("JUnit: '" + test + "'", "failed objectives: " + Arrays.toString(failures.toArray(new String[0])));
        }
    }

    static {
        boolean bl = $assertionsDisabled = !EffObjectives.class.desiredAssertionStatus();
        if (TestMode.ENABLED) {
            Skript.registerEffect(EffObjectives.class, "ensure [[junit] test] %string% completes [(objective|trigger)[s]] %strings%", "complete [(objective|trigger)[s]] %strings% (for|on) [[junit] test] %string%");
        }
        requirements = HashMultimap.create();
        completeness = HashMultimap.create();
    }
}

