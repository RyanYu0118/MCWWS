/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Beehive
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.EntityBlockStorage
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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Consumer;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EntityBlockStorage;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Storage Entity Count")
@Description(value={"The current number of entities stored inside an entity block storage (i.e. beehive).", "The maximum amount of entities an entity block storage can hold."})
@Example.Examples(value={@Example(value="broadcast the stored entity count of {_beehive}"), @Example(value="set the maximum entity count of {_beehive} to 20")})
@Since(value={"2.11"})
public class ExprEntityStorageEntityCount
extends SimplePropertyExpression<Block, Integer> {
    private boolean withMax;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.withMax = parseResult.hasTag("max");
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Integer convert(Block block) {
        BlockState blockState = block.getState();
        if (blockState instanceof EntityBlockStorage) {
            EntityBlockStorage blockStorage = (EntityBlockStorage)blockState;
            if (this.withMax) {
                return blockStorage.getMaxEntities();
            }
            return blockStorage.getEntityCount();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.withMax) {
            return switch (mode) {
                case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Integer.class);
                default -> null;
            };
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int value = delta != null ? (Integer)delta[0] : 0;
        Consumer<EntityBlockStorage> consumer = switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> blockStorage -> blockStorage.setMaxEntities(value);
            case Changer.ChangeMode.RESET -> blockStorage -> {
                if (blockStorage instanceof Beehive) {
                    blockStorage.setMaxEntities(3);
                } else {
                    blockStorage.setMaxEntities(1);
                }
            };
            case Changer.ChangeMode.ADD -> blockStorage -> {
                int current = blockStorage.getMaxEntities();
                blockStorage.setMaxEntities(current + value);
            };
            case Changer.ChangeMode.REMOVE -> blockStorage -> {
                int current = blockStorage.getMaxEntities();
                blockStorage.setMaxEntities(current - value);
            };
            default -> throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)mode));
        };
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof EntityBlockStorage)) continue;
            EntityBlockStorage blockStorage2 = (EntityBlockStorage)blockState;
            consumer.accept(blockStorage2);
            blockStorage2.update(true, false);
        }
    }

    @Override
    public Class<Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.withMax ? "maximum " : "") + "stored entity count";
    }

    static {
        ExprEntityStorageEntityCount.registerDefault(ExprEntityStorageEntityCount.class, Integer.class, "[max:max[imum]] [stored] entity count", "blocks");
    }
}

