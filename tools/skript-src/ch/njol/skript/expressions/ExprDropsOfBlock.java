/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Drops Of Block")
@Description(value={"A list of the items that will drop when a block is broken."})
@RequiredPlugins(value={"Minecraft 1.15+ ('as %entity%')"})
@Example(value="on break of block:\n\tgive drops of block using player's tool to player\n")
@Since(value={"2.5.1"})
public class ExprDropsOfBlock
extends SimpleExpression<ItemType> {
    private static final boolean DROPS_OF_ENTITY_EXISTS = Skript.methodExists(Block.class, "getDrops", ItemStack.class, Entity.class);
    private Expression<Block> block;
    private Expression<ItemType> item;
    private Expression<Entity> entity;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.block = exprs[0];
        this.item = exprs[1];
        if (!DROPS_OF_ENTITY_EXISTS && parseResult.mark == 1) {
            Skript.error("Getting the drops of a block as an entity is only possible on Minecraft 1.15+", ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        this.entity = exprs[2];
        return true;
    }

    @Nullable
    protected ItemType[] get(Event e) {
        @Nullable Block[] blocks = this.block.getArray(e);
        if (this.block != null) {
            if (this.item == null) {
                ArrayList<ItemType> list = new ArrayList<ItemType>();
                for (Block block : blocks) {
                    ItemStack[] drops;
                    for (ItemStack drop : drops = block.getDrops().toArray(new ItemStack[0])) {
                        list.add(new ItemType(drop));
                    }
                }
                return list.toArray(new ItemType[0]);
            }
            if (this.entity != null) {
                ItemType item = this.item.getSingle(e);
                Entity entity = this.entity.getSingle(e);
                ArrayList<ItemType> list = new ArrayList<ItemType>();
                for (Block block : blocks) {
                    ItemStack[] drops;
                    for (ItemStack drop : drops = block.getDrops(item.getRandom(), entity).toArray(new ItemStack[0])) {
                        list.add(new ItemType(drop));
                    }
                }
                return list.toArray(new ItemType[0]);
            }
            ItemType item = this.item.getSingle(e);
            ArrayList<ItemType> list = new ArrayList<ItemType>();
            for (Block block : blocks) {
                ItemStack[] drops;
                for (ItemStack drop : drops = block.getDrops(item.getRandom()).toArray(new ItemStack[0])) {
                    list.add(new ItemType(drop));
                }
            }
            return list.toArray(new ItemType[0]);
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "drops of " + this.block.toString(e, debug) + (String)(this.item != null ? " using " + this.item.toString(e, debug) + (this.entity != null ? " as " + this.entity.toString(e, debug) : null) : "");
    }

    static {
        Skript.registerExpression(ExprDropsOfBlock.class, ItemType.class, ExpressionType.COMBINED, "[(all|the|all [of] the)] drops of %blocks% [(using|with) %-itemtype% [(1\u00a6as %-entity%)]]", "%blocks%'s drops [(using|with) %-itemtype% [(1\u00a6as %-entity%)]]");
    }
}

