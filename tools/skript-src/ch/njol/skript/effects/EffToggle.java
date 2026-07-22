/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.Openable
 *  org.bukkit.block.data.Powerable
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Toggle")
@Description(value={"Toggle the state of a block or boolean."})
@Example.Examples(value={@Example(value="# use arrows to toggle switches, doors, etc.\non projectile hit:\n\tprojectile is arrow\n\ttoggle the block at the arrow\n"), @Example(value="# With booleans\ntoggle gravity of player\n")})
@Since(value={"1.4, 2.12 (booleans)"})
public class EffToggle
extends Effect {
    private static final Patterns<Action> patterns = new Patterns(new Object[][]{{"(open|turn on|activate) %blocks%", Action.ACTIVATE}, {"(close|turn off|de[-]activate) %blocks%", Action.DEACTIVATE}, {"(toggle|switch) [[the] state of] %blocks/booleans%", Action.TOGGLE}});
    private Expression<?> togglables;
    private Action action;
    private Type type;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.togglables = expressions[0];
        this.action = patterns.getInfo(matchedPattern);
        this.type = Type.fromClass(this.togglables);
        if (this.type == Type.BOOLEANS && !Changer.ChangerUtils.acceptsChange(this.togglables, Changer.ChangeMode.SET, Boolean.class)) {
            Skript.error("Cannot toggle '" + String.valueOf(this.togglables) + "' as it cannot be set to booleans.");
            return false;
        }
        if (this.type == Type.MIXED && !Changer.ChangerUtils.acceptsChange(this.togglables, Changer.ChangeMode.SET, Block.class, Boolean.class)) {
            Skript.error("Cannot toggle '" + String.valueOf(this.togglables) + "' as it cannot be set to both blocks and booleans.");
            return false;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        switch (this.type.ordinal()) {
            case 1: {
                this.toggleBooleans(event);
                break;
            }
            case 0: {
                this.toggleBlocks(event);
                break;
            }
            case 2: {
                this.toggleMixed(event);
            }
        }
    }

    private void toggleBlocks(Event event) {
        for (Object obj : this.togglables.getArray(event)) {
            if (!(obj instanceof Block)) continue;
            Block block = (Block)obj;
            this.toggleSingleBlock(block);
        }
    }

    private void toggleSingleBlock(@NotNull Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Openable) {
            Openable openable = (Openable)data;
            openable.setOpen(this.action.apply(openable.isOpen()));
        } else if (data instanceof Powerable) {
            Powerable powerable = (Powerable)data;
            powerable.setPowered(this.action.apply(powerable.isPowered()));
        }
        block.setBlockData(data);
    }

    private void toggleBooleans(Event event) {
        this.togglables.changeInPlace(event, obj -> {
            if (!(obj instanceof Boolean)) {
                return null;
            }
            Boolean bool = (Boolean)obj;
            return this.action.apply(bool);
        });
    }

    private void toggleMixed(Event event) {
        this.togglables.changeInPlace(event, obj -> {
            if (obj instanceof Block) {
                Block block = (Block)obj;
                this.toggleSingleBlock(block);
                return block;
            }
            if (obj instanceof Boolean) {
                Boolean bool = (Boolean)obj;
                return this.action.apply(bool);
            }
            return obj;
        });
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String actionText = switch (this.action.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "activate";
            case 1 -> "deactivate";
            case 2 -> "toggle";
        };
        return actionText + " " + this.togglables.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffToggle.class, patterns.getPatterns());
    }

    private static enum Action {
        ACTIVATE,
        DEACTIVATE,
        TOGGLE;


        public boolean apply(boolean current) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> true;
                case 1 -> false;
                case 2 -> !current;
            };
        }
    }

    private static enum Type {
        BLOCKS,
        BOOLEANS,
        MIXED;


        public static Type fromClass(Expression<?> expression) {
            boolean isBlockType = expression.canReturn(Block.class);
            boolean isBooleanType = expression.canReturn(Boolean.class);
            if (isBlockType && !isBooleanType) {
                return BLOCKS;
            }
            if (isBooleanType && !isBlockType) {
                return BOOLEANS;
            }
            return MIXED;
        }
    }
}

