/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
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
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Test Block")
@Description(value={"The block the testing is taking place at."})
@Example(value="test \"example\":\n\tset test block to air\n")
@NoDoc
public class ExprTestBlock
extends SimpleExpression<Block> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected Block @Nullable [] get(Event event) {
        return new Block[]{SkriptJUnitTest.getBlock()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the test block";
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerExpression(ExprTestBlock.class, Block.class, ExpressionType.SIMPLE, "[the] test(-| )block");
        }
    }
}

