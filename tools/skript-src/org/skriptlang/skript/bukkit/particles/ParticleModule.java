/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Effect
 *  org.bukkit.EntityEffect
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Particle$DustTransition
 *  org.bukkit.Particle$Spell
 *  org.bukkit.Particle$Trail
 *  org.bukkit.Vibration
 *  org.bukkit.Vibration$Destination
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.particles;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.SimpleClassSerializer;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.particles.GameEffect;
import org.skriptlang.skript.bukkit.particles.ParticleUtils;
import org.skriptlang.skript.bukkit.particles.elements.effects.EffPlayEffect;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprGameEffectWithData;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprParticleCount;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprParticleDistribution;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprParticleOffset;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprParticleSpeed;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprParticleWithData;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprParticleWithOffset;
import org.skriptlang.skript.bukkit.particles.elements.expressions.ExprParticleWithSpeed;
import org.skriptlang.skript.bukkit.particles.particleeffects.ConvergingEffect;
import org.skriptlang.skript.bukkit.particles.particleeffects.DirectionalEffect;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;
import org.skriptlang.skript.bukkit.particles.particleeffects.ScalableEffect;
import org.skriptlang.skript.bukkit.particles.registration.DataGameEffects;
import org.skriptlang.skript.bukkit.particles.registration.DataParticles;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

public class ParticleModule
extends HierarchicalAddonModule {
    public ParticleModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void initSelf(SkriptAddon addon) {
        ParticleModule.registerClasses();
        ParticleModule.registerDataSerializers();
        DataGameEffects.getGameEffectInfos();
        DataParticles.getParticleInfos();
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, EffPlayEffect::register, ExprGameEffectWithData::register, ExprParticleCount::register, ExprParticleDistribution::register, ExprParticleOffset::register, ExprParticleSpeed::register, ExprParticleWithData::register, ExprParticleWithOffset::register, ExprParticleWithSpeed::register);
    }

    private static void registerClasses() {
        Classes.registerClass(new ClassInfo<GameEffect>(GameEffect.class, "gameeffect").user("game ?effects?").since("2.14").description("Various game effects that can be played for players, like record disc songs, splash potions breaking, or fake bone meal effects.").name("Game Effect").usage(GameEffect.getAllNamesWithoutData()).supplier(() -> {
            Effect[] effects = Effect.values();
            return Arrays.stream(effects).map(GameEffect::new).filter(effect -> effect.getData() == null).iterator();
        }).serializer(new Serializer<GameEffect>(){

            @Override
            public Fields serialize(GameEffect effect) {
                Fields fields = new Fields();
                fields.putPrimitive("name", effect.getEffect().name());
                fields.putObject("data", effect.getData());
                return fields;
            }

            @Override
            public void deserialize(GameEffect effect, Fields fields) {
                assert (false);
            }

            @Override
            protected GameEffect deserialize(Fields fields) throws StreamCorruptedException {
                GameEffect effect;
                String name = fields.getAndRemovePrimitive("name", String.class);
                try {
                    effect = new GameEffect(Effect.valueOf((String)name));
                }
                catch (IllegalArgumentException e) {
                    return null;
                }
                effect.setData(fields.getObject("data"));
                return effect;
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }

            @Override
            protected boolean canBeInstantiated() {
                return false;
            }
        }).defaultExpression(new EventValueExpression<GameEffect>(GameEffect.class)).parser(new Parser<GameEffect>(){

            @Override
            public GameEffect parse(String input, ParseContext context) {
                return GameEffect.parse(input);
            }

            @Override
            public String toString(GameEffect effect, int flags) {
                return effect.toString(flags);
            }

            @Override
            public String toVariableNameString(GameEffect o) {
                return o.getEffect().name();
            }
        }));
        Classes.registerClass(new EnumClassInfo<EntityEffect>(EntityEffect.class, "entityeffect", "entity effect").user("entity ?effects?").name("Entity Effect").description("Various entity effects that can be played for entities, like wolf howling, or villager happy.").since("2.14"));
        Classes.registerClass(new ClassInfo<Particle>(Particle.class, "bukkitparticle").name(ClassInfo.NO_DOC).since("2.14").parser(new Parser<Particle>(){

            @Override
            public Particle parse(String input, ParseContext context) {
                throw new IllegalStateException();
            }

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(Particle particle, int flags) {
                return ParticleEffect.toString(particle, flags);
            }

            @Override
            public String toVariableNameString(Particle particle) {
                return this.toString(particle, 0);
            }
        }));
        Classes.registerClass(new ClassInfo<ParticleEffect>(ParticleEffect.class, "particle").user("particle( ?effect)?s?").since("2.14").description("Various particles.").name("Particle").usage(ParticleEffect.getAllNamesWithoutData()).supplier(() -> {
            Particle[] particles = Particle.values();
            return Arrays.stream(particles).map(ParticleEffect::of).iterator();
        }).serializer(new ParticleSerializer()).defaultExpression(new EventValueExpression<ParticleEffect>(ParticleEffect.class)).parser(new Parser<ParticleEffect>(){

            @Override
            public ParticleEffect parse(String input, ParseContext context) {
                return ParticleEffect.parse(input, context);
            }

            @Override
            public String toString(ParticleEffect effect, int flags) {
                return effect.toString();
            }

            @Override
            public String toVariableNameString(ParticleEffect effect) {
                return effect.particle().name();
            }
        }));
        Classes.registerClass(new ClassInfo<ConvergingEffect>(ConvergingEffect.class, "convergingparticle").user("converging ?particle( ?effect)?s?").since("2.14").description("A particle effect where particles converge towards a point.").name("Converging Particle Effect").supplier(() -> ParticleUtils.getConvergingParticles().stream().map(ConvergingEffect::new).iterator()).serializer(new ParticleSerializer()).defaultExpression(new EventValueExpression<ConvergingEffect>(ConvergingEffect.class)).parser(new Parser<ConvergingEffect>(){

            @Override
            public ConvergingEffect parse(String input, ParseContext context) {
                ParticleEffect effect = ParticleEffect.parse(input, context);
                if (effect instanceof ConvergingEffect) {
                    ConvergingEffect convergingEffect = (ConvergingEffect)effect;
                    return convergingEffect;
                }
                return null;
            }

            @Override
            public String toString(ConvergingEffect effect, int flags) {
                return effect.toString();
            }

            @Override
            public String toVariableNameString(ConvergingEffect effect) {
                return effect.particle().name();
            }
        }));
        Classes.registerClass(new ClassInfo<DirectionalEffect>(DirectionalEffect.class, "directionalparticle").user("directional ?particle( ?effect)?s?").since("2.14").description("A particle effect which can be given a directional velocity.").name("Directional Particle Effect").supplier(() -> ParticleUtils.getDirectionalParticles().stream().map(DirectionalEffect::new).iterator()).serializer(new ParticleSerializer()).defaultExpression(new EventValueExpression<DirectionalEffect>(DirectionalEffect.class)).parser(new Parser<DirectionalEffect>(){

            @Override
            public DirectionalEffect parse(String input, ParseContext context) {
                ParticleEffect effect = ParticleEffect.parse(input, context);
                if (effect instanceof DirectionalEffect) {
                    DirectionalEffect convergingEffect = (DirectionalEffect)effect;
                    return convergingEffect;
                }
                return null;
            }

            @Override
            public String toString(DirectionalEffect effect, int flags) {
                return effect.toString();
            }

            @Override
            public String toVariableNameString(DirectionalEffect effect) {
                return effect.particle().name();
            }
        }));
        Classes.registerClass(new ClassInfo<ScalableEffect>(ScalableEffect.class, "scalableparticle").user("scalable ?particle( ?effect)?s?").since("2.14").description("A particle effect which can be scaled up or down.").name("Scalable Particle Effect").supplier(() -> ParticleUtils.getScalableParticles().stream().map(ScalableEffect::new).iterator()).serializer(new ParticleSerializer()).defaultExpression(new EventValueExpression<ScalableEffect>(ScalableEffect.class)).parser(new Parser<ScalableEffect>(){

            @Override
            public ScalableEffect parse(String input, ParseContext context) {
                ParticleEffect effect = ParticleEffect.parse(input, context);
                if (effect instanceof ScalableEffect) {
                    ScalableEffect convergingEffect = (ScalableEffect)effect;
                    return convergingEffect;
                }
                return null;
            }

            @Override
            public String toString(ScalableEffect effect, int flags) {
                return effect.toString();
            }

            @Override
            public String toVariableNameString(ScalableEffect effect) {
                return effect.particle().name();
            }
        }).property(Property.SCALE, "The scale multiplier to use for a particle. Generally larger numbers will result in larger particles.", Skript.instance(), new ExpressionPropertyHandler<ScalableEffect, Number>(){

            @Override
            @Nullable
            public Number convert(ScalableEffect propertyHolder) {
                return propertyHolder.hasScale() ? Double.valueOf(propertyHolder.scale()) : null;
            }

            @Override
            public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
                Class[] classArray;
                switch (mode) {
                    case SET: 
                    case ADD: 
                    case REMOVE: 
                    case RESET: {
                        Class[] classArray2 = new Class[1];
                        classArray = classArray2;
                        classArray2[0] = Number.class;
                        break;
                    }
                    default: {
                        classArray = null;
                    }
                }
                return classArray;
            }

            @Override
            public void change(ScalableEffect propertyHolder, Object @Nullable [] delta, Changer.ChangeMode mode) {
                double scaleDelta = delta == null ? 1.0 : ((Number)delta[0]).doubleValue();
                switch (mode) {
                    case REMOVE: {
                        scaleDelta = -scaleDelta;
                    }
                    case ADD: {
                        if (!propertyHolder.hasScale()) break;
                        propertyHolder.scale(propertyHolder.scale() + scaleDelta);
                        break;
                    }
                    case SET: {
                        propertyHolder.scale(scaleDelta);
                        break;
                    }
                    case RESET: {
                        if (!propertyHolder.hasScale()) break;
                        propertyHolder.scale(scaleDelta);
                    }
                }
            }

            @Override
            @NotNull
            public Class<Number> returnType() {
                return Number.class;
            }
        }));
    }

    private static void registerDataSerializers() {
        Variables.yggdrasil.registerSingleClass(Color.class, "particle.color");
        Variables.yggdrasil.registerClassResolver(new SimpleClassSerializer.NonInstantiableClassSerializer<Particle.DustOptions>(Particle.DustOptions.class, "particle.dustoptions"){

            @Override
            public Fields serialize(Particle.DustOptions object) {
                Fields fields = new Fields();
                fields.putObject("color", object.getColor());
                fields.putPrimitive("size", Float.valueOf(object.getSize()));
                return fields;
            }

            @Override
            protected Particle.DustOptions deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                Color color = fields.getAndRemoveObject("color", Color.class);
                float size = fields.getAndRemovePrimitive("size", Float.class).floatValue();
                if (color == null) {
                    throw new NotSerializableException("Color cannot be null for DustOptions");
                }
                return new Particle.DustOptions(color, size);
            }
        });
        Variables.yggdrasil.registerClassResolver(new SimpleClassSerializer.NonInstantiableClassSerializer<Particle.DustTransition>(Particle.DustTransition.class, "particle.dusttransition"){

            @Override
            public Fields serialize(Particle.DustTransition object) {
                Fields fields = new Fields();
                fields.putObject("fromColor", object.getColor());
                fields.putObject("toColor", object.getToColor());
                fields.putPrimitive("size", Float.valueOf(object.getSize()));
                return fields;
            }

            @Override
            protected Particle.DustTransition deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                Color fromColor = fields.getAndRemoveObject("fromColor", Color.class);
                Color toColor = fields.getAndRemoveObject("toColor", Color.class);
                float size = fields.getAndRemovePrimitive("size", Float.class).floatValue();
                if (fromColor == null || toColor == null) {
                    throw new NotSerializableException("Colors cannot be null for DustTransition");
                }
                return new Particle.DustTransition(fromColor, toColor, size);
            }
        });
        Variables.yggdrasil.registerClassResolver(new SimpleClassSerializer.NonInstantiableClassSerializer<Vibration>(Vibration.class, "particle.vibration"){

            @Override
            public Fields serialize(Vibration object) {
                Fields fields = new Fields();
                fields.putObject("destination", object.getDestination());
                fields.putPrimitive("arrivalTime", object.getArrivalTime());
                return fields;
            }

            @Override
            protected Vibration deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                Vibration.Destination destination = fields.getAndRemoveObject("destination", Vibration.Destination.class);
                int arrivalTime = fields.getAndRemovePrimitive("arrivalTime", Integer.class);
                if (destination == null) {
                    throw new NotSerializableException("Destination cannot be null for Vibration");
                }
                return new Vibration(destination, arrivalTime);
            }
        });
        if (Skript.isRunningMinecraft(1, 21, 9)) {
            Variables.yggdrasil.registerClassResolver(new SimpleClassSerializer.NonInstantiableClassSerializer<Particle.Spell>(Particle.Spell.class, "particle.spell"){

                @Override
                public Fields serialize(Particle.Spell object) {
                    Fields fields = new Fields();
                    fields.putObject("color", object.getColor());
                    fields.putPrimitive("power", Float.valueOf(object.getPower()));
                    return fields;
                }

                @Override
                protected Particle.Spell deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                    Color color = fields.getAndRemoveObject("color", Color.class);
                    float power = fields.getAndRemovePrimitive("power", Float.class).floatValue();
                    if (color == null) {
                        throw new NotSerializableException("Color cannot be null for Spell");
                    }
                    return new Particle.Spell(color, power);
                }
            });
        }
        if (Skript.isRunningMinecraft(1, 21, 4)) {
            Variables.yggdrasil.registerClassResolver(new SimpleClassSerializer.NonInstantiableClassSerializer<Particle.Trail>(Particle.Trail.class, "particle.trail"){

                @Override
                public Fields serialize(Particle.Trail object) {
                    Fields fields = new Fields();
                    fields.putObject("target", object.getTarget());
                    fields.putObject("color", object.getColor());
                    fields.putPrimitive("duration", object.getDuration());
                    return fields;
                }

                @Override
                protected Particle.Trail deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                    Location target = fields.getAndRemoveObject("target", Location.class);
                    Color color = fields.getAndRemoveObject("color", Color.class);
                    int duration = 20;
                    if (fields.hasField("duration")) {
                        duration = fields.getAndRemovePrimitive("duration", Integer.class);
                    }
                    if (target == null) {
                        throw new NotSerializableException("Target cannot be null for Trail");
                    }
                    if (color == null) {
                        throw new NotSerializableException("Color cannot be null for Trail");
                    }
                    return new Particle.Trail(target, color, duration);
                }
            });
        } else if (Skript.isRunningMinecraft(1, 21, 2)) {
            Class targetColorClass = Arrays.stream(Particle.class.getClasses()).filter(c -> c.getSimpleName().equals("TargetColor")).findFirst().orElse(null);
            if (targetColorClass == null) {
                throw new RuntimeException("Could not find Particle.TargetColor class for serializer");
            }
            try {
                final Constructor constructor = targetColorClass.getDeclaredConstructor(Location.class, Color.class);
                final Method getTargetMethod = targetColorClass.getDeclaredMethod("getTarget", new Class[0]);
                final Method getColorMethod = targetColorClass.getDeclaredMethod("getColor", new Class[0]);
                Variables.yggdrasil.registerClassResolver(new SimpleClassSerializer.NonInstantiableClassSerializer<Object>(targetColorClass, "particle.targetcolor"){

                    @Override
                    public Fields serialize(Object object) {
                        Fields fields = new Fields();
                        try {
                            fields.putObject("target", getTargetMethod.invoke(object, new Object[0]));
                            fields.putObject("color", getColorMethod.invoke(object, new Object[0]));
                        }
                        catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                        return fields;
                    }

                    @Override
                    protected Object deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        Location target = fields.getAndRemoveObject("target", Location.class);
                        Color color = fields.getAndRemoveObject("color", Color.class);
                        if (target == null) {
                            throw new NotSerializableException("Target cannot be null for Trail");
                        }
                        if (color == null) {
                            throw new NotSerializableException("Color cannot be null for Trail");
                        }
                        try {
                            return constructor.newInstance(target, color);
                        }
                        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String name() {
        return "particle";
    }

    static class ParticleSerializer
    extends Serializer<ParticleEffect> {
        ParticleSerializer() {
        }

        @Override
        public Fields serialize(ParticleEffect effect) {
            Fields fields = new Fields();
            fields.putObject("name", effect.particle().name());
            fields.putPrimitive("count", effect.count());
            fields.putPrimitive("offsetX", effect.offsetX());
            fields.putPrimitive("offsetY", effect.offsetY());
            fields.putPrimitive("offsetZ", effect.offsetZ());
            fields.putPrimitive("extra", effect.extra());
            fields.putObject("data", effect.data());
            fields.putPrimitive("force", effect.force());
            return fields;
        }

        @Override
        public void deserialize(ParticleEffect effect, Fields fields) {
            assert (false);
        }

        @Override
        protected ParticleEffect deserialize(Fields fields) throws StreamCorruptedException {
            ParticleEffect effect;
            String name = fields.getAndRemoveObject("name", String.class);
            try {
                effect = ParticleEffect.of(Particle.valueOf((String)name));
            }
            catch (IllegalArgumentException e) {
                return null;
            }
            return effect.count(fields.getAndRemovePrimitive("count", Integer.class)).offset(fields.getAndRemovePrimitive("offsetX", Double.class), fields.getAndRemovePrimitive("offsetY", Double.class), fields.getAndRemovePrimitive("offsetZ", Double.class)).extra(fields.getAndRemovePrimitive("extra", Double.class)).force(fields.getAndRemovePrimitive("force", Boolean.class)).data(fields.getAndRemoveObject("data", effect.particle().getDataType()));
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }

        @Override
        protected boolean canBeInstantiated() {
            return false;
        }
    }
}

