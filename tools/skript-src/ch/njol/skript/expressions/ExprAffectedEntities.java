/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.AreaEffectCloudApplyEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
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
import java.util.Iterator;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Affected Entities")
@Description(value={"The affected entities in the <a href='#aoe_cloud_effect'>area cloud effect</a> event."})
@Example(value="on area cloud effect:\n\tloop affected entities:\n\t\tif loop-value is a player:\n\t\t\tsend \"WARNING: you've step on an area effect cloud!\" to loop-value\n")
@Since(value={"2.4"})
public class ExprAffectedEntities
extends SimpleExpression<LivingEntity>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(AreaEffectCloudApplyEvent.class);
    }

    protected LivingEntity @Nullable [] get(Event event) {
        if (event instanceof AreaEffectCloudApplyEvent) {
            AreaEffectCloudApplyEvent areaEvent = (AreaEffectCloudApplyEvent)event;
            return areaEvent.getAffectedEntities().toArray(new LivingEntity[0]);
        }
        return null;
    }

    @Override
    @Nullable
    public Iterator<? extends LivingEntity> iterator(Event event) {
        if (event instanceof AreaEffectCloudApplyEvent) {
            AreaEffectCloudApplyEvent areaEvent = (AreaEffectCloudApplyEvent)event;
            return areaEvent.getAffectedEntities().iterator();
        }
        return super.iterator(event);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.REMOVE -> CollectionUtils.array(LivingEntity[].class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof AreaEffectCloudApplyEvent)) {
            return;
        }
        AreaEffectCloudApplyEvent areaEvent = (AreaEffectCloudApplyEvent)event;
        LivingEntity[] entities = (LivingEntity[])delta;
        switch (mode) {
            case REMOVE: {
                for (LivingEntity entity : entities) {
                    areaEvent.getAffectedEntities().remove(entity);
                }
                break;
            }
            case SET: {
                areaEvent.getAffectedEntities().clear();
            }
            case ADD: {
                for (LivingEntity entity : entities) {
                    areaEvent.getAffectedEntities().add(entity);
                }
                break;
            }
            case DELETE: 
            case RESET: {
                areaEvent.getAffectedEntities().clear();
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public Class<? extends LivingEntity> getReturnType() {
        return LivingEntity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the affected entities";
    }

    static {
        Skript.registerExpression(ExprAffectedEntities.class, LivingEntity.class, ExpressionType.SIMPLE, "[the] affected entities");
    }
}

