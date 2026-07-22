/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerItemMendEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
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
import org.bukkit.event.player.PlayerItemMendEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Mending Repair Amount")
@Description(value={"The number of durability points an item is to be repaired in a mending event.", " Modifying the repair amount will affect how much experience is given to the player after mending."})
@Example(value="on item mend:\n\tset the mending repair amount to 100\n")
@Since(value={"2.5.1"})
public class ExprMendingRepairAmount
extends SimpleExpression<Long>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerItemMendEvent.class);
    }

    protected Long[] get(Event e) {
        if (!(e instanceof PlayerItemMendEvent)) {
            return null;
        }
        return new Long[]{((PlayerItemMendEvent)e).getRepairAmount()};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: 
            case RESET: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(event instanceof PlayerItemMendEvent)) {
            return;
        }
        PlayerItemMendEvent e = (PlayerItemMendEvent)event;
        int newLevel = delta != null ? ((Number)delta[0]).intValue() : 0;
        switch (mode) {
            case SET: {
                break;
            }
            case ADD: {
                newLevel += e.getRepairAmount();
                break;
            }
            case REMOVE: {
                newLevel = e.getRepairAmount() - newLevel;
                break;
            }
            case RESET: {
                int repairAmount = e.getExperienceOrb().getExperience() * 2;
                int itemDamage = ItemUtils.getDamage(e.getItem());
                newLevel = Math.min(itemDamage, repairAmount);
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
        e.setRepairAmount(newLevel);
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
        return "the mending repair amount";
    }

    static {
        Skript.registerExpression(ExprMendingRepairAmount.class, Long.class, ExpressionType.SIMPLE, "[the] [mending] repair amount");
    }
}

