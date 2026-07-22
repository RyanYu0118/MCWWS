/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Damage Value/Durability")
@Description(value={"The damage value/durability of an item."})
@Example.Examples(value={@Example(value="set damage value of player's tool to 10"), @Example(value="reset the durability of {_item}"), @Example(value="set durability of player's held item to 0")})
@Since(value={"1.2, 2.7 (durability reversed)"})
public class ExprDurability
extends SimplePropertyExpression<Object, Integer> {
    private boolean durability;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.durability = parseResult.mark == 1;
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Integer convert(Object object) {
        ItemStack itemStack = ItemUtils.asItemStack(object);
        if (itemStack == null) {
            return null;
        }
        int damage = ItemUtils.getDamage(itemStack);
        return this.convertToDamage(itemStack, damage);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: 
            case DELETE: 
            case RESET: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int change;
        int n = change = delta == null ? 0 : ((Number)delta[0]).intValue();
        if (mode == Changer.ChangeMode.REMOVE) {
            change = -change;
        }
        for (Object object : this.getExpr().getArray(event)) {
            ItemStack itemStack = ItemUtils.asItemStack(object);
            if (itemStack == null) continue;
            ItemUtils.setDamage(itemStack, this.convertToDamage(itemStack, switch (mode) {
                case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> {
                    int current = this.convertToDamage(itemStack, ItemUtils.getDamage(itemStack));
                    yield current + change;
                }
                case Changer.ChangeMode.SET -> change;
                default -> 0;
            }));
            if (object instanceof Slot) {
                ((Slot)object).setItem(itemStack);
                continue;
            }
            if (!(object instanceof ItemType)) continue;
            ((ItemType)object).setItemMeta(itemStack.getItemMeta());
        }
    }

    private int convertToDamage(ItemStack itemStack, int value) {
        if (!this.durability) {
            return value;
        }
        int maxDurability = ItemUtils.getMaxDamage(itemStack);
        if (maxDurability == 0) {
            return 0;
        }
        return maxDurability - value;
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public String getPropertyName() {
        return this.durability ? "durability" : "damage";
    }

    static {
        ExprDurability.register(ExprDurability.class, Integer.class, "(damage[s] [value[s]]|1:durabilit(y|ies))", "itemtypes/itemstacks/slots");
    }
}

