/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityUnleashEvent
 *  org.bukkit.event.entity.EntityUnleashEvent$UnleashReason
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Unleash Reason")
@Description(value={"The unleash reason in an unleash event."})
@Example(value="if the unleash reason is distance:\n\tbroadcast \"The leash was snapped in half.\"\n")
@Events(value={"Leash / Unleash"})
@Since(value={"2.10"})
public class ExprUnleashReason
extends EventValueExpression<EntityUnleashEvent.UnleashReason>
implements EventRestrictedSyntax {
    public ExprUnleashReason() {
        super(EntityUnleashEvent.UnleashReason.class);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityUnleashEvent.class);
    }

    protected EntityUnleashEvent.UnleashReason[] get(Event event) {
        if (!(event instanceof EntityUnleashEvent)) {
            return new EntityUnleashEvent.UnleashReason[0];
        }
        EntityUnleashEvent unleashEvent = (EntityUnleashEvent)event;
        return new EntityUnleashEvent.UnleashReason[]{unleashEvent.getReason()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends EntityUnleashEvent.UnleashReason> getReturnType() {
        return EntityUnleashEvent.UnleashReason.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the unleash reason";
    }

    static {
        Skript.registerExpression(ExprUnleashReason.class, EntityUnleashEvent.UnleashReason.class, ExpressionType.SIMPLE, "[the] unleash[ing] reason");
    }
}

