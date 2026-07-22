/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
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
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Hostname")
@Description(value={"The hostname used by the connecting player to connect to the server in a <a href='#connect'>connect</a> event."})
@Example(value="on connect:\n\thostname is \"testers.example.com\"\n\tsend \"Welcome back tester!\"\n")
@Since(value={"2.6.1"})
public class ExprHostname
extends SimpleExpression<String>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerLoginEvent.class);
    }

    @Nullable
    protected String[] get(Event e) {
        if (!(e instanceof PlayerLoginEvent)) {
            return null;
        }
        return new String[]{((PlayerLoginEvent)e).getHostname()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "hostname";
    }

    static {
        Skript.registerExpression(ExprHostname.class, String.class, ExpressionType.SIMPLE, "[the] (host|domain)[ ][name]");
    }
}

