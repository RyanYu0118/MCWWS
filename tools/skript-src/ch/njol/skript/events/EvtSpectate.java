/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
 *  com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EvtSpectate
extends SkriptEvent {
    private Literal<EntityData<?>> datas;
    private static final int STOP = -1;
    private static final int SWAP = 0;
    private static final int START = 1;
    private int pattern;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.pattern = matchedPattern - 1;
        this.datas = args[0];
        return true;
    }

    @Override
    public boolean check(Event event) {
        Entity entity;
        boolean swap = false;
        if (this.pattern != -1 && event instanceof PlayerStartSpectatingEntityEvent) {
            PlayerStartSpectatingEntityEvent spectating = (PlayerStartSpectatingEntityEvent)event;
            entity = spectating.getNewSpectatorTarget();
            swap = this.pattern == 0 && entity != null && spectating.getCurrentSpectatorTarget() != null;
            if (swap) {
                entity = spectating.getCurrentSpectatorTarget();
            }
        } else if (event instanceof PlayerStopSpectatingEntityEvent) {
            entity = ((PlayerStopSpectatingEntityEvent)event).getSpectatorTarget();
        } else {
            return false;
        }
        if (this.pattern == 0 && !swap) {
            return false;
        }
        if (this.datas == null) {
            return true;
        }
        for (EntityData data : (EntityData[])this.datas.getAll(event)) {
            if (!data.isInstance(entity)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.pattern == 1 ? "start" : (this.pattern == 0 ? "swap" : "stop")) + " spectating" + (String)(this.datas != null ? "of " + this.datas.toString(event, debug) : "");
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent")) {
            Skript.registerEvent("Spectate", EvtSpectate.class, CollectionUtils.array(PlayerStartSpectatingEntityEvent.class, PlayerStopSpectatingEntityEvent.class), "[player] stop spectating [(of|from) %-*entitydatas%]", "[player] (swap|switch) spectating [(of|from) %-*entitydatas%]", "[player] start spectating [of %-*entitydatas%]").description("Called with a player starts, stops or swaps spectating an entity.").examples("on player start spectating of a zombie:").since("2.7");
        }
    }
}

