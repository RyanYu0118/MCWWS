/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.Ageable
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Ageable
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
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
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Age of Block/Entity")
@Description(value={"Returns the age or maximum age of blocks and age for entities (there in no maximum age for entities).", "For blocks, 'Age' represents the different growth stages that a crop-like block can go through. A value of 0 indicates that the crop was freshly planted, whilst a value equal to 'maximum age' indicates that the crop is ripe and ready to be harvested.", "For entities, 'Age' represents the time left for them to become adults and it's in minus increasing to be 0 which means they're adults, e.g. A baby cow needs 20 minutes to become an adult which equals to 24,000 ticks so their age will be -24000 once spawned."})
@Example.Examples(value={@Example(value="# Set targeted crop to fully grown crop\nset age of targeted block to maximum age of targeted block\n"), @Example(value="# Spawn a baby cow that will only need 1 minute to become an adult\nspawn a baby cow at player\nset age of last spawned entity to -1200 # in ticks = 60 seconds\n")})
@Since(value={"2.7"})
public class ExprAge
extends SimplePropertyExpression<Object, Integer> {
    private boolean isMax;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isMax = parseResult.hasTag("max");
        this.setExpr(exprs[0]);
        if (this.isMax && !this.getExpr().canReturn(Block.class)) {
            Skript.error("Cannot use 'max age' expression with entities, use just the 'age' expression instead");
            return false;
        }
        return true;
    }

    @Override
    @Nullable
    public Integer convert(Object obj) {
        if (obj instanceof Block) {
            BlockData bd = ((Block)obj).getBlockData();
            if (!(bd instanceof Ageable)) {
                return null;
            }
            Ageable ageable = (Ageable)bd;
            return this.isMax ? ageable.getMaximumAge() : ageable.getAge();
        }
        if (obj instanceof org.bukkit.entity.Ageable) {
            return this.isMax ? null : Integer.valueOf(((org.bukkit.entity.Ageable)obj).getAge());
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.isMax || mode == Changer.ChangeMode.REMOVE_ALL || mode == Changer.ChangeMode.DELETE) {
            return null;
        }
        return CollectionUtils.array(Number.class);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.RESET && delta == null) {
            return;
        }
        int newValue = mode != Changer.ChangeMode.RESET ? ((Number)delta[0]).intValue() : 0;
        block6: for (Object obj : this.getExpr().getArray(event)) {
            Integer oldValue = this.convert(obj);
            if (oldValue == null && mode != Changer.ChangeMode.RESET) continue;
            switch (mode) {
                case REMOVE: {
                    ExprAge.setAge(obj, oldValue - newValue);
                    continue block6;
                }
                case ADD: {
                    ExprAge.setAge(obj, oldValue + newValue);
                    continue block6;
                }
                case SET: {
                    ExprAge.setAge(obj, newValue);
                    continue block6;
                }
                case RESET: {
                    if (obj instanceof org.bukkit.entity.Ageable) {
                        newValue = -24000;
                    }
                    ExprAge.setAge(obj, newValue);
                }
            }
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.isMax ? "maximum " : "") + "age";
    }

    private static void setAge(Object obj, int value) {
        if (obj instanceof Block) {
            Block block = (Block)obj;
            BlockData bd = block.getBlockData();
            if (bd instanceof Ageable) {
                ((Ageable)bd).setAge(Math.max(Math.min(value, ((Ageable)bd).getMaximumAge()), 0));
                block.setBlockData(bd);
            }
        } else if (obj instanceof org.bukkit.entity.Ageable) {
            ((org.bukkit.entity.Ageable)obj).setAge(value);
        }
    }

    static {
        ExprAge.register(ExprAge.class, Integer.class, "[:max[imum]] age", "blocks/entities");
    }
}

