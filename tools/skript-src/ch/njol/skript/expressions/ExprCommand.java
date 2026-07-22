/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.server.ServerCommandEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.command.ScriptCommandEvent;
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
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Command")
@Description(value={"The command that caused an 'on command' event (excluding the leading slash and all arguments)"})
@Example(value="# prevent any commands except for the /exit command during some game\non command:\n\tif {game::%player%::playing} is true:\n\t\tif the command is not \"exit\":\n\t\t\tmessage \"You're not allowed to use commands during the game\"\n\t\t\tcancel the event\n")
@Since(value={"2.0, 2.7 (support for script commands)"})
@Events(value={"command"})
public class ExprCommand
extends SimpleExpression<String>
implements EventRestrictedSyntax {
    private boolean fullCommand;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.fullCommand = matchedPattern == 0;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerCommandPreprocessEvent.class, ServerCommandEvent.class, ScriptCommandEvent.class);
    }

    @Nullable
    protected String[] get(Event e) {
        Object s;
        if (e instanceof PlayerCommandPreprocessEvent) {
            s = ((PlayerCommandPreprocessEvent)e).getMessage().substring(1).trim();
        } else if (e instanceof ServerCommandEvent) {
            s = ((ServerCommandEvent)e).getCommand().trim();
        } else {
            ScriptCommandEvent event = (ScriptCommandEvent)e;
            s = event.getCommandLabel() + " " + event.getArgsString();
        }
        if (this.fullCommand) {
            return new String[]{s};
        }
        int c = ((String)s).indexOf(32);
        return new String[]{c == -1 ? s : ((String)s).substring(0, c)};
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
    public String toString(@Nullable Event e, boolean debug) {
        return this.fullCommand ? "the full command" : "the command";
    }

    static {
        Skript.registerExpression(ExprCommand.class, String.class, ExpressionType.SIMPLE, "[the] (full|complete|whole) command", "[the] command [(label|alias)]");
    }
}

