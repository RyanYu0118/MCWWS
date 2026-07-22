/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerKickEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.player.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Kick Message")
@Description(value={"The message sent to all online players when a player is kicked from server."})
@Example(value="on kick:\n\tset the kick message to \"%player% was booted from the server! They won't be missed...\"\n")
@Since(value={"2.0 beta 9"})
@Events(value={"kick"})
public class ExprKickMessage
extends SimpleExpression<Component>
implements EventRestrictedSyntax {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprKickMessage.class, Component.class).supplier(ExprKickMessage::new)).priority(SyntaxInfo.SIMPLE)).addPattern("[the] kick( |-)message")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected Component @Nullable [] get(Event event) {
        if (event instanceof PlayerKickEvent) {
            PlayerKickEvent kickEvent = (PlayerKickEvent)event;
            return new Component[]{kickEvent.leaveMessage()};
        }
        return new Component[0];
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("'" + this.toString(null, false) + "' can't be changed after the event has passed");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Component.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (event instanceof PlayerKickEvent) {
            PlayerKickEvent kickEvent = (PlayerKickEvent)event;
            kickEvent.leaveMessage((Component)(delta == null ? Component.empty() : (Component)delta[0]));
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Component> getReturnType() {
        return Component.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the kick message";
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return new Class[]{PlayerKickEvent.class};
    }
}

