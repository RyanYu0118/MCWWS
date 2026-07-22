/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockGrowEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockGrowEvent;
import org.jetbrains.annotations.Nullable;

public class EvtPlantGrowth
extends SkriptEvent {
    @Nullable
    private Literal<ItemType> types;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.types = args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (this.types != null) {
            for (ItemType type : this.types.getAll()) {
                if (!new ItemType(((BlockGrowEvent)e).getBlock()).equals(type)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "plant growth";
    }

    static {
        Skript.registerEvent("Block Growth", EvtPlantGrowth.class, BlockGrowEvent.class, "(plant|crop|block) grow[(th|ing)] [[of] %-itemtypes%]").description("Called when a crop grows. Alternative to new form of generic grow event.").examples("on crop growth:").since("2.2-Fixes-V10");
    }
}

