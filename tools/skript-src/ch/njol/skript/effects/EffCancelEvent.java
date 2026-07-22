/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.Event$Result
 *  org.bukkit.event.block.BlockCanBuildEvent
 *  org.bukkit.event.entity.EntityToggleSwimEvent
 *  org.bukkit.event.inventory.InventoryInteractEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.PlayerUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.events.EvtClick;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Cancel Event")
@Description(value={"Cancels the event (e.g. prevent blocks from being placed, or damage being taken)."})
@Example(value="on damage:\n\tvictim is a player\n\tvictim has the permission \"skript.god\"\n\tcancel the event\n")
@Since(value={"1.0"})
public class EffCancelEvent
extends Effect {
    private boolean cancel;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (isDelayed == Kleenean.TRUE) {
            Skript.error("An event cannot be cancelled after it has already passed", ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        this.cancel = matchedPattern == 0;
        Class<? extends Event>[] currentEvents = this.getParser().getCurrentEvents();
        if (currentEvents == null) {
            return false;
        }
        if (this.cancel && this.getParser().isCurrentEvent((Class<? extends Event>)EntityToggleSwimEvent.class)) {
            Skript.error("Cancelling a toggle swim event has no effect");
            return false;
        }
        for (Class<? extends Event> event : currentEvents) {
            if (!Cancellable.class.isAssignableFrom(event) && !BlockCanBuildEvent.class.isAssignableFrom(event)) continue;
            return true;
        }
        if (this.getParser().isCurrentEvent((Class<? extends Event>)PlayerLoginEvent.class)) {
            Skript.error("A connect event cannot be cancelled, but the player may be kicked ('kick player by reason of \"...\"')", ErrorQuality.SEMANTIC_ERROR);
        } else {
            Skript.error(Utils.A(this.getParser().getCurrentEventName()) + " event cannot be cancelled", ErrorQuality.SEMANTIC_ERROR);
        }
        return false;
    }

    @Override
    public void execute(Event event) {
        if (event instanceof Cancellable) {
            Cancellable cancellable = (Cancellable)event;
            cancellable.setCancelled(this.cancel);
        }
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent playerInteractEvent = (PlayerInteractEvent)event;
            EvtClick.interactTracker.eventModified((Cancellable)event);
            playerInteractEvent.setUseItemInHand(this.cancel ? Event.Result.DENY : Event.Result.DEFAULT);
            playerInteractEvent.setUseInteractedBlock(this.cancel ? Event.Result.DENY : Event.Result.DEFAULT);
        } else if (event instanceof PlayerInteractEntityEvent) {
            EvtClick.interactTracker.eventModified((Cancellable)event);
        } else if (event instanceof BlockCanBuildEvent) {
            BlockCanBuildEvent blockCanBuildEvent = (BlockCanBuildEvent)event;
            blockCanBuildEvent.setBuildable(!this.cancel);
        } else if (event instanceof PlayerDropItemEvent) {
            PlayerDropItemEvent playerDropItemEvent = (PlayerDropItemEvent)event;
            PlayerUtils.updateInventory(playerDropItemEvent.getPlayer());
        } else if (event instanceof InventoryInteractEvent) {
            InventoryInteractEvent interactEvent = (InventoryInteractEvent)event;
            PlayerUtils.updateInventory((Player)interactEvent.getWhoClicked());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.cancel ? "" : "un") + "cancel event";
    }

    static {
        Skript.registerEffect(EffCancelEvent.class, "cancel [the] event", "uncancel [the] event");
    }
}

