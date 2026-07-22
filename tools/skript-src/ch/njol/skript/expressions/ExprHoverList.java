/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.server.PaperServerListPingEvent
 *  com.destroystokyo.paper.event.server.PaperServerListPingEvent$ListedPlayerInfo
 *  net.kyori.adventure.text.Component
 *  org.bukkit.entity.Player
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;

@Name(value="Hover List")
@Description(value={"The list when you hover on the player counts of the server in the server list.", "This can be changed using texts or players in a <a href='#server_list_ping'>server list ping</a> event only. Adding players to the list means adding the name of the players.", "And note that, for example if there are 5 online players (includes <a href='#ExprOnlinePlayersCount'>fake online count</a>) in the server and the hover list is set to 3 values, Minecraft will show \"... and 2 more ...\" at end of the list."})
@Example(value="on server list ping:\n\tclear the hover list\n\tadd \"&aWelcome to the &6Minecraft &aserver!\" to the hover list\n\tadd \"\" to the hover list # A blank line\n\tadd \"&cThere are &6%online players count% &conline players!\" to the hover list\n")
@Since(value={"2.3"})
@Events(value={"server list ping"})
public class ExprHoverList
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

    public String @Nullable [] get(Event event) {
        if (!(event instanceof PaperServerListPingEvent)) {
            return null;
        }
        PaperServerListPingEvent pingEvent = (PaperServerListPingEvent)event;
        return (String[])pingEvent.getListedPlayers().stream().map(PaperServerListPingEvent.ListedPlayerInfo::name).toArray(String[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("Can't change the hover list anymore after the server list ping event has already passed");
            return null;
        }
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: 
            case DELETE: 
            case RESET: {
                return CollectionUtils.array(Component[].class, Player[].class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(event instanceof PaperServerListPingEvent)) {
            return;
        }
        PaperServerListPingEvent pingEvent = (PaperServerListPingEvent)event;
        if (delta != null) {
            delta = Arrays.stream(delta).map(obj -> {
                Object object;
                if (obj instanceof Component) {
                    Component component = (Component)obj;
                    object = TextComponentParser.instance().toLegacyString(component);
                } else {
                    object = obj;
                }
                return object;
            }).toArray();
        }
        ArrayList<PaperServerListPingEvent.ListedPlayerInfo> values = new ArrayList<PaperServerListPingEvent.ListedPlayerInfo>();
        if (mode != Changer.ChangeMode.DELETE && mode != Changer.ChangeMode.RESET && mode != Changer.ChangeMode.REMOVE) {
            for (Object object : delta) {
                if (object instanceof Player) {
                    Player player = (Player)object;
                    values.add(new PaperServerListPingEvent.ListedPlayerInfo(player.getName(), player.getUniqueId()));
                    continue;
                }
                values.add(new PaperServerListPingEvent.ListedPlayerInfo((String)object, UUID.randomUUID()));
            }
        }
        List sample = pingEvent.getListedPlayers();
        switch (mode) {
            case SET: {
                sample.clear();
            }
            case ADD: {
                sample.addAll(values);
                break;
            }
            case REMOVE: {
                for (Object value : delta) {
                    sample.removeIf(profile -> profile.name().equals(value));
                }
                break;
            }
            case DELETE: 
            case RESET: {
                sample.clear();
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the hover list";
    }

    static {
        Skript.registerExpression(ExprHoverList.class, String.class, ExpressionType.SIMPLE, "[the] [custom] [player|server] (hover|sample) ([message] list|message)", "[the] [custom] player [hover|sample] list");
    }
}

