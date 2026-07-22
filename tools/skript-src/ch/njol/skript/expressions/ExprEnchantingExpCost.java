/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.skript.util.Experience;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Enchanting Experience Cost")
@Description(value={"The cost of enchanting in an enchant event.", "This is number that was displayed in the enchantment table, not the actual number of levels removed."})
@Example(value="on enchant:\n\tsend \"Cost: %the displayed enchanting cost%\" to player\n")
@Events(value={"enchant"})
@Since(value={"2.5"})
public class ExprEnchantingExpCost
extends SimpleExpression<Long>
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
    protected Long[] get(Event e) {
        return new Long[]{((EnchantItemEvent)e).getExpLevelCost()};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.REMOVE_ALL) {
            return null;
        }
        return CollectionUtils.array(Number.class, Experience.class);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        Object c = delta[0];
        int cost = c instanceof Number ? ((Number)c).intValue() : ((Experience)c).getXP();
        EnchantItemEvent e = (EnchantItemEvent)event;
        switch (mode) {
            case SET: {
                e.setExpLevelCost(cost);
                break;
            }
            case ADD: {
                int add = e.getExpLevelCost() + cost;
                e.setExpLevelCost(add);
                break;
            }
            case REMOVE: {
                int subtract = e.getExpLevelCost() - cost;
                e.setExpLevelCost(subtract);
                break;
            }
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
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the displayed cost of enchanting";
    }

    static {
        Skript.registerExpression(ExprEnchantingExpCost.class, Long.class, ExpressionType.SIMPLE, "[the] [displayed] ([e]xp[erience]|enchanting) cost");
    }
}

