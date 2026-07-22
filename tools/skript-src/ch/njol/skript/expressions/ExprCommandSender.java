/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.command.CommandSender;

@Name(value="Command Sender")
@Description(value={"The player or the console who sent a command. Mostly useful in <a href='commands'>commands</a> and <a href='#command'>command events</a>.", "If the command sender is a command block, its location can be retrieved by using %block's location%"})
@Example.Examples(value={@Example(value="make the command sender execute \"/say hi!\""), @Example(value="on command:\n\tlog \"%executor% used command /%command% %arguments%\" to \"commands.log\"\n")})
@Since(value={"2.0"})
@Events(value={"command"})
public class ExprCommandSender
extends EventValueExpression<CommandSender> {
    public ExprCommandSender() {
        super(CommandSender.class);
    }

    static {
        ExprCommandSender.register(ExprCommandSender.class, CommandSender.class, "[command['s]] (sender|executor)");
    }
}

