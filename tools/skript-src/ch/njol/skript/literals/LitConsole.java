/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.literals;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Console")
@Description(value={"Represents the server's console which can receive messages and execute commands"})
@Example.Examples(value={@Example(value="execute console command \"/stop\""), @Example(value="send \"message to console\" to the console")})
@Since(value={"1.3.1"})
public class LitConsole
extends SimpleLiteral<ConsoleCommandSender> {
    private static final ConsoleCommandSender console;

    public LitConsole() {
        super(new ConsoleCommandSender[]{console}, ConsoleCommandSender.class, true);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the console";
    }

    static {
        Skript.registerExpression(LitConsole.class, ConsoleCommandSender.class, ExpressionType.SIMPLE, "[the] (console|server)");
        console = Bukkit.getConsoleSender();
    }
}

