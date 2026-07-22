/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.server.PaperServerListPingEvent
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.event.server.ServerListPingEvent
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
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Max Players")
@Description(value={"The count of max players. This can be changed in a <a href='#server_list_ping'>server list ping</a> event only.", "'real max players' returns the real count of max players of the server and can be modified on Paper 1.16 or later."})
@Example(value="on server list ping:\n\tset the max players count to (online players count + 1)\n")
@Since(value={"2.3, 2.7 (modify max real players)"})
public class ExprMaxPlayers
extends SimpleExpression<Integer> {
    private boolean isReal;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean isServerPingEvent;
        boolean bl = isServerPingEvent = this.getParser().isCurrentEvent((Class<? extends Event>)ServerListPingEvent.class) || this.getParser().isCurrentEvent((Class<? extends Event>)PaperServerListPingEvent.class);
        if (parseResult.mark == 2 && !isServerPingEvent) {
            Skript.error("The 'shown' max players count expression can't be used outside of a server list ping event");
            return false;
        }
        this.isReal = parseResult.mark == 0 && !isServerPingEvent || parseResult.mark == 1;
        return true;
    }

    @Nullable
    public Integer[] get(Event event) {
        if (!this.isReal && !(event instanceof ServerListPingEvent)) {
            return null;
        }
        if (this.isReal) {
            return CollectionUtils.array(Bukkit.getMaxPlayers());
        }
        return CollectionUtils.array(((ServerListPingEvent)event).getMaxPlayers());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (!this.isReal && this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("Can't change the fake max players count anymore after the server list ping event has already passed");
            return null;
        }
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: {
                return CollectionUtils.array(Number.class);
            }
            case RESET: 
            case DELETE: {
                if (this.isReal) break;
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int amount;
        int n = amount = delta == null ? 0 : ((Number)delta[0]).intValue();
        if (!this.isReal && !(event instanceof ServerListPingEvent)) {
            return;
        }
        if (this.isReal) {
            switch (mode) {
                case SET: {
                    Bukkit.setMaxPlayers((int)amount);
                    break;
                }
                case ADD: {
                    Bukkit.setMaxPlayers((int)(Bukkit.getMaxPlayers() + amount));
                    break;
                }
                case REMOVE: {
                    Bukkit.setMaxPlayers((int)(Bukkit.getMaxPlayers() - amount));
                }
            }
        } else {
            ServerListPingEvent pingEvent = (ServerListPingEvent)event;
            switch (mode) {
                case SET: {
                    pingEvent.setMaxPlayers(amount);
                    break;
                }
                case ADD: {
                    pingEvent.setMaxPlayers(pingEvent.getMaxPlayers() + amount);
                    break;
                }
                case REMOVE: {
                    pingEvent.setMaxPlayers(pingEvent.getMaxPlayers() - amount);
                    break;
                }
                case RESET: 
                case DELETE: {
                    pingEvent.setMaxPlayers(Bukkit.getMaxPlayers());
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the count of " + (this.isReal ? "real max players" : "max players");
    }

    static {
        Skript.registerExpression(ExprMaxPlayers.class, Integer.class, ExpressionType.PROPERTY, "[the] [1:(real|default)|2:(fake|shown|displayed)] max[imum] player[s] [count|amount|number|size]", "[the] [1:(real|default)|2:(fake|shown|displayed)] max[imum] (count|amount|number|size) of players");
    }
}

