/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Named Item/Inventory")
@Description(value={"Directly names an item/inventory, useful for defining a named item/inventory in a script. If you want to (re)name existing items/inventories you can either use this expression or use <code>set <a href='#PropExprName'>name of &lt;item/inventory&gt;</a> to &lt;text&gt;</code>."})
@Example.Examples(value={@Example(value="give a diamond sword of sharpness 100 named \"<gold>Excalibur\" to the player"), @Example(value="set tool of player to the player's tool named \"<gold>Wand\""), @Example(value="set the name of the player's tool to \"<gold>Wand\""), @Example(value="open hopper inventory named \"Magic Hopper\" to player")})
@Since(value={"2.0, 2.2-dev34 (inventories)"})
public class ExprNamed
extends PropertyExpression<Object, Object> {
    private Expression<Component> name;
    private Class<?>[] returnTypes;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        InventoryType inventoryType;
        Literal lit;
        this.setExpr(exprs[0]);
        Expression<Object> expression = exprs[0];
        if (expression instanceof Literal && (expression = (lit = (Literal)expression).getSingle()) instanceof InventoryType && !(inventoryType = (InventoryType)expression).isCreatable()) {
            Skript.error("Cannot create an inventory of type " + Classes.toString(inventoryType));
            return false;
        }
        this.name = exprs[1];
        ArrayList<Class> returnTypes = new ArrayList<Class>();
        if (exprs[0].canReturn(ItemType.class)) {
            returnTypes.add(ItemType.class);
        }
        if (exprs[0].canReturn(InventoryType.class)) {
            returnTypes.add(Inventory.class);
        }
        this.returnTypes = returnTypes.toArray(new Class[0]);
        return true;
    }

    @Override
    protected Object[] get(Event event, Object[] source) {
        Component name = this.name.getSingle(event);
        if (name == null) {
            return this.get(source, obj -> obj);
        }
        return this.get(source, object -> {
            if (object instanceof InventoryType) {
                InventoryType inventoryType = (InventoryType)object;
                if (!inventoryType.isCreatable()) {
                    return null;
                }
                return Bukkit.createInventory(null, (InventoryType)inventoryType, (Component)name);
            }
            ItemType item = (ItemType)object;
            item = item.clone();
            ItemMeta meta = item.getItemMeta();
            meta.displayName(name);
            item.setItemMeta(meta);
            return item;
        });
    }

    @Override
    public Class<?> getReturnType() {
        if (this.returnTypes.length == 1) {
            return this.returnTypes[0];
        }
        return Object.class;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return Arrays.copyOf(this.returnTypes, this.returnTypes.length);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getExpr().toString(event, debug) + " named " + this.name.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprNamed.class, Object.class, ExpressionType.PROPERTY, "%itemtype/inventorytype% (named|with name[s]) %textcomponent%");
    }
}

