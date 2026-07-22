/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Container
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprItemsIn;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Inventory")
@Description(value={"The inventory of a block or player. You can usually omit this expression and can directly add or remove items to/from blocks or players."})
@Example.Examples(value={@Example(value="add a plank to the player's inventory"), @Example(value="clear the player's inventory"), @Example(value="remove 5 wool from the inventory of the clicked block")})
@Since(value={"1.0"})
public class ExprInventory
extends SimpleExpression<Object> {
    private boolean inLoop;
    private Expression<?> holders;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (exprs[0].getSource() instanceof ExprItemsIn) {
            return false;
        }
        Node n = SkriptLogger.getNode();
        this.inLoop = n != null && ("loop " + parseResult.expr).equals(n.getKey());
        this.holders = exprs[0];
        return true;
    }

    @Override
    protected Object[] get(Event e) {
        ArrayList<Inventory> inventories = new ArrayList<Inventory>();
        for (final Object holder : this.holders.getArray(e)) {
            BlockState state;
            ItemMeta meta;
            if (holder instanceof InventoryHolder) {
                inventories.add(((InventoryHolder)holder).getInventory());
                continue;
            }
            if (!(holder instanceof ItemType) || !((meta = ((ItemType)holder).getItemMeta()) instanceof BlockStateMeta) || !((state = ((BlockStateMeta)meta).getBlockState()) instanceof Container)) continue;
            final Inventory underlyingInv = ((Container)state).getInventory();
            Inventory proxy = (Inventory)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{Inventory.class}, new InvocationHandler(){

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Object returnValue = method.invoke((Object)underlyingInv, args);
                    boolean updateSucceeded = state.update();
                    if (updateSucceeded) {
                        ((BlockStateMeta)meta).setBlockState(state);
                        ((ItemType)holder).setItemMeta(meta);
                    }
                    return returnValue;
                }
            });
            inventories.add(proxy);
        }
        Object[] invArray = inventories.toArray(new Inventory[0]);
        if (this.inLoop) {
            ExprItemsIn expr = new ExprItemsIn();
            expr.init(new Expression[]{new SimpleExpression(this, (Inventory[])invArray){
                final /* synthetic */ Inventory[] val$invArray;
                {
                    this.val$invArray = inventoryArray;
                }

                protected Object[] get(Event e) {
                    return this.val$invArray;
                }

                @Override
                public boolean isSingle() {
                    return this.val$invArray.length == 1;
                }

                @Override
                public Class<?> getReturnType() {
                    return Inventory.class;
                }

                @Override
                public String toString(@Nullable Event e, boolean debug) {
                    return "loop of inventory expression";
                }

                @Override
                public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
                    return true;
                }
            }}, 0, Kleenean.FALSE, null);
            return expr.get(e);
        }
        return invArray;
    }

    @Override
    public boolean isSingle() {
        return !this.inLoop && this.holders.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        return this.inLoop ? Slot.class : Inventory.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "inventor" + (this.holders.isSingle() ? "y" : "ies") + " of " + this.holders.toString(e, debug);
    }

    static {
        PropertyExpression.register(ExprInventory.class, Object.class, "inventor(y|ies)", "inventoryholders/itemtypes");
    }
}

