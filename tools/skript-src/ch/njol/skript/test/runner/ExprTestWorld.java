/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
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
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Test World")
@Description(value={"The world the testing is taking place in."})
@Example(value="test \"example\":\n\tspawn zombie at test location\n\tassert last spawned zombie's world is test world with \"zombie did not spawn in test world\"\n")
@NoDoc
public class ExprTestWorld
extends SimpleExpression<World> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected World @Nullable [] get(Event event) {
        return new World[]{SkriptJUnitTest.getTestWorld()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends World> getReturnType() {
        return World.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the test world";
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerExpression(ExprTestWorld.class, World.class, ExpressionType.SIMPLE, "[the] test(-| )world");
        }
    }
}

