/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Stop Server")
@Description(value={"Stops or restarts the server. If restart is used when the restart-script spigot.yml option isn't defined, the server will stop instead."})
@Example.Examples(value={@Example(value="stop the server"), @Example(value="restart server")})
@Since(value={"2.5"})
public class EffStopServer
extends Effect {
    private boolean restart;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.restart = matchedPattern == 1;
        return true;
    }

    @Override
    protected void execute(Event e) {
        if (this.restart) {
            Bukkit.spigot().restart();
        } else {
            Bukkit.shutdown();
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.restart ? "restart" : "stop") + " the server";
    }

    static {
        Skript.registerEffect(EffStopServer.class, "(stop|shut[ ]down) [the] server", "restart [the] server");
    }
}

