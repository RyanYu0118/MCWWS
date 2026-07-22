/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.conditions;

import ch.njol.skript.command.Commands;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;

@Name(value="Is a Skript command")
@Description(value={"Checks whether a command/string is a custom Skript command."})
@Example(value="on command:\n\tcommand is a skript command\n")
@Since(value={"2.6"})
public class CondIsSkriptCommand
extends PropertyCondition<String> {
    @Override
    public boolean check(String cmd) {
        return Commands.scriptCommandExists(cmd);
    }

    @Override
    protected String getPropertyName() {
        return "skript command";
    }

    static {
        CondIsSkriptCommand.register(CondIsSkriptCommand.class, "[a] s(k|c)ript (command|cmd)", "string");
    }
}

