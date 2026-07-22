/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityExplodeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Exploded Blocks")
@Description(value={"Get all the blocks that were destroyed in an explode event. Supports add/remove/set/clear/delete blocks."})
@Example.Examples(value={@Example(value="on explode:\n\tloop exploded blocks:\n\t\tadd loop-block to {exploded::blocks::*}\n"), @Example(value="on explode:\n\tloop exploded blocks:\n\t\tif loop-block is grass:\n\t\t\tremove loop-block from exploded blocks\n"), @Example(value="on explode:\n\tclear exploded blocks\n"), @Example(value="on explode:\n\tset exploded blocks to blocks in radius 10 around event-entity\n"), @Example(value="on explode:\n\tadd blocks above event-entity to exploded blocks\n")})
@Events(value={"explode"})
@Since(value={"2.5, 2.8.6 (modify blocks)"})
public class ExprExplodedBlocks
extends SimpleExpression<Block>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityExplodeEvent.class);
    }

    @Nullable
    protected Block[] get(Event e) {
        if (!(e instanceof EntityExplodeEvent)) {
            return null;
        }
        List blockList = ((EntityExplodeEvent)e).blockList();
        return blockList.toArray(new Block[blockList.size()]);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD: 
            case REMOVE: 
            case SET: 
            case DELETE: {
                return CollectionUtils.array(Block[].class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(event instanceof EntityExplodeEvent)) {
            return;
        }
        List blocks = ((EntityExplodeEvent)event).blockList();
        switch (mode) {
            case DELETE: {
                blocks.clear();
                break;
            }
            case SET: {
                blocks.clear();
            }
            case ADD: {
                for (Object object : delta) {
                    if (!(object instanceof Block)) continue;
                    blocks.add((Block)object);
                }
                break;
            }
            case REMOVE: {
                for (Object object : delta) {
                    if (!(object instanceof Block)) continue;
                    blocks.remove((Block)object);
                }
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean d) {
        return "exploded blocks";
    }

    static {
        Skript.registerExpression(ExprExplodedBlocks.class, Block.class, ExpressionType.COMBINED, "[the] exploded blocks");
    }
}

