/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.EntityBlockStorage
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EntityBlockStorage;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Clear Entity Storage")
@Description(value={"Clear the stored entities of an entity block storage (i.e. beehive)."})
@Example(value="clear the stored entities of {_beehive}")
@Since(value={"2.11"})
public class EffClearEntityStorage
extends Effect {
    private Expression<Block> blocks;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.blocks = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Block block : this.blocks.getArray(event)) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof EntityBlockStorage)) continue;
            EntityBlockStorage blockStorage = (EntityBlockStorage)blockState;
            blockStorage.clearEntities();
            blockStorage.update(true, false);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "clear the stored entities of " + this.blocks.toString(event, debug);
    }

    static {
        if (Skript.methodExists(EntityBlockStorage.class, "clearEntities", new Class[0])) {
            Skript.registerEffect(EffClearEntityStorage.class, "(clear|empty) the (stored entities|entity storage) of %blocks%");
        }
    }
}

