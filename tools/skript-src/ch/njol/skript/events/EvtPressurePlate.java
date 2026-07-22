/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.Material
 *  org.bukkit.Tag
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public class EvtPressurePlate
extends SkriptEvent {
    private boolean tripwire;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.tripwire = matchedPattern == 1;
        return true;
    }

    @Override
    public boolean check(Event event) {
        Material type;
        PlayerInteractEvent interactEvent = (PlayerInteractEvent)event;
        Block clickedBlock = interactEvent.getClickedBlock();
        Material material = type = clickedBlock == null ? null : clickedBlock.getType();
        if (type == null || interactEvent.getAction() != Action.PHYSICAL) {
            return false;
        }
        if (this.tripwire) {
            return type == Material.TRIPWIRE || type == Material.TRIPWIRE_HOOK;
        }
        return Tag.PRESSURE_PLATES.isTagged((Keyed)type);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.tripwire ? "trip" : "stepping on a pressure plate";
    }

    static {
        Skript.registerEvent("Pressure Plate / Trip", EvtPressurePlate.class, PlayerInteractEvent.class, "[step[ping] on] [a] [pressure] plate", "(trip|[step[ping] on] [a] tripwire)").description("Called when a <i>player</i> steps on a pressure plate or tripwire respectively.").examples("on step on pressure plate:").since("1.0 (pressure plate), 1.4.4 (tripwire)");
    }
}

