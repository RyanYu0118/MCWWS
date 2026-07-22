/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Bed/Anchor Spawn")
@Description(value={"Checks what the respawn location of a player in the respawn event is."})
@Example(value="on respawn:\n\tthe respawn location is a bed\n\tbroadcast \"%player% is respawning in their bed! So cozy!\"\n")
@RequiredPlugins(value={"Minecraft 1.16+"})
@Since(value={"2.7"})
@Events(value={"respawn"})
public class CondRespawnLocation
extends Condition
implements EventRestrictedSyntax {
    private boolean bedSpawn;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(parseResult.mark == 1);
        this.bedSpawn = parseResult.hasTag("bed");
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerRespawnEvent.class);
    }

    @Override
    public boolean check(Event event) {
        if (event instanceof PlayerRespawnEvent) {
            PlayerRespawnEvent respawnEvent = (PlayerRespawnEvent)event;
            return (this.bedSpawn ? respawnEvent.isBedSpawn() : respawnEvent.isAnchorSpawn()) != this.isNegated();
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the respawn location " + (this.isNegated() ? "isn't" : "is") + " a " + (this.bedSpawn ? "bed spawn" : "respawn anchor");
    }

    static {
        Skript.registerCondition(CondRespawnLocation.class, "[the] respawn location (was|is)[1:(n'| no)t] [a] (:bed|respawn anchor)");
    }
}

