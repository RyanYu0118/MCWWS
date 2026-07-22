/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.server.PaperServerListPingEvent
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
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
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Version String")
@Description(value={"The text to show if the protocol version of the server doesn't match with protocol version of the client. You can check the <a href='#ExprProtocolVersion'>protocol version</a> expression for more information about this.", "This can only be set in a <a href='#server_list_ping'>server list ping</a> event."})
@Example(value="on server list ping:\n\tset the protocol version to 0 # 13w41a (1.7), so it will show the version string always\n\tset the version string to \"&lt;light green&gt;Version: &lt;orange&gt;%minecraft version%\"\n")
@Since(value={"2.3"})
@Events(value={"Server List Ping"})
public class ExprVersionString
extends SimpleExpression<String>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PaperServerListPingEvent.class);
    }

    @Nullable
    public String[] get(Event event) {
        if (!(event instanceof PaperServerListPingEvent)) {
            return new String[0];
        }
        return CollectionUtils.array(((PaperServerListPingEvent)event).getVersion());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("Can't change the version string anymore after the server list ping event has already passed");
            return null;
        }
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(String.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof PaperServerListPingEvent)) {
            return;
        }
        ((PaperServerListPingEvent)event).setVersion((String)delta[0]);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the version string";
    }

    static {
        Skript.registerExpression(ExprVersionString.class, String.class, ExpressionType.SIMPLE, "[the] [shown|custom] version [string|text]");
    }
}

