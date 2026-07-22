/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Break Block")
@Description(value={"Breaks the block and spawns items as if a player had mined it", "\nYou can add a tool, which will spawn items based on how that tool would break the block ", "(ie: When using a hand to break stone, it drops nothing, whereas with a pickaxe it drops cobblestone)"})
@Example.Examples(value={@Example(value="on right click:\n\tbreak clicked block naturally\n"), @Example(value="loop blocks in radius 10 around player:\n\tbreak loop-block using player's tool\n"), @Example(value="loop blocks in radius 10 around player:\n\tbreak loop-block naturally using diamond pickaxe\n")})
@Since(value={"2.4"})
public class EffBreakNaturally
extends Effect {
    private Expression<Block> blocks;
    @Nullable
    private Expression<ItemType> tool;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.blocks = exprs[0];
        this.tool = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        ItemType tool = this.tool != null ? this.tool.getSingle(e) : null;
        for (Block block : this.blocks.getArray(e)) {
            if (tool != null) {
                ItemStack is = tool.getRandom();
                if (is != null) {
                    block.breakNaturally(is);
                    continue;
                }
                block.breakNaturally();
                continue;
            }
            block.breakNaturally();
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "break " + this.blocks.toString(e, debug) + " naturally" + (String)(this.tool != null ? " using " + this.tool.toString(e, debug) : "");
    }

    static {
        Skript.registerEffect(EffBreakNaturally.class, "break %blocks% [naturally] [using %-itemtype%]");
    }
}

