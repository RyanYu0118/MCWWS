/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityTargetEvent
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Predicate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Target")
@Description(value={"For players this is the entity at the crosshair.", "For mobs and experience orbs this is the entity they are attacking/following (if any).", "The 'ray size' and 'ignoring blocks' options are only valid for players' targets.", "The 'ray size' option effectively increases the area around the crosshair an entity can be in. It does so by expanding the hitboxes of entities by the given amount. Display entities have a hit box of 0, so using the 'ray size' option can be helpful when targeting them.", "May grab entities in unloaded chunks."})
@Example.Examples(value={@Example(value="on entity target:\n\tif entity's target is a player:\n\t\tsend \"You're being followed by an %entity%!\" to target of entity\n"), @Example(value="reset target of entity # Makes the entity target-less"), @Example(value="delete targeted entity of player # for players it will delete the target"), @Example(value="delete target of last spawned zombie # for entities it will make them target-less")})
@Since(value={"1.4.2, 2.7 (Reset), 2.8.0 (ignore blocks, ray size)"})
public class ExprTarget
extends PropertyExpression<LivingEntity, Entity> {
    private static boolean ignoreBlocks;
    private static int targetBlockDistance;
    @Nullable
    private Expression<Number> raysize;
    @Nullable
    private EntityData<?> type;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.type = exprs[matchedPattern] == null ? null : (EntityData)exprs[matchedPattern].getSingle(null);
        this.setExpr(exprs[1 - matchedPattern]);
        targetBlockDistance = SkriptConfig.maxTargetBlockDistance.value();
        if (targetBlockDistance < 0) {
            targetBlockDistance = 100;
        }
        ignoreBlocks = parser.hasTag("blocks");
        this.raysize = exprs[2];
        return true;
    }

    protected Entity[] get(Event event, LivingEntity[] source) {
        double raysize = this.raysize != null ? this.raysize.getOptionalSingle(event).orElse(0.0).doubleValue() : 0.0;
        return this.get(source, entity -> {
            if (event instanceof EntityTargetEvent && entity.equals((Object)((EntityTargetEvent)event).getEntity()) && !Delay.isDelayed(event)) {
                Entity target = ((EntityTargetEvent)event).getTarget();
                if (target == null || this.type != null && !this.type.isInstance(target)) {
                    return null;
                }
                return target;
            }
            return ExprTarget.getTarget(entity, this.type, raysize);
        });
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case RESET: 
            case DELETE: {
                return CollectionUtils.array(LivingEntity.class);
            }
        }
        return super.acceptChange(mode);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.DELETE) {
            LivingEntity target;
            LivingEntity livingEntity = target = delta == null ? null : (LivingEntity)delta[0];
            if (event instanceof EntityTargetEvent) {
                EntityTargetEvent targetEvent = (EntityTargetEvent)event;
                for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
                    if (!entity.equals((Object)targetEvent.getEntity())) continue;
                    targetEvent.setTarget((Entity)target);
                }
            } else {
                double raysize = this.raysize != null ? this.raysize.getOptionalSingle(event).orElse(0.0).doubleValue() : 0.0;
                for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
                    Object playerTarget;
                    if (entity instanceof Mob) {
                        ((Mob)entity).setTarget(target);
                        continue;
                    }
                    if (!(entity instanceof Player) || mode != Changer.ChangeMode.DELETE || (playerTarget = ExprTarget.getTarget(entity, this.type, raysize)) == null || playerTarget instanceof OfflinePlayer) continue;
                    playerTarget.remove();
                }
            }
            return;
        }
        super.change(event, delta, mode);
    }

    @Override
    public boolean setTime(int time) {
        if (time != EventValues.TIME_PAST) {
            return super.setTime(time, EntityTargetEvent.class, this.getExpr());
        }
        return super.setTime(time);
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return this.type != null ? this.type.getType() : Entity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "target" + (String)(this.type == null ? "" : "ed " + String.valueOf(this.type)) + (String)(this.getExpr().isDefault() ? "" : " of " + this.getExpr().toString(event, debug));
    }

    @Nullable
    public static <T extends Entity> T getTarget(LivingEntity origin, @Nullable EntityData<T> type, double raysize) {
        RayTraceResult result;
        RayTraceResult blockResult;
        if (origin instanceof Mob) {
            return (T)(((Mob)origin).getTarget() == null || type != null && !type.isInstance((Entity)((Mob)origin).getTarget()) ? null : ((Mob)origin).getTarget());
        }
        Predicate<Entity> predicate = entity -> {
            if (entity.equals((Object)origin)) {
                return false;
            }
            if (type != null && !type.isInstance((Entity)entity)) {
                return false;
            }
            return !(entity instanceof Player) || ((Player)entity).getGameMode() != GameMode.SPECTATOR;
        };
        Location eyes = origin.getEyeLocation();
        Vector direction = origin.getLocation().getDirection();
        double distance = targetBlockDistance;
        if (!ignoreBlocks && (blockResult = origin.getWorld().rayTraceBlocks(eyes, direction, (double)targetBlockDistance)) != null) {
            Vector hit = blockResult.getHitPosition();
            distance = eyes.toVector().distance(hit);
        }
        if ((result = origin.getWorld().rayTraceEntities(eyes, direction, distance, raysize, predicate)) == null) {
            return null;
        }
        Entity hitEntity = result.getHitEntity();
        if (hitEntity == null) {
            return null;
        }
        return (T)result.getHitEntity();
    }

    static {
        Skript.registerExpression(ExprTarget.class, Entity.class, ExpressionType.PROPERTY, "[the] target[[ed] %-*entitydata%] [of %livingentities%] [blocks:ignoring blocks] [[with|at] [a] ray[ ]size [of] %-number%]", "%livingentities%'[s] target[[ed] %-*entitydata%] [blocks:ignoring blocks] [[with|at] [a] ray[ ]size [of] %-number%]");
    }
}

