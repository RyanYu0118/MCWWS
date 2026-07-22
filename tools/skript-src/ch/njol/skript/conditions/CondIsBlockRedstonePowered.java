/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Block Redstone Powered")
@Description(value={"Checks if a block is indirectly or directly powered by redstone"})
@Example.Examples(value={@Example(value="if clicked block is redstone powered:\n\tsend \"This block is well-powered by redstone!\"\n"), @Example(value="if clicked block is indirectly redstone powered:\n\tsend \"This block is indirectly redstone powered.\"\n")})
@Since(value={"2.5"})
public class CondIsBlockRedstonePowered
extends Condition {
    private Expression<Block> blocks;
    private boolean isIndirectlyPowered;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.blocks = exprs[0];
        this.isIndirectlyPowered = matchedPattern % 2 == 1;
        this.setNegated(matchedPattern > 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.isIndirectlyPowered ? this.blocks.check(e, Block::isBlockIndirectlyPowered, this.isNegated()) : this.blocks.check(e, Block::isBlockPowered, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, e, debug, this.blocks, (this.isIndirectlyPowered ? "indirectly " : "") + "powered");
    }

    static {
        Skript.registerCondition(CondIsBlockRedstonePowered.class, "%blocks% (is|are) redstone powered", "%blocks% (is|are) indirectly redstone powered", "%blocks% (is|are)(n't| not) redstone powered", "%blocks% (is|are)(n't| not) indirectly redstone powered");
    }
}

