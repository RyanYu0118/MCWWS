/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.server.PaperServerListPingEvent
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Online Player Count")
@Description(value={"The amount of online players. This can be changed in a <a href='#server_list_ping'>server list ping</a> event only to show fake online player amount.", "<code>real online player count</code> always return the real count of online players and can't be changed."})
@Example(value="on server list ping:\n\t# This will make the max players count 5 if there are 4 players online.\n\tset the fake max players count to (online player count + 1)\n")
@Since(value={"2.3"})
public class ExprOnlinePlayersCount
extends SimpleExpression<Long> {
    private boolean isReal;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean isListPingEvent = this.getParser().isCurrentEvent((Class<? extends Event>)PaperServerListPingEvent.class);
        if (parseResult.mark == 2 && !isListPingEvent) {
            Skript.error("The 'fake' online players count expression can't be used outside of a server list ping event");
            return false;
        }
        this.isReal = parseResult.mark == 0 && !isListPingEvent || parseResult.mark == 1;
        return true;
    }

    @Nullable
    public Long[] get(Event e) {
        if (!this.isReal && !(e instanceof PaperServerListPingEvent)) {
            return null;
        }
        if (this.isReal) {
            return CollectionUtils.array(Bukkit.getOnlinePlayers().size());
        }
        return CollectionUtils.array(((PaperServerListPingEvent)e).getNumPlayers());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (!this.isReal) {
            if (this.getParser().getHasDelayBefore().isTrue()) {
                Skript.error("Can't change the shown online players count anymore after the server list ping event has already passed");
                return null;
            }
            switch (mode) {
                case SET: 
                case ADD: 
                case REMOVE: 
                case DELETE: 
                case RESET: {
                    return CollectionUtils.array(Number.class);
                }
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(e instanceof PaperServerListPingEvent)) {
            return;
        }
        PaperServerListPingEvent event = (PaperServerListPingEvent)e;
        switch (mode) {
            case SET: {
                event.setNumPlayers(((Number)delta[0]).intValue());
                break;
            }
            case ADD: {
                event.setNumPlayers(event.getNumPlayers() + ((Number)delta[0]).intValue());
                break;
            }
            case REMOVE: {
                event.setNumPlayers(event.getNumPlayers() - ((Number)delta[0]).intValue());
                break;
            }
            case DELETE: 
            case RESET: {
                event.setNumPlayers(Bukkit.getOnlinePlayers().size());
            }
        }
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
        return "the count of " + (this.isReal ? "real max players" : "max players");
    }

    static {
        Skript.registerExpression(ExprOnlinePlayersCount.class, Long.class, ExpressionType.PROPERTY, "[the] [(1:(real|default)|2:(fake|shown|displayed))] [online] player (count|amount|number)", "[the] [(1:(real|default)|2:(fake|shown|displayed))] (count|amount|number|size) of online players");
    }
}

