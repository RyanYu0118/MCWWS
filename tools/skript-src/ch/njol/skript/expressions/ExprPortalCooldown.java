/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Portal Cooldown")
@Description(value={"The amount of time before an entity can use a portal. By default, it is 15 seconds after exiting a nether portal or end gateway.", "Players in survival/adventure get a cooldown of 0.5 seconds, while those in creative get no cooldown.", "Resetting will set the cooldown back to the default 15 seconds for non-player entities and 0.5 seconds for players."})
@Example(value="on portal:\n\twait 1 tick\n\tset portal cooldown of event-entity to 5 seconds\n")
@Since(value={"2.8.0"})
public class ExprPortalCooldown
extends SimplePropertyExpression<Entity, Timespan> {
    private static final int DEFAULT_COOLDOWN = 300;
    private static final int DEFAULT_COOLDOWN_PLAYER = 10;

    @Override
    @Nullable
    public Timespan convert(Entity entity) {
        return new Timespan(Timespan.TimePeriod.TICK, entity.getPortalCooldown());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case RESET: 
            case DELETE: 
            case REMOVE: {
                return CollectionUtils.array(Timespan.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Entity[] entities = (Entity[])this.getExpr().getArray(event);
        int change = delta == null ? 0 : (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        switch (mode) {
            case REMOVE: {
                change = -change;
            }
            case ADD: {
                for (Entity entity : entities) {
                    entity.setPortalCooldown(Math.max(entity.getPortalCooldown() + change, 0));
                }
                break;
            }
            case RESET: {
                for (Entity entity : entities) {
                    if (entity instanceof Player) {
                        if (((Player)entity).getGameMode() == GameMode.CREATIVE) {
                            entity.setPortalCooldown(0);
                            continue;
                        }
                        entity.setPortalCooldown(10);
                        continue;
                    }
                    entity.setPortalCooldown(300);
                }
                break;
            }
            case SET: 
            case DELETE: {
                for (Entity entity : entities) {
                    entity.setPortalCooldown(Math.max(change, 0));
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "portal cooldown";
    }

    static {
        ExprPortalCooldown.register(ExprPortalCooldown.class, Timespan.class, "portal cooldown", "entities");
    }
}

