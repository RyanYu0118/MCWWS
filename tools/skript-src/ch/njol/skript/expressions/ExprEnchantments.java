/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.event.Event
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
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Item Enchantments")
@Description(value={"All the enchantments an <a href='#itemtype'>item type</a> has."})
@Example(value="clear enchantments of event-item")
@Since(value={"2.2-dev36"})
public class ExprEnchantments
extends SimpleExpression<EnchantmentType> {
    private Expression<ItemType> items;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.items = exprs[0];
        return true;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Nullable
    protected EnchantmentType[] get(Event e) {
        ArrayList enchantments = new ArrayList();
        for (ItemType item : this.items.getArray(e)) {
            EnchantmentType[] enchants = item.getEnchantmentTypes();
            if (enchants == null) continue;
            Collections.addAll(enchantments, enchants);
        }
        return enchantments.toArray(new EnchantmentType[0]);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return CollectionUtils.array(Enchantment[].class, EnchantmentType[].class);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        ItemType[] source = this.items.getArray(e);
        EnchantmentType[] enchants = new EnchantmentType[delta != null ? delta.length : 0];
        if (delta != null && delta.length != 0) {
            for (int i = 0; i < delta.length; ++i) {
                enchants[i] = delta[i] instanceof EnchantmentType ? (EnchantmentType)delta[i] : new EnchantmentType((Enchantment)delta[i]);
            }
        }
        switch (mode) {
            case ADD: {
                for (ItemType item : source) {
                    item.addEnchantments(enchants);
                }
                break;
            }
            case REMOVE: 
            case REMOVE_ALL: {
                for (ItemType item : source) {
                    ItemMeta meta = item.getItemMeta();
                    assert (meta != null);
                    for (EnchantmentType enchant : enchants) {
                        Enchantment ench = enchant.getType();
                        assert (ench != null);
                        if (enchant.getInternalLevel() == -1 || meta.getEnchantLevel(ench) == enchant.getLevel()) {
                            meta.removeEnchant(ench);
                        }
                        item.setItemMeta(meta);
                    }
                }
                break;
            }
            case SET: {
                for (ItemType item : source) {
                    item.clearEnchantments();
                    item.addEnchantments(enchants);
                }
                break;
            }
            case DELETE: 
            case RESET: {
                for (ItemType item : source) {
                    item.clearEnchantments();
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends EnchantmentType> getReturnType() {
        return EnchantmentType.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the enchantments of " + this.items.toString(e, debug);
    }

    static {
        PropertyExpression.register(ExprEnchantments.class, EnchantmentType.class, "enchantments", "itemtypes");
    }
}

