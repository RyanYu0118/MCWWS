/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.enchantments.EnchantmentOffer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Experience;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Enchantment Offer Cost")
@Description(value={"The cost of an enchantment offer. This is displayed to the right of an enchantment offer.", "If the cost is changed, it will always be at least 1.", "This changes how many levels are required to enchant, but does not change the number of levels removed.", "To change the number of levels removed, use the enchant event."})
@Example(value="set cost of enchantment offer 1 to 50")
@Since(value={"2.5"})
@RequiredPlugins(value={"1.11 or newer"})
public class ExprEnchantmentOfferCost
extends SimplePropertyExpression<EnchantmentOffer, Long> {
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public Long convert(EnchantmentOffer offer) {
        return offer.getCost();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.REMOVE_ALL || mode == Changer.ChangeMode.RESET) {
            return null;
        }
        return CollectionUtils.array(Number.class, Experience.class);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int cost;
        EnchantmentOffer[] offers = (EnchantmentOffer[])this.getExpr().getArray(event);
        if (offers.length == 0 || delta == null) {
            return;
        }
        Object c = delta[0];
        int n = cost = c instanceof Number ? ((Number)c).intValue() : ((Experience)c).getXP();
        if (cost < 1) {
            return;
        }
        switch (mode) {
            case SET: {
                for (EnchantmentOffer offer : offers) {
                    offer.setCost(cost);
                }
                break;
            }
            case ADD: {
                for (EnchantmentOffer offer : offers) {
                    int change = offer.getCost() + cost;
                    if (change < 1) {
                        return;
                    }
                    offer.setCost(change);
                }
                break;
            }
            case REMOVE: {
                for (EnchantmentOffer offer : offers) {
                    int change = offer.getCost() - cost;
                    if (change < 1) {
                        return;
                    }
                    offer.setCost(change);
                }
                break;
            }
            case RESET: 
            case DELETE: 
            case REMOVE_ALL: {
                if (!$assertionsDisabled) {
                    throw new AssertionError();
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "enchantment cost";
    }

    static {
        boolean bl = $assertionsDisabled = !ExprEnchantmentOfferCost.class.desiredAssertionStatus();
        if (Skript.classExists("org.bukkit.enchantments.EnchantmentOffer")) {
            ExprEnchantmentOfferCost.register(ExprEnchantmentOfferCost.class, Long.class, "[enchant[ment]] cost", "enchantmentoffers");
        }
    }
}

