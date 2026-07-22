/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.HashSet;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Item Flags")
@Description(value={"The item flags of an item. Can be modified."})
@Example.Examples(value={@Example(value="set item flags of player's tool to hide enchants and hide attributes"), @Example(value="add hide potion effects to item flags of player's held item"), @Example(value="remove hide enchants from item flags of {legendary sword}")})
@Since(value={"2.10"})
public class ExprItemFlags
extends PropertyExpression<ItemType, ItemFlag> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected ItemFlag[] get(Event event, ItemType[] source) {
        HashSet flags = new HashSet();
        for (ItemType itemType : source) {
            ItemMeta meta = itemType.getItemMeta();
            flags.addAll(meta.getItemFlags());
        }
        return flags.toArray(new ItemFlag[0]);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> CollectionUtils.array(ItemFlag[].class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        ItemFlag[] flags = delta != null ? (ItemFlag[])delta : new ItemFlag[]{};
        for (ItemType itemType : (ItemType[])this.getExpr().getArray(event)) {
            ItemMeta meta = itemType.getItemMeta();
            switch (mode) {
                case SET: {
                    meta.removeItemFlags(ItemFlag.values());
                    meta.addItemFlags(flags);
                    break;
                }
                case ADD: {
                    meta.addItemFlags(flags);
                    break;
                }
                case REMOVE: {
                    meta.removeItemFlags(flags);
                    break;
                }
                case RESET: 
                case DELETE: {
                    meta.removeItemFlags(ItemFlag.values());
                    break;
                }
                default: {
                    return;
                }
            }
            itemType.setItemMeta(meta);
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends ItemFlag> getReturnType() {
        return ItemFlag.class;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "item flags of " + this.getExpr().toString(event, debug);
    }

    static {
        ExprItemFlags.register(ExprItemFlags.class, ItemFlag.class, "item flags", "itemtypes");
    }
}

