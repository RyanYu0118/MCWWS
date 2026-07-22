/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Readied Arrow/Bow")
@Description(value={"The bow or arrow in a <a href='#ready_arrow'>Ready Arrow event</a>."})
@Example(value="on player ready arrow:\n\tselected bow's name is \"Spectral Bow\"\n\tif selected arrow is not a spectral arrow:\n\t\tcancel event\n")
@Since(value={"2.8.0"})
@Events(value={"ready arrow"})
public class ExprReadiedArrow
extends SimpleExpression<ItemStack> {
    private boolean isArrow;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isArrow = parseResult.hasTag("arrow");
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerReadyArrowEvent.class)) {
            Skript.error("'the readied " + (this.isArrow ? "arrow" : "bow") + "' can only be used in a ready arrow event");
            return false;
        }
        return true;
    }

    @Nullable
    protected ItemStack[] get(Event event) {
        if (!(event instanceof PlayerReadyArrowEvent)) {
            return null;
        }
        if (this.isArrow) {
            return new ItemStack[]{((PlayerReadyArrowEvent)event).getArrow()};
        }
        return new ItemStack[]{((PlayerReadyArrowEvent)event).getBow()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the readied " + (this.isArrow ? "arrow" : "bow");
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.player.PlayerReadyArrowEvent")) {
            Skript.registerExpression(ExprReadiedArrow.class, ItemStack.class, ExpressionType.SIMPLE, "[the] (readied|selected|drawn) (:arrow|bow)");
        }
    }
}

