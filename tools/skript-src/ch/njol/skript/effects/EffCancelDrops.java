/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockDropItemEvent
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.player.PlayerHarvestBlockEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Cancel Drops")
@Description(value={"Cancels drops of items in a death, block break, block drop, and block harvest events.", "The dropped experience can be cancelled in a death and block break events.", "Please note that using this in a death event doesn't keep items or experience of a dead player. If you want to do that, use the <a href='#EffKeepInventory'>Keep Inventory / Experience</a> effect."})
@Example.Examples(value={@Example(value="on death of a zombie:\n\tif name of the entity is \"&cSpecial\":\n\t\tcancel drops of items\n"), @Example(value="on break of a coal ore:\n\tcancel the experience drops\n"), @Example(value="on player block harvest:\n\tcancel the item drops\n")})
@Since(value={"2.4, 2.12 (harvest event)"})
@RequiredPlugins(value={"1.12.2 or newer (cancelling item drops of blocks)"})
@Events(value={"death", "break / mine", "block drop", "harvest block"})
public class EffCancelDrops
extends Effect
implements EventRestrictedSyntax {
    private boolean cancelItems;
    private boolean cancelExps;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.cancelItems = !parseResult.hasTag("xp");
        boolean bl = this.cancelExps = !parseResult.hasTag("items");
        if (isDelayed.isTrue()) {
            Skript.error("Can't cancel the drops anymore after the event has already passed");
            return false;
        }
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityDeathEvent.class, BlockBreakEvent.class, BlockDropItemEvent.class, PlayerHarvestBlockEvent.class);
    }

    @Override
    protected void execute(Event event) {
        if (event instanceof EntityDeathEvent) {
            EntityDeathEvent deathEvent = (EntityDeathEvent)event;
            if (this.cancelItems) {
                deathEvent.getDrops().clear();
            }
            if (this.cancelExps) {
                deathEvent.setDroppedExp(0);
            }
        } else if (event instanceof BlockBreakEvent) {
            BlockBreakEvent breakEvent = (BlockBreakEvent)event;
            if (this.cancelItems) {
                breakEvent.setDropItems(false);
            }
            if (this.cancelExps) {
                breakEvent.setExpToDrop(0);
            }
        } else if (event instanceof BlockDropItemEvent) {
            BlockDropItemEvent dropItemEvent = (BlockDropItemEvent)event;
            dropItemEvent.getItems().forEach(Entity::remove);
        } else if (event instanceof PlayerHarvestBlockEvent) {
            PlayerHarvestBlockEvent harvestBlockEvent = (PlayerHarvestBlockEvent)event;
            harvestBlockEvent.getItemsHarvested().clear();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.cancelItems && !this.cancelExps) {
            return "cancel the drops of items";
        }
        if (this.cancelExps && !this.cancelItems) {
            return "cancel the drops of experiences";
        }
        return "cancel the drops";
    }

    static {
        Skript.registerEffect(EffCancelDrops.class, "(cancel|clear|delete) [the] drops [of (items:items|xp:[e]xp[erience][s])]", "(cancel|clear|delete) [the] (items:item|xp:[e]xp[erience]) drops");
    }
}

