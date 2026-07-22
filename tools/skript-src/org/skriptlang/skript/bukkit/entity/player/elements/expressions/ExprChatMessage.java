/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.Component
 *  org.bukkit.event.Event
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
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Chat Message")
@Description(value={"The chat message in a chat event."})
@Example(value="on chat:\n\tplayer has permission \"admin\"\n\tset the message to \"<light red>%message%\"\n")
@Since(value={"1.4.6, 2.15 (support for reset)"})
@Events(value={"chat"})
public class ExprChatMessage
extends SimpleExpression<Component>
implements EventRestrictedSyntax {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprChatMessage.class, Component.class).supplier(ExprChatMessage::new)).addPattern("[the] [chat( |-)]message")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected Component[] get(Event event) {
        if (event instanceof AsyncChatEvent) {
            AsyncChatEvent asyncChatEvent = (AsyncChatEvent)event;
            return new Component[]{asyncChatEvent.message()};
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
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Component.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof AsyncChatEvent)) {
            return;
        }
        AsyncChatEvent asyncChatEvent = (AsyncChatEvent)event;
        asyncChatEvent.message((Component)(switch (mode) {
            case Changer.ChangeMode.SET -> {
                if (!$assertionsDisabled && delta == null) {
                    throw new AssertionError();
                }
                yield (Component)delta[0];
            }
            case Changer.ChangeMode.DELETE -> Component.empty();
            case Changer.ChangeMode.RESET -> asyncChatEvent.originalMessage();
            default -> throw new IllegalStateException("Unexpected change mode: " + String.valueOf((Object)mode));
        }));
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
        return "the chat message";
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return new Class[]{AsyncChatEvent.class};
    }
}

