/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent$Status
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Resource Pack")
@Description(value={"Checks state of the resource pack in a <a href='#resource_pack_request_action'>resource pack request response</a> event."})
@Example(value="on resource pack response:\n\tif the resource pack wasn't accepted:\n\t\tkick the player due to \"You have to install the resource pack to play in this server!\"\n")
@Since(value={"2.4"})
@Events(value={"resource pack request response"})
public class CondResourcePack
extends Condition
implements EventRestrictedSyntax {
    private Expression<PlayerResourcePackStatusEvent.Status> states;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.states = exprs[0];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerResourcePackStatusEvent.class);
    }

    @Override
    public boolean check(Event e) {
        if (!(e instanceof PlayerResourcePackStatusEvent)) {
            return this.isNegated();
        }
        PlayerResourcePackStatusEvent.Status state = ((PlayerResourcePackStatusEvent)e).getStatus();
        return this.states.check(e, arg_0 -> state.equals(arg_0), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "resource pack was " + (this.isNegated() ? "not " : "") + this.states.toString(e, debug);
    }

    static {
        Skript.registerCondition(CondResourcePack.class, "[the] resource pack (was|is|has) [been] %resourcepackstate%", "[the] resource pack (was|is|has)(n't| not) [been] %resourcepackstate%");
    }
}

