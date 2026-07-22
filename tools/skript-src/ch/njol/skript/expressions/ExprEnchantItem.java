/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.enchantment.EnchantItemEvent
 *  org.bukkit.event.enchantment.PrepareItemEnchantEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Enchant Item")
@Description(value={"The enchant item in an enchant prepare event or enchant event.", "It can be modified, but enchantments will still be applied in the enchant event."})
@Example.Examples(value={@Example(value="on enchant:\n\tset the enchanted item to a diamond chestplate\n"), @Example(value="on enchant prepare:\n\tset the enchant item to a wooden sword\n")})
@Events(value={"enchant prepare", "enchant"})
@Since(value={"2.5"})
public class ExprEnchantItem
extends SimpleExpression<ItemType>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EnchantItemEvent.class, PrepareItemEnchantEvent.class);
    }

    @Nullable
    protected ItemType[] get(Event e) {
        if (e instanceof PrepareItemEnchantEvent) {
            return new ItemType[]{new ItemType(((PrepareItemEnchantEvent)e).getItem())};
        }
        if (e instanceof EnchantItemEvent) {
            return new ItemType[]{new ItemType(((EnchantItemEvent)e).getItem())};
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(ItemType.class);
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        ItemType item = (ItemType)delta[0];
        switch (mode) {
            case SET: {
                if (event instanceof PrepareItemEnchantEvent) {
                    PrepareItemEnchantEvent e = (PrepareItemEnchantEvent)event;
                    e.getItem().setType(item.getMaterial());
                    e.getItem().setItemMeta(item.getItemMeta());
                    e.getItem().setAmount(item.getAmount());
                    break;
                }
                if (!(event instanceof EnchantItemEvent)) break;
                EnchantItemEvent e = (EnchantItemEvent)event;
                e.getItem().setType(item.getMaterial());
                e.getItem().setItemMeta(item.getItemMeta());
                e.getItem().setAmount(item.getAmount());
                break;
            }
            case ADD: 
            case REMOVE: 
            case RESET: 
            case DELETE: 
            case REMOVE_ALL: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "enchanted item";
    }

    static {
        Skript.registerExpression(ExprEnchantItem.class, ItemType.class, ExpressionType.SIMPLE, "[the] enchant[ed] item");
    }
}

