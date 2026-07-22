/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.block.BeaconEffectEvent
 *  org.bukkit.event.Event
 *  org.bukkit.potion.PotionEffectType
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
import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

@Name(value="Applied Beacon Effect")
@Description(value={"The type of effect applied by a beacon."})
@Example(value="on beacon effect:\n\tif the applied effect is primary beacon effect:\n\t\tbroadcast \"Is Primary\"\n\telse if applied effect = secondary effect:\n\t\tbroadcast \"Is Secondary\"\n")
@Events(value={"Beacon Effect"})
@Since(value={"2.10"})
public class ExprAppliedEffect
extends SimpleExpression<PotionEffectType>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(BeaconEffectEvent.class);
    }

    protected PotionEffectType @Nullable [] get(Event event) {
        if (!(event instanceof BeaconEffectEvent)) {
            return null;
        }
        BeaconEffectEvent effectEvent = (BeaconEffectEvent)event;
        return new PotionEffectType[]{effectEvent.getEffect().getType()};
    }

    @Override
    public Class<PotionEffectType> getReturnType() {
        return PotionEffectType.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "applied effect";
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.block.BeaconEffectEvent")) {
            Skript.registerExpression(ExprAppliedEffect.class, PotionEffectType.class, ExpressionType.SIMPLE, "[the] applied [beacon] effect");
        }
    }
}

