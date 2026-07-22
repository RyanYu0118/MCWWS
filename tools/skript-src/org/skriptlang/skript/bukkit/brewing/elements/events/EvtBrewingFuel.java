/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.BrewingStandFuelEvent
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.brewing.elements.events;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.EventValues;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EvtBrewingFuel
extends SkriptEvent {
    private Literal<ItemType> items;

    public static void register(SyntaxRegistry registry) {
        registry.register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(EvtBrewingFuel.class, "Brewing Fuel").addEvent(BrewingStandFuelEvent.class).addPatterns("brew[ing [stand]] consum(e|ing) fuel [of %-itemtypes%]", "brew[ing [stand]] fuel consumption [of %-itemtypes%]")).addDescription("Called when a brewing stand is about to use an item to increase its fuel level.").addExample("on brewing consume fuel:\n\tprevent the brewing stand from consuming fuel\non brewing fuel consumption of blaze powder:\n").addSince("2.13").supplier(EvtBrewingFuel::new)).build());
        EventValues.registerEventValue(BrewingStandFuelEvent.class, ItemStack.class, BrewingStandFuelEvent::getFuel);
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.items = args[0];
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof BrewingStandFuelEvent)) {
            return false;
        }
        BrewingStandFuelEvent brewingStandFuelEvent = (BrewingStandFuelEvent)event;
        if (this.items == null) {
            return true;
        }
        ItemStack itemStack = brewingStandFuelEvent.getFuel();
        for (ItemType itemType : this.items.getArray()) {
            if (!itemType.isOfType(itemStack)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"brewing stand fuel consumption");
        if (this.items != null) {
            builder.append("of", this.items);
        }
        return builder.toString();
    }
}

