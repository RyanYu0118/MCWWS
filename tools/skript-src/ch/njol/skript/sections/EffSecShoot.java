/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Fireball
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.util.Consumer
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Direction;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Shoot")
@Description(value={"Shoots a projectile (or any other entity) from a given entity or location."})
@Example.Examples(value={@Example(value="shoot arrow from all players at speed 2"), @Example(value="shoot a pig from all players:\n\tadd event-entity to {_projectiles::*}\n")})
@Since(value={"2.10"})
public class EffSecShoot
extends EffectSection {
    private static final boolean RUNNING_PAPER;
    private static Method launchWithBukkitConsumer;
    private static Method worldSpawnWithBukkitConsumer;
    private static final Double DEFAULT_SPEED;
    private Expression<EntityData<?>> types;
    private Expression<?> shooters;
    @Nullable
    private Expression<Number> velocity;
    @Nullable
    private Expression<Direction> direction;
    public static Entity lastSpawned;
    @Nullable
    private Trigger trigger;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        this.types = exprs[matchedPattern];
        this.shooters = exprs[1 - matchedPattern];
        this.velocity = exprs[2];
        this.direction = exprs[3];
        if (sectionNode != null) {
            this.trigger = SectionUtils.loadLinkedCode("shoot", (beforeLoading, afterLoading) -> this.loadCode(sectionNode, "shoot", (Runnable)beforeLoading, (Runnable)afterLoading, (Class<? extends Event>)ShootEvent.class));
            return this.trigger != null;
        }
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        Direction finalDirection;
        lastSpawned = null;
        Double finalVelocity = this.velocity != null ? (Number)this.velocity.getSingle(event) : (Number)DEFAULT_SPEED;
        Direction direction = finalDirection = this.direction != null ? this.direction.getSingle(event) : Direction.IDENTITY;
        if (finalVelocity == null || finalDirection == null) {
            return null;
        }
        EntityData<?>[] data = this.types.getArray(event);
        for (Object shooter : this.shooters.getArray(event)) {
            for (EntityData<?> entityData : data) {
                Vector vector;
                Entity finalProjectile = null;
                if (shooter instanceof LivingEntity) {
                    LivingEntity livingShooter = (LivingEntity)shooter;
                    vector = finalDirection.getDirection(livingShooter.getLocation()).multiply(((Number)finalVelocity).doubleValue());
                    Consumer<? extends Entity> afterSpawn = this.afterSpawn(event, entityData, livingShooter, vector);
                    Class<?> type = entityData.getType();
                    Location shooterLoc = livingShooter.getLocation();
                    shooterLoc.setY(shooterLoc.getY() + livingShooter.getEyeHeight() / 2.0);
                    boolean isProjectile = false;
                    boolean useWorldSpawn = false;
                    if (Fireball.class.isAssignableFrom(type)) {
                        shooterLoc = livingShooter.getEyeLocation().add(vector.clone().normalize().multiply(0.5));
                        isProjectile = true;
                        useWorldSpawn = true;
                    } else if (Projectile.class.isAssignableFrom(type)) {
                        isProjectile = true;
                        if (this.trigger != null && !RUNNING_PAPER) {
                            useWorldSpawn = true;
                        }
                    }
                    CaseUsage caseUsage = this.getCaseUsage(isProjectile, useWorldSpawn, this.trigger != null);
                    if (caseUsage == CaseUsage.PROJECTILE_NO_WORLD_TRIGGER_BUKKIT) {
                        try {
                            Object[] objectArray = new Object[3];
                            objectArray[0] = type;
                            objectArray[1] = vector;
                            objectArray[2] = afterSpawn::accept;
                            launchWithBukkitConsumer.invoke((Object)livingShooter, objectArray);
                        }
                        catch (Exception exception) {}
                    } else if (caseUsage == CaseUsage.PROJECTILE_WORLD_TRIGGER_BUKKIT) {
                        try {
                            Object[] objectArray = new Object[2];
                            objectArray[0] = type;
                            objectArray[1] = afterSpawn::accept;
                            worldSpawnWithBukkitConsumer.invoke((Object)livingShooter.getWorld(), objectArray);
                        }
                        catch (Exception exception) {}
                    } else {
                        finalProjectile = caseUsage.shootHandler(entityData, livingShooter, shooterLoc, type, vector, afterSpawn);
                    }
                } else {
                    vector = finalDirection.getDirection((Location)shooter).multiply(((Number)finalVelocity).doubleValue());
                    if (this.trigger != null) {
                        entityData.spawn((Location)shooter, this.afterSpawn(event, entityData, null, vector));
                    } else {
                        finalProjectile = (Entity)entityData.spawn((Location)shooter);
                    }
                }
                if (finalProjectile == null) continue;
                finalProjectile.setVelocity(vector);
                lastSpawned = finalProjectile;
            }
        }
        return super.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "shoot " + this.types.toString(event, debug) + " from " + this.shooters.toString(event, debug) + (String)(this.velocity != null ? " at speed " + this.velocity.toString(event, debug) : "") + (String)(this.direction != null ? " " + this.direction.toString(event, debug) : "");
    }

    private static <E extends Entity> void set(Entity entity, EntityData<E> entityData) {
        entityData.set(entity);
    }

    private CaseUsage getCaseUsage(Boolean isProjectile, Boolean useWorldSpawn, Boolean hasTrigger) {
        if (!isProjectile.booleanValue()) {
            if (!hasTrigger.booleanValue()) {
                return CaseUsage.NOT_PROJECTILE_NO_TRIGGER;
            }
            return CaseUsage.NOT_PROJECTILE_TRIGGER;
        }
        if (!useWorldSpawn.booleanValue()) {
            if (!hasTrigger.booleanValue()) {
                return CaseUsage.PROJECTILE_NO_WORLD_NO_TRIGGER;
            }
            if (launchWithBukkitConsumer != null) {
                return CaseUsage.PROJECTILE_NO_WORLD_TRIGGER_BUKKIT;
            }
            return CaseUsage.PROJECTILE_NO_WORLD_TRIGGER;
        }
        if (!hasTrigger.booleanValue()) {
            return CaseUsage.PROJECTILE_WORLD_NO_TRIGGER;
        }
        if (worldSpawnWithBukkitConsumer != null) {
            return CaseUsage.PROJECTILE_WORLD_TRIGGER_BUKKIT;
        }
        return CaseUsage.PROJECTILE_WORLD_TRIGGER;
    }

    private Consumer<? extends Entity> afterSpawn(Event event, EntityData<?> entityData, @Nullable LivingEntity shooter, Vector vector) {
        return entity -> {
            entity.setVelocity(vector);
            if (entity instanceof Fireball) {
                Fireball fireball = (Fireball)entity;
                fireball.setShooter((ProjectileSource)shooter);
            } else if (entity instanceof Projectile) {
                Projectile projectile = (Projectile)entity;
                projectile.setShooter((ProjectileSource)shooter);
                EffSecShoot.set((Entity)projectile, entityData);
            }
            ShootEvent shootEvent = new ShootEvent((Entity)entity, shooter);
            lastSpawned = entity;
            Variables.withLocalVariables(event, shootEvent, () -> TriggerItem.walk(this.trigger, shootEvent));
        };
    }

    static {
        Skript.registerSection(EffSecShoot.class, "shoot %entitydatas% [from %livingentities/locations%] [(at|with) (speed|velocity) %-number%] [%-direction%]", "(make|let) %livingentities/locations% shoot %entitydatas% [(at|with) (speed|velocity) %-number%] [%-direction%]");
        EventValues.registerEventValue(ShootEvent.class, Entity.class, ShootEvent::getProjectile);
        EventValues.registerEventValue(ShootEvent.class, Projectile.class, shootEvent -> {
            Projectile projectile;
            Entity patt0$temp = shootEvent.getProjectile();
            return patt0$temp instanceof Projectile ? (projectile = (Projectile)patt0$temp) : null;
        });
        if (!Skript.isRunningMinecraft(1, 20, 2)) {
            try {
                launchWithBukkitConsumer = LivingEntity.class.getMethod("launchProjectile", Class.class, Vector.class, org.bukkit.util.Consumer.class);
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
            try {
                worldSpawnWithBukkitConsumer = World.class.getMethod("spawn", Location.class, Class.class, org.bukkit.util.Consumer.class);
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
        }
        boolean launchHasJavaConsumer = Skript.methodExists(LivingEntity.class, "launchProjectile", Class.class, Vector.class, Consumer.class);
        RUNNING_PAPER = launchWithBukkitConsumer != null || launchHasJavaConsumer;
        DEFAULT_SPEED = 5.0;
        lastSpawned = null;
    }

    private static enum CaseUsage {
        NOT_PROJECTILE_NO_TRIGGER{

            @Override
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                return entityData.spawn(location);
            }
        }
        ,
        NOT_PROJECTILE_TRIGGER{

            @Override
            @Nullable
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                entityData.spawn(location, consumer);
                return null;
            }
        }
        ,
        PROJECTILE_NO_WORLD_NO_TRIGGER{

            @Override
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                Projectile projectile = shooter.launchProjectile(type);
                EffSecShoot.set((Entity)projectile, entityData);
                return projectile;
            }
        }
        ,
        PROJECTILE_NO_WORLD_TRIGGER_BUKKIT{

            @Override
            @Nullable
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                return null;
            }
        }
        ,
        PROJECTILE_NO_WORLD_TRIGGER{

            @Override
            @Nullable
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                shooter.launchProjectile(type, vector, consumer);
                return null;
            }
        }
        ,
        PROJECTILE_WORLD_NO_TRIGGER{

            @Override
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                Projectile projectile = (Projectile)shooter.getWorld().spawn(location, type);
                projectile.setShooter((ProjectileSource)shooter);
                return projectile;
            }
        }
        ,
        PROJECTILE_WORLD_TRIGGER_BUKKIT{

            @Override
            @Nullable
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                return null;
            }
        }
        ,
        PROJECTILE_WORLD_TRIGGER{

            @Override
            @Nullable
            public Entity shootHandler(EntityData<?> entityData, LivingEntity shooter, Location location, Class<? extends Entity> type, Vector vector, Consumer<?> consumer) {
                shooter.getWorld().spawn(location, type, consumer);
                return null;
            }
        };


        @Nullable
        public abstract Entity shootHandler(EntityData<?> var1, LivingEntity var2, Location var3, Class<? extends Entity> var4, Vector var5, Consumer<?> var6);
    }

    public static class ShootEvent
    extends Event {
        private Entity projectile;
        @Nullable
        private LivingEntity shooter;

        public ShootEvent(Entity projectile, @Nullable LivingEntity shooter) {
            this.projectile = projectile;
            this.shooter = shooter;
        }

        public Entity getProjectile() {
            return this.projectile;
        }

        @Nullable
        public LivingEntity getShooter() {
            return this.shooter;
        }

        @NotNull
        public HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }
}

