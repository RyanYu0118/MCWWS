/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.player.PlayerItemDamageEvent
 *  org.bukkit.event.vehicle.VehicleDamageEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.HealthUtils;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Damage")
@Description(value={"How much damage is done in a entity/vehicle/item damage events.", "For entity damage events, possibly ignoring armour, criticals and/or enchantments (remember that in Skript '1' is one full heart, not half a heart).", "For items, it's the amount of durability damage the item will be taking."})
@Example.Examples(value={@Example(value="on item damage:\n\tevent-item is any tool\n\tclear damage # unbreakable tools as the damage will be 0\n"), @Example(value="on damage:\n\tincrease the damage by 2\n")})
@Since(value={"1.3.5, 2.8.0 (item damage event)"})
@Events(value={"Damage", "Vehicle Damage", "Item Damage"})
public class ExprDamage
extends SimpleExpression<Number>
implements EventRestrictedSyntax {
    private Kleenean delay;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.delay = isDelayed;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityDamageEvent.class, VehicleDamageEvent.class, PlayerItemDamageEvent.class);
    }

    @Nullable
    protected Number[] get(Event event) {
        if (!(event instanceof EntityDamageEvent || event instanceof VehicleDamageEvent || event instanceof PlayerItemDamageEvent)) {
            return new Number[0];
        }
        if (event instanceof VehicleDamageEvent) {
            return CollectionUtils.array(((VehicleDamageEvent)event).getDamage());
        }
        if (event instanceof PlayerItemDamageEvent) {
            return CollectionUtils.array(((PlayerItemDamageEvent)event).getDamage());
        }
        return CollectionUtils.array(HealthUtils.getDamage((EntityDamageEvent)event));
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.delay != Kleenean.FALSE) {
            Skript.error("Can't change the damage anymore after the event has already passed");
            return null;
        }
        switch (mode) {
            case ADD: 
            case SET: 
            case DELETE: 
            case REMOVE: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        if (!(event instanceof EntityDamageEvent || event instanceof VehicleDamageEvent || event instanceof PlayerItemDamageEvent)) {
            return;
        }
        double value = delta == null ? 0.0 : ((Number)delta[0]).doubleValue();
        switch (mode) {
            case SET: 
            case DELETE: {
                if (event instanceof VehicleDamageEvent) {
                    ((VehicleDamageEvent)event).setDamage(value);
                    break;
                }
                if (event instanceof PlayerItemDamageEvent) {
                    ((PlayerItemDamageEvent)event).setDamage((int)value);
                    break;
                }
                HealthUtils.setDamage((EntityDamageEvent)event, value);
                break;
            }
            case REMOVE: {
                value = -value;
            }
            case ADD: {
                if (event instanceof VehicleDamageEvent) {
                    ((VehicleDamageEvent)event).setDamage(((VehicleDamageEvent)event).getDamage() + value);
                    break;
                }
                if (event instanceof PlayerItemDamageEvent) {
                    ((PlayerItemDamageEvent)event).setDamage((int)((double)((PlayerItemDamageEvent)event).getDamage() + value));
                    break;
                }
                HealthUtils.setDamage((EntityDamageEvent)event, HealthUtils.getDamage((EntityDamageEvent)event) + value);
                break;
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
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the damage";
    }

    static {
        Skript.registerExpression(ExprDamage.class, Number.class, ExpressionType.SIMPLE, "[the] damage");
    }
}

