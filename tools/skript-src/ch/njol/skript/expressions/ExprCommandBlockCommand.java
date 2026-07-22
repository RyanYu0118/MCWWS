/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.CommandBlock
 *  org.bukkit.entity.minecart.CommandMinecart
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Command Block Command")
@Description(value={"Gets or sets the command associated with a command block or minecart with command block."})
@Example.Examples(value={@Example(value="send command of {_block}"), @Example(value="set command of {_cmdMinecart} to \"say asdf\"")})
@Since(value={"2.10"})
public class ExprCommandBlockCommand
extends SimplePropertyExpression<Object, String> {
    @Override
    @Nullable
    public String convert(Object holder) {
        Block block;
        BlockState blockState;
        String command = "";
        if (holder instanceof Block && (blockState = (block = (Block)holder).getState()) instanceof CommandBlock) {
            CommandBlock cmdBlock = (CommandBlock)blockState;
            command = cmdBlock.getCommand();
        } else if (holder instanceof CommandMinecart) {
            CommandMinecart cmdMinecart = (CommandMinecart)holder;
            command = cmdMinecart.getCommand();
        }
        return command.isEmpty() ? null : command;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
            return CollectionUtils.array(String.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        String newCommand = delta == null ? null : (String)delta[0];
        block3: for (Object holder : this.getExpr().getArray(event)) {
            switch (mode) {
                case RESET: 
                case DELETE: 
                case SET: {
                    Block block;
                    BlockState blockState;
                    if (holder instanceof Block && (blockState = (block = (Block)holder).getState()) instanceof CommandBlock) {
                        CommandBlock cmdBlock = (CommandBlock)blockState;
                        cmdBlock.setCommand(newCommand);
                        cmdBlock.update();
                        continue block3;
                    }
                    if (!(holder instanceof CommandMinecart)) continue block3;
                    CommandMinecart cmdMinecart = (CommandMinecart)holder;
                    cmdMinecart.setCommand(newCommand);
                    continue block3;
                }
                default: {
                    assert (false);
                    continue block3;
                }
            }
        }
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "command block command";
    }

    static {
        ExprCommandBlockCommand.register(ExprCommandBlockCommand.class, String.class, "[command[ ]block] command", "blocks/entities");
    }
}

