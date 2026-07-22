/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.audience.Audience
 *  org.bukkit.Bukkit
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
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Chat Recipients")
@Description(value={"The recipients of a chat event"})
@Example(value="chat recipients")
@Since(value={"2.2-Fixes-v7, 2.2-dev35 (clearing recipients), 2.15 (returns Audience)"})
@Events(value={"chat"})
public class ExprChatRecipients
extends SimpleExpression<Audience>
implements EventRestrictedSyntax {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprChatRecipients.class, Audience.class).supplier(ExprChatRecipients::new)).addPattern("[the] [chat( | -)]recipients")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    protected Audience[] get(Event event) {
        if (event instanceof AsyncChatEvent) {
            AsyncChatEvent asyncChatEvent = (AsyncChatEvent)event;
            return asyncChatEvent.viewers().toArray(new Audience[0]);
        }
        return new Audience[0];
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("'" + this.toString(null, false) + "' can't be changed after the event has passed");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.REMOVE, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Audience[].class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof AsyncChatEvent)) {
            return;
        }
        AsyncChatEvent asyncChatEvent = (AsyncChatEvent)event;
        switch (mode) {
            case ADD: {
                assert (delta != null);
                for (Object audience : delta) {
                    asyncChatEvent.viewers().add((Audience)audience);
                }
                break;
            }
            case SET: {
                assert (delta != null);
                asyncChatEvent.viewers().clear();
                for (Object audience : delta) {
                    asyncChatEvent.viewers().add((Audience)audience);
                }
                break;
            }
            case REMOVE: {
                assert (delta != null);
                for (Object audience : delta) {
                    asyncChatEvent.viewers().remove((Audience)audience);
                }
                break;
            }
            case DELETE: {
                asyncChatEvent.viewers().clear();
                break;
            }
            case RESET: {
                asyncChatEvent.viewers().clear();
                asyncChatEvent.viewers().addAll(Bukkit.getOnlinePlayers());
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<Audience> getReturnType() {
        return Audience.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the chat recipients";
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return new Class[]{AsyncChatEvent.class};
    }
}

