/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.chat.ChatRenderer
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.ComponentLike
 *  net.kyori.adventure.text.TextReplacementConfig
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
import ch.njol.skript.effects.EffChange;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParsingStack;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Iterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Chat Format")
@Description(value={"Can be used to modify the chat format.", "The sender of a message is represented by [player] or [sender].", "The message is represented by [message] or [msg]."})
@Example(value="set the chat format to \"<yellow>[player]<light gray>: <green>[message]\"")
@Since(value={"2.2-dev31"})
@Events(value={"chat"})
public class ExprChatFormat
extends SimpleExpression<Component>
implements EventRestrictedSyntax {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprChatFormat.class, Component.class).supplier(ExprChatFormat::new)).addPattern("[the] (message|chat) format[ting]")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Iterator<ParsingStack.Element> stackIterator = this.getParser().getParsingStack().iterator();
        if (!stackIterator.hasNext() || stackIterator.next().getSyntaxElementClass() != EffChange.class) {
            Skript.error("'" + this.toString(null, false) + "' can only be changed, not obtained");
            return false;
        }
        return true;
    }

    protected Component[] get(Event event) {
        this.error("'" + this.toString(null, false) + "' cannot be obtained. Returning <none>.");
        return new Component[0];
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        if (this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("'" + this.toString(null, false) + "' can't be changed after the event has passed");
            return null;
        }
        switch (mode) {
            case SET: 
            case RESET: {
                Class[] classArray2 = new Class[1];
                classArray = classArray2;
                classArray2[0] = Component.class;
                break;
            }
            default: {
                classArray = null;
            }
        }
        return classArray;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof AsyncChatEvent)) {
            return;
        }
        AsyncChatEvent asyncChatEvent = (AsyncChatEvent)event;
        if (delta == null) {
            asyncChatEvent.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> Component.translatable((String)"chat.type.text", (ComponentLike[])new ComponentLike[]{sourceDisplayName, message})));
            return;
        }
        asyncChatEvent.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> ((Component)delta[0]).replaceText((TextReplacementConfig)TextReplacementConfig.builder().match("(?i)\\[(player|sender|message|msg)]").replacement((matchResult, builder) -> matchResult.group(1).startsWith("m") ? message : sourceDisplayName).build())));
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
        return "the chat format";
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return new Class[]{AsyncChatEvent.class};
    }
}

