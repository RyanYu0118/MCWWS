/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Particle$DustTransition
 *  org.bukkit.Particle$Spell
 *  org.bukkit.Particle$Trail
 *  org.bukkit.Vibration
 *  org.bukkit.Vibration$Destination
 *  org.bukkit.Vibration$Destination$BlockDestination
 *  org.bukkit.Vibration$Destination$EntityDestination
 *  org.bukkit.entity.Entity
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.particles.registration;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.skript.util.Timespan;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.particles.registration.DataSupplier;
import org.skriptlang.skript.bukkit.particles.registration.EffectInfo;
import org.skriptlang.skript.bukkit.particles.registration.ToString;

public class DataParticles {
    private static final List<EffectInfo<Particle, ?>> PARTICLE_INFOS = new ArrayList();

    private static <F, D> void registerParticle(Particle particle, String pattern, D defaultData, Function<F, D> dataFunction, ToString toStringFunction) {
        DataParticles.registerParticle(particle, pattern, (event, expressions, parseResult) -> {
            if (expressions[0] == null) {
                return defaultData;
            }
            Object data = dataFunction.apply(expressions[0].getSingle(event));
            if (data == null) {
                return defaultData;
            }
            return data;
        }, toStringFunction);
    }

    private static <D> void registerParticle(Particle particle, String pattern, DataSupplier<D> dataSupplier, ToString toStringFunction) {
        PARTICLE_INFOS.add(new EffectInfo<Particle, D>(particle, pattern, dataSupplier, toStringFunction));
    }

    public static @Unmodifiable @NotNull List<EffectInfo<Particle, ?>> getParticleInfos() {
        if (PARTICLE_INFOS.isEmpty()) {
            DataParticles.registerAll();
        }
        return Collections.unmodifiableList(PARTICLE_INFOS);
    }

    private static void registerAll() {
        if (Skript.isRunningMinecraft(1, 21, 9)) {
            DataSupplier<Particle.Spell> spellData = (event, expressions, parseResult) -> {
                Number power;
                Color color = (Color)expressions[0].getSingle(event);
                if (color == null) {
                    color = ColorRGB.fromBukkitColor(org.bukkit.Color.WHITE);
                }
                if ((power = (Number)expressions[1].getSingle(event)) == null) {
                    power = 1.0;
                }
                return new Particle.Spell(color.asBukkitColor(), power.floatValue());
            };
            DataParticles.registerParticle(Particle.EFFECT, "%color% effect particle[s] (of|with) power %number%", spellData, (exprs, parseResult, builder) -> builder.append(exprs[0], "effect particle of power", exprs[1]));
            DataParticles.registerParticle(Particle.INSTANT_EFFECT, "%color% instant effect particle[s] (of|with) power %number%", spellData, (exprs, parseResult, builder) -> builder.append(exprs[0], "instant effect particle of power", exprs[1]));
            DataParticles.registerParticle(Particle.FLASH, "%color% flash particle[s]", org.bukkit.Color.WHITE, color -> ((Color)color).asBukkitColor(), (exprs, parseResult, builder) -> builder.append(exprs[0], "flash particle"));
        }
        DataParticles.registerParticle(Particle.ENTITY_EFFECT, "%color% (potion|entity) effect particle[s]", org.bukkit.Color.WHITE, color -> ((Color)color).asBukkitColor(), (exprs, parseResult, builder) -> builder.append(exprs[0], "potion effect particle"));
        if (Skript.isRunningMinecraft(1, 21, 5)) {
            DataParticles.registerParticle(Particle.TINTED_LEAVES, "%color% tinted leaves particle[s]", org.bukkit.Color.WHITE, color -> ((Color)color).asBukkitColor(), (exprs, parseResult, builder) -> builder.append(exprs[0], "tinted leaves particle"));
        }
        DataParticles.registerParticle(Particle.DUST, "%color% dust particle[s] [of size %number%]", (event, expressions, parseResult) -> {
            Color color = (Color)expressions[0].getSingle(event);
            org.bukkit.Color bukkitColor = color == null ? org.bukkit.Color.WHITE : color.asBukkitColor();
            Number size = (Number)expressions[1].getSingle(event);
            if (size == null || size.doubleValue() <= 0.0) {
                size = 1.0;
            }
            return new Particle.DustOptions(bukkitColor, size.floatValue());
        }, (exprs, parseResult, builder) -> builder.append(exprs[0], "dust particle of size", exprs[1]));
        DataParticles.registerParticle(Particle.DUST_COLOR_TRANSITION, "%color% dust particle[s] [of size %number%] that transitions to %color%", (event, expressions, parseResult) -> {
            Color toColor;
            Color color = (Color)expressions[0].getSingle(event);
            org.bukkit.Color bukkitColor = color == null ? org.bukkit.Color.WHITE : color.asBukkitColor();
            Number size = (Number)expressions[1].getSingle(event);
            if (size == null || size.doubleValue() <= 0.0) {
                size = 1.0;
            }
            org.bukkit.Color bukkitToColor = (toColor = (Color)expressions[2].getSingle(event)) == null ? org.bukkit.Color.WHITE : toColor.asBukkitColor();
            return new Particle.DustTransition(bukkitColor, bukkitToColor, size.floatValue());
        }, (exprs, parseResult, builder) -> builder.append(exprs[0], "dust particle of size", exprs[1], "that transitions to", exprs[2]));
        DataParticles.registerParticle(Particle.BLOCK, "%itemtype/blockdata% block particle[s]", DataSupplier::getBlockData, (exprs, parseResult, builder) -> builder.append(exprs[0], "block particle"));
        if (Skript.isRunningMinecraft(1, 21, 2)) {
            DataParticles.registerParticle(Particle.BLOCK_CRUMBLE, "%itemtype/blockdata% [block] crumble particle[s]", DataSupplier::getBlockData, (exprs, parseResult, builder) -> builder.append(exprs[0], "block crumble particle"));
        }
        DataParticles.registerParticle(Particle.BLOCK_MARKER, "%itemtype/blockdata% [block] marker particle[s]", DataSupplier::getBlockData, (exprs, parseResult, builder) -> builder.append(exprs[0], "block marker particle"));
        DataParticles.registerParticle(Particle.DUST_PILLAR, "%itemtype/blockdata% dust pillar particle[s]", DataSupplier::getBlockData, (exprs, parseResult, builder) -> builder.append(exprs[0], "dust pillar particle"));
        DataParticles.registerParticle(Particle.FALLING_DUST, "falling %itemtype/blockdata% dust particle[s]", DataSupplier::getBlockData, (exprs, parseResult, builder) -> builder.append("falling", exprs[0], "dust particle"));
        DataParticles.registerParticle(Particle.ITEM, "%itemtype% item particle[s]", (event, expressions, parseResult) -> {
            ItemType itemType = (ItemType)expressions[0].getSingle(event);
            if (itemType == null) {
                return new ItemStack(Material.AIR);
            }
            return itemType.getRandom();
        }, (exprs, parseResult, builder) -> builder.append(exprs[0], "item particle"));
        DataParticles.registerParticle(Particle.SCULK_CHARGE, "sculk charge particle[s] [with [a] roll angle [of] %-number%]", (event, expressions, parseResult) -> {
            if (expressions[0] == null) {
                return Float.valueOf(0.0f);
            }
            Number angle = (Number)expressions[0].getSingle(event);
            if (angle == null) {
                return Float.valueOf(0.0f);
            }
            return Float.valueOf((float)Math.toRadians(angle.floatValue()));
        }, (exprs, parseResult, builder) -> builder.append((Object)"sculk charge particle)").appendIf(exprs[0] != null, "with a roll angle of", exprs[0]));
        DataParticles.registerParticle(Particle.SHRIEK, "shriek particle[s] [delayed by %-timespan%]", 0, timespan -> ((Timespan)timespan).getAs(Timespan.TimePeriod.TICK), (exprs, parseResult, builder) -> builder.append((Object)"shriek particle").appendIf(exprs[0] != null, "delayed by", exprs[0]));
        DataParticles.registerParticle(Particle.VIBRATION, "vibration particle[s] moving to[wards] %entity/location% [over [a duration of] %-timespan%]", (event, expressions, parseResult) -> {
            Vibration.Destination.BlockDestination destination;
            Object target = expressions[0].getSingle(event);
            if (target instanceof Location) {
                Location location = (Location)target;
                destination = new Vibration.Destination.BlockDestination(location);
            } else if (target instanceof Entity) {
                Entity entity = (Entity)target;
                destination = new Vibration.Destination.EntityDestination(entity);
            } else {
                return null;
            }
            Timespan timespan = (Timespan)expressions[1].getSingle(event);
            int duration = timespan == null ? 20 : (int)timespan.getAs(Timespan.TimePeriod.TICK);
            return new Vibration((Vibration.Destination)destination, duration);
        }, (exprs, parseResult, builder) -> builder.append("vibration particle moving towards", exprs[0]).appendIf(exprs[1] != null, "over", exprs[1]));
        if (Skript.isRunningMinecraft(1, 21, 4)) {
            DataParticles.registerParticle(Particle.TRAIL, "%color% trail particle[s] moving to[wards] %location% [over [a duration of] %-timespan%]", (event, expressions, parseResult) -> {
                Timespan duration;
                Color color = (Color)expressions[0].getSingle(event);
                org.bukkit.Color bukkitColor = color == null ? org.bukkit.Color.WHITE : color.asBukkitColor();
                Location targetLocation = (Location)expressions[1].getSingle(event);
                if (targetLocation == null) {
                    return null;
                }
                Number durationTicks = 20;
                if (expressions[2] != null && (duration = (Timespan)expressions[2].getSingle(event)) != null) {
                    durationTicks = duration.getAs(Timespan.TimePeriod.TICK);
                }
                return new Particle.Trail(targetLocation, bukkitColor, ((Number)durationTicks).intValue());
            }, (exprs, parseResult, builder) -> builder.append(exprs[0], "trail particle leading to", exprs[1]).appendIf(exprs[2] != null, "over", exprs[2]));
        } else if (Skript.isRunningMinecraft(1, 21, 2)) {
            Class<?>[] classes = Particle.class.getClasses();
            Class<?> targetColorClass = null;
            for (Class<?> cls : classes) {
                if (!cls.getSimpleName().equals("TargetColor")) continue;
                targetColorClass = cls;
                break;
            }
            if (targetColorClass != null) {
                try {
                    Constructor<?> constructor = targetColorClass.getDeclaredConstructor(Location.class, org.bukkit.Color.class);
                    DataParticles.registerParticle(Particle.TRAIL, "[a[n]] %color% trail particle moving to[wards] %location%", (event, expressions, parseResult) -> {
                        Color color = (Color)expressions[0].getSingle(event);
                        org.bukkit.Color bukkitColor = color == null ? org.bukkit.Color.WHITE : color.asBukkitColor();
                        Location targetLocation = (Location)expressions[1].getSingle(event);
                        if (targetLocation == null) {
                            return null;
                        }
                        try {
                            return constructor.newInstance(targetLocation, bukkitColor);
                        }
                        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }, (exprs, parseResult, builder) -> builder.append(exprs[0], "trail particle moving to", exprs[1]));
                }
                catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (Skript.isRunningMinecraft(1, 21, 9)) {
            DataParticles.registerParticle(Particle.DRAGON_BREATH, "dragon breath particle[s] [of power %-number%]", Float.valueOf(0.5f), input -> input, (exprs, parseResult, builder) -> builder.append((Object)"dragon breath particle").appendIf(exprs[0] != null, "of power", exprs[0]));
        }
    }
}

