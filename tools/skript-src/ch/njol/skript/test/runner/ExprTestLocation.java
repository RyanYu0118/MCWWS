/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.test.runner.SkriptJUnitTest;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Test Location")
@Description(value={"The location the testing is taking place at."})
@Example(value="test \"example\":\n\tspawn zombie at test location\n\tassert last spawned zombie's world is test world with \"zombie did not spawn in test world\"\n")
@NoDoc
public class ExprTestLocation
extends SimpleExpression<Location> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected Location @Nullable [] get(Event event) {
        return new Location[]{SkriptJUnitTest.getTestLocation()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the test location";
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerExpression(ExprTestLocation.class, Location.class, ExpressionType.SIMPLE, "[the] test(-| )location");
        }
    }
}

