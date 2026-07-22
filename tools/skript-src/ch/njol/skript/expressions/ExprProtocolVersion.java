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

@Name(value="Protocol Version")
@Description(value={"The protocol version that will be sent as the protocol version of the server in a server list ping event. For more information and list of protocol versions <a href='https://wiki.vg/Protocol_version_numbers'>visit wiki.vg</a>.", "If this protocol version doesn't match with the protocol version of the client, the client will see the <a href='#ExprVersionString'>version string</a>.", "But please note that, this expression has no visual effect over the version string. For example if the server uses PaperSpigot 1.12.2, and you make the protocol version 107 (1.9),", "the version string will not be \"Paper 1.9\", it will still be \"Paper 1.12.2\".", "But then you can customize the <a href='#ExprVersionString'>version string</a> as you wish.", "Also if the protocol version of the player is higher than protocol version of the server, it will say", "\"Server out of date!\", and if vice-versa \"Client out of date!\" when you hover on the ping bars.", "", "This can be set in a <a href='#server_list_ping'>server list ping</a> event only", "(increase and decrease effects cannot be used because that wouldn't make sense)."})
@Example(value="on server list ping:\n\tset the version string to \"<light green>Version: <orange>%minecraft version%\"\n\tset the protocol version to 0 # 13w41a (1.7) - so the player will see the custom version string almost always\n")
@Since(value={"2.3"})
@Events(value={"server list ping"})
public class ExprProtocolVersion
extends SimpleExpression<Long>
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
    public Long[] get(Event e) {
        if (!(e instanceof PaperServerListPingEvent)) {
            return null;
        }
        return CollectionUtils.array(((PaperServerListPingEvent)e).getProtocolVersion());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("Can't change the protocol version anymore after the server list ping event has already passed");
            return null;
        }
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Number.class);
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(e instanceof PaperServerListPingEvent)) {
            return;
        }
        ((PaperServerListPingEvent)e).setProtocolVersion(((Number)delta[0]).intValue());
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the protocol version";
    }

    static {
        Skript.registerExpression(ExprProtocolVersion.class, Long.class, ExpressionType.SIMPLE, "[the] [server] [(sent|required|fake)] protocol version [number]");
    }
}

