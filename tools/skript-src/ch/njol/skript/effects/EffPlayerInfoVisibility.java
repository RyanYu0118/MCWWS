/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.server.PaperServerListPingEvent
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Player Info Visibility")
@Description(value={"Sets whether all player related information is hidden in the server list.", "The Vanilla Minecraft client will display ??? (dark gray) instead of player counts and will not show the", "<a href='#ExprHoverList'>hover hist</a> when hiding player info.", "<a href='#ExprVersionString'>The version string</a> can override the ???.", "Also the <a href='#ExprOnlinePlayersCount'>Online Players Count</a> and", "<a href='#ExprMaxPlayers'>Max Players</a> expressions will return -1 when hiding player info."})
@Example.Examples(value={@Example(value="hide player info"), @Example(value="hide player related information in the server list"), @Example(value="reveal all player related info")})
@Since(value={"2.3"})
@Events(value={"server list ping"})
public class EffPlayerInfoVisibility
extends Effect
implements EventRestrictedSyntax {
    private boolean shouldHide;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (isDelayed == Kleenean.TRUE) {
            Skript.error("Can't change the player info visibility anymore after the server list ping event has already passed");
            return false;
        }
        this.shouldHide = matchedPattern == 0;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PaperServerListPingEvent.class);
    }

    @Override
    protected void execute(Event e) {
        if (!(e instanceof PaperServerListPingEvent)) {
            return;
        }
        ((PaperServerListPingEvent)e).setHidePlayers(this.shouldHide);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.shouldHide ? "hide" : "show") + " player info in the server list";
    }

    static {
        Skript.registerEffect(EffPlayerInfoVisibility.class, "hide [all] player [related] info[rmation] [(in|on|from) [the] server list]", "(show|reveal) [all] player [related] info[rmation] [(in|to|on|from) [the] server list]");
    }
}

