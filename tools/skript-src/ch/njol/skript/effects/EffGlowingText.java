/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Sign
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.ItemMeta
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
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Sign Glow")
@Description(value={"Makes a sign (either a block or item) have glowing text or normal text"})
@Example(value="make target block of player have glowing text")
@Since(value={"2.8.0"})
public class EffGlowingText
extends Effect {
    private Expression<?> objects;
    private boolean glowing;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.objects = exprs[0];
        this.glowing = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Object obj : this.objects.getArray(event)) {
            if (obj instanceof Block) {
                BlockState state = ((Block)obj).getState();
                if (!(state instanceof Sign)) continue;
                ((Sign)state).setGlowingText(this.glowing);
                state.update();
                continue;
            }
            if (!(obj instanceof ItemType)) continue;
            ItemType item = (ItemType)obj;
            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof BlockStateMeta)) {
                return;
            }
            BlockStateMeta blockMeta = (BlockStateMeta)meta;
            BlockState state = blockMeta.getBlockState();
            if (!(state instanceof Sign)) {
                return;
            }
            ((Sign)state).setGlowingText(this.glowing);
            state.update();
            blockMeta.setBlockState(state);
            item.setItemMeta(meta);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.objects.toString(event, debug) + " have " + (this.glowing ? "glowing text" : "normal text");
    }

    static {
        if (Skript.methodExists(Sign.class, "setGlowingText", Boolean.TYPE)) {
            Skript.registerEffect(EffGlowingText.class, "make %blocks/itemtypes% have glowing text", "make %blocks/itemtypes% have (normal|non[-| ]glowing) text");
        }
    }
}

