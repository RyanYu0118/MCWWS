/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.event.Event
 *  org.bukkit.event.world.PortalCreateEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
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
import java.util.Iterator;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.world.PortalCreateEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Portal")
@Description(value={"The blocks associated with a portal in the portal creation event."})
@Example(value="on portal creation:\n\tloop portal blocks:\n\t\tbroadcast \"%loop-block% is part of a portal!\"\n")
@Since(value={"2.4"})
@Events(value={"portal_create"})
public class ExprPortal
extends SimpleExpression<Block>
implements EventRestrictedSyntax {
    private static final boolean USING_BLOCKSTATE = Skript.isRunningMinecraft(1, 14);

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PortalCreateEvent.class);
    }

    @Nullable
    protected Block[] get(Event e) {
        if (!(e instanceof PortalCreateEvent)) {
            return null;
        }
        List blocks = ((PortalCreateEvent)e).getBlocks();
        if (USING_BLOCKSTATE) {
            return (Block[])blocks.stream().map(block -> ((BlockState)block).getBlock()).toArray(Block[]::new);
        }
        return (Block[])blocks.stream().map(Block.class::cast).toArray(Block[]::new);
    }

    @Override
    @Nullable
    public Iterator<Block> iterator(Event e) {
        if (!(e instanceof PortalCreateEvent)) {
            return null;
        }
        List blocks = ((PortalCreateEvent)e).getBlocks();
        if (USING_BLOCKSTATE) {
            return blocks.stream().map(block -> ((BlockState)block).getBlock()).iterator();
        }
        return blocks.iterator();
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public Class<Block> getReturnType() {
        return Block.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the portal blocks";
    }

    static {
        Skript.registerExpression(ExprPortal.class, Block.class, ExpressionType.SIMPLE, "[the] portal['s] blocks", "[the] blocks of [the] portal");
    }
}

