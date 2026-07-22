/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Cancel Command Cooldown")
@Description(value={"Only usable in commands. Makes it so the current command usage isn't counted towards the cooldown."})
@Example(value="command /nick <text>:\n\texecutable by: players\n\tcooldown: 10 seconds\n\ttrigger:\n\t\tif length of arg-1 is more than 16:\n\t\t\t# Makes it so that invalid arguments don't make you wait for the cooldown again\n\t\t\tcancel the cooldown\n\t\t\tsend \"Your nickname may be at most 16 characters.\"\n\t\t\tstop\n\t\tset the player's display name to arg-1\n")
@Since(value={"2.2-dev34"})
public class EffCancelCooldown
extends Effect
implements EventRestrictedSyntax {
    private boolean cancel;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.cancel = matchedPattern == 0;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(ScriptCommandEvent.class);
    }

    @Override
    protected void execute(Event e) {
        if (!(e instanceof ScriptCommandEvent)) {
            return;
        }
        ((ScriptCommandEvent)e).setCooldownCancelled(this.cancel);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.cancel ? "" : "un") + "cancel the command cooldown";
    }

    static {
        Skript.registerEffect(EffCancelCooldown.class, "(cancel|ignore) [the] [current] [command] cooldown", "un(cancel|ignore) [the] [current] [command] cooldown");
    }
}

