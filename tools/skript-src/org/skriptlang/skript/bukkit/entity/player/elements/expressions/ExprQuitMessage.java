/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerQuitEvent
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Quit Message")
@Description(value={"The message sent to all online players when a player quits the server."})
@Example(value="on quit:\n\tif {vanish::%player's uuid%} is set:\n\t\tclear the quit message\n\telse:\n\t\tset the quit message to \"%player% left this server... they will be missed!\"\n")
@Since(value={"1.4.9"})
@Events(value={"quit"})
public class ExprQuitMessage
extends SimpleExpression<Component>
implements EventRestrictedSyntax {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprQuitMessage.class, Component.class).supplier(ExprQuitMessage::new)).priority(SyntaxInfo.SIMPLE)).addPattern("[the] (quit|leave|log[ ]out)( |-)message")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected Component @Nullable [] get(Event event) {
        PlayerQuitEvent quitEvent;
        Component message;
        if (event instanceof PlayerQuitEvent && (message = (quitEvent = (PlayerQuitEvent)event).quitMessage()) != null) {
            return new Component[]{message};
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
        if (event instanceof PlayerQuitEvent) {
            PlayerQuitEvent quitEvent = (PlayerQuitEvent)event;
            quitEvent.quitMessage((Component)(delta == null ? Component.empty() : (Component)delta[0]));
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
        return "the quit message";
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return new Class[]{PlayerQuitEvent.class};
    }
}

