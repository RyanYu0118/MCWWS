/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.event.Event
 *  org.bukkit.event.enchantment.EnchantItemEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
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
import ch.njol.skript.util.EnchantmentType;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Applied Enchantments")
@Description(value={"The applied enchantments in an enchant event.", " Deleting or removing the applied enchantments will prevent the item's enchantment."})
@Example(value="on enchant:\n\tset the applied enchantments to sharpness 10 and fire aspect 5\n")
@Events(value={"enchant"})
@Since(value={"2.5"})
public class ExprAppliedEnchantments
extends SimpleExpression<EnchantmentType>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EnchantItemEvent.class);
    }

    @Nullable
    protected EnchantmentType[] get(Event e) {
        if (!(e instanceof EnchantItemEvent)) {
            return null;
        }
        return (EnchantmentType[])((EnchantItemEvent)e).getEnchantsToAdd().entrySet().stream().map(entry -> new EnchantmentType((Enchantment)entry.getKey(), (Integer)entry.getValue())).toArray(EnchantmentType[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL || mode == Changer.ChangeMode.RESET) {
            return null;
        }
        return CollectionUtils.array(Enchantment[].class, EnchantmentType[].class);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(event instanceof EnchantItemEvent)) {
            return;
        }
        EnchantmentType[] enchants = new EnchantmentType[delta != null ? delta.length : 0];
        if (delta != null && delta.length != 0) {
            for (int i = 0; i < delta.length; ++i) {
                enchants[i] = delta[i] instanceof EnchantmentType ? (EnchantmentType)delta[i] : new EnchantmentType((Enchantment)delta[i]);
            }
        }
        EnchantItemEvent e = (EnchantItemEvent)event;
        switch (mode) {
            case SET: {
                e.getEnchantsToAdd().clear();
            }
            case ADD: {
                for (EnchantmentType enchant : enchants) {
                    e.getEnchantsToAdd().put(enchant.getType(), enchant.getLevel());
                }
                break;
            }
            case REMOVE: {
                for (EnchantmentType enchant : enchants) {
                    e.getEnchantsToAdd().remove(enchant.getType(), enchant.getLevel());
                }
                break;
            }
            case DELETE: {
                e.getEnchantsToAdd().clear();
            }
            case REMOVE_ALL: 
            case RESET: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends EnchantmentType> getReturnType() {
        return EnchantmentType.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "applied enchantments";
    }

    static {
        Skript.registerExpression(ExprAppliedEnchantments.class, EnchantmentType.class, ExpressionType.SIMPLE, "[the] applied enchant[ment]s");
    }
}

