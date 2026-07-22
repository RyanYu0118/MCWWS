/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.block.BeaconEffectEvent
 *  org.bukkit.event.Event
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class EvtBeaconEffect
extends SkriptEvent {
    @Nullable
    private Literal<PotionEffectType> potionTypes;
    @Nullable
    private Boolean primaryCheck;

    @Override
    public boolean init(Literal<?>[] exprs, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.potionTypes = exprs[0];
        if (parseResult.hasTag("primary")) {
            this.primaryCheck = true;
        } else if (parseResult.hasTag("secondary")) {
            this.primaryCheck = false;
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof BeaconEffectEvent)) {
            return false;
        }
        BeaconEffectEvent effectEvent = (BeaconEffectEvent)event;
        if (this.primaryCheck != null && effectEvent.isPrimary() != this.primaryCheck.booleanValue()) {
            return false;
        }
        if (this.potionTypes != null) {
            return this.potionTypes.check(event, type -> effectEvent.getEffect().getType() == type);
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.primaryCheck == null ? "" : (this.primaryCheck != false ? "primary " : "secondary ")) + "beacon effect" + (String)(this.potionTypes == null ? "" : " of " + this.potionTypes.toString(event, debug));
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.block.BeaconEffectEvent")) {
            Skript.registerEvent("Beacon Effect", EvtBeaconEffect.class, BeaconEffectEvent.class, "[:primary|:secondary] beacon effect [of %-potioneffecttypes%]", "application of [:primary|:secondary] beacon effect [of %-potioneffecttypes%]", "[:primary|:secondary] beacon effect apply [of %-potioneffecttypes%]").description("Called when a player gets an effect from a beacon.").examples("on beacon effect:", "\tbroadcast applied effect", "\tbroadcast event-player", "\tbroadcast event-block", "on primary beacon effect apply of haste:", "on application of secondary beacon effect:", "on beacon effect of speed:").since("2.10");
        }
    }
}

