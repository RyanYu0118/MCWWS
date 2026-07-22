/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Registry
 *  org.bukkit.event.entity.EntityPotionEffectEvent$Action
 *  org.bukkit.event.entity.EntityPotionEffectEvent$Cause
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.potion.PotionEffectTypeCategory
 */
package org.skriptlang.skript.bukkit.potion;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.BukkitUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.classes.YggdrasilSerializer;
import ch.njol.skript.classes.registry.RegistryClassInfo;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import org.bukkit.Registry;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.potion.elements.conditions.CondHasPotion;
import org.skriptlang.skript.bukkit.potion.elements.conditions.CondIsPoisoned;
import org.skriptlang.skript.bukkit.potion.elements.conditions.CondIsPotionAmbient;
import org.skriptlang.skript.bukkit.potion.elements.conditions.CondIsPotionInstant;
import org.skriptlang.skript.bukkit.potion.elements.conditions.CondPotionHasIcon;
import org.skriptlang.skript.bukkit.potion.elements.conditions.CondPotionHasParticles;
import org.skriptlang.skript.bukkit.potion.elements.effects.EffApplyPotionEffect;
import org.skriptlang.skript.bukkit.potion.elements.effects.EffPoison;
import org.skriptlang.skript.bukkit.potion.elements.effects.EffPotionAmbient;
import org.skriptlang.skript.bukkit.potion.elements.effects.EffPotionIcon;
import org.skriptlang.skript.bukkit.potion.elements.effects.EffPotionInfinite;
import org.skriptlang.skript.bukkit.potion.elements.effects.EffPotionParticles;
import org.skriptlang.skript.bukkit.potion.elements.events.EvtEntityPotion;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionAmplifier;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionDuration;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionEffect;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionEffectTypeCategory;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionEffects;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprSecPotionEffect;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprSkriptPotionEffect;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;

public class PotionModule
extends HierarchicalAddonModule {
    public PotionModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void initSelf(SkriptAddon addon) {
        Registry petRegistry;
        Classes.registerClass(new ClassInfo<SkriptPotionEffect>(SkriptPotionEffect.class, "skriptpotioneffect").name(ClassInfo.NO_DOC).defaultExpression(new EventValueExpression<SkriptPotionEffect>(SkriptPotionEffect.class)).parser(new Parser<SkriptPotionEffect>(this){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(SkriptPotionEffect potionEffect, int flags) {
                return potionEffect.toString(flags);
            }

            @Override
            public String toVariableNameString(SkriptPotionEffect potionEffect) {
                return "potion_effect:" + potionEffect.potionEffectType().getKey().getKey();
            }
        }).serializer(new YggdrasilSerializer()));
        Classes.registerClass(new ClassInfo<PotionEffect>(PotionEffect.class, "potioneffect").user("potion ?effects?").name("Potion Effect").description("A potion effect, including the potion effect type, tier and duration.").usage("speed of tier 1 for 10 seconds").since("2.5.2").parser(new Parser<PotionEffect>(this){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(PotionEffect potionEffect, int flags) {
                return SkriptPotionEffect.fromBukkitEffect(potionEffect).toString(flags);
            }

            @Override
            public String toVariableNameString(PotionEffect potionEffect) {
                return "potion_effect:" + potionEffect.getType().getKey().getKey();
            }
        }).serializer(new Serializer<PotionEffect>(this){

            @Override
            public Fields serialize(PotionEffect potionEffect) {
                Fields fields = new Fields();
                fields.putObject("potion", SkriptPotionEffect.fromBukkitEffect(potionEffect));
                return fields;
            }

            @Override
            public void deserialize(PotionEffect potionEffect, Fields fields) {
                assert (false);
            }

            @Override
            protected PotionEffect deserialize(Fields fields) throws StreamCorruptedException {
                if (!fields.hasField("potion")) {
                    String typeName = fields.getObject("type", String.class);
                    assert (typeName != null);
                    PotionEffectType type = PotionEffectType.getByName((String)typeName);
                    if (type == null) {
                        throw new StreamCorruptedException("Invalid PotionEffectType " + typeName);
                    }
                    int amplifier = fields.getPrimitive("amplifier", Integer.TYPE);
                    int duration = fields.getPrimitive("duration", Integer.TYPE);
                    boolean particles = fields.getPrimitive("particles", Boolean.TYPE);
                    boolean ambient = fields.getPrimitive("ambient", Boolean.TYPE);
                    return new PotionEffect(type, duration, amplifier, ambient, particles);
                }
                SkriptPotionEffect potionEffect = fields.getObject("potion", SkriptPotionEffect.class);
                if (potionEffect == null) {
                    throw new StreamCorruptedException();
                }
                return potionEffect.asBukkitPotionEffect();
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }

            @Override
            protected boolean canBeInstantiated() {
                return false;
            }
        }));
        if (BukkitUtils.registryExists("MOB_EFFECT")) {
            petRegistry = Registry.MOB_EFFECT;
        } else if (BukkitUtils.registryExists("EFFECT")) {
            petRegistry = Registry.EFFECT;
        } else {
            throw new IllegalStateException("Potion effect registry does not exist");
        }
        Classes.registerClass(new RegistryClassInfo<PotionEffectType>(PotionEffectType.class, petRegistry, "potioneffecttype", "potion effect types", false).user("potion ?effect ?types?").name("Potion Effect Type").description("A potion effect type, e.g. 'strength' or 'swiftness'.").examples("apply swiftness 5 to the player", "apply potion of speed 2 to the player for 60 seconds", "remove invisibility from the victim").since("2.0 beta 3"));
        Classes.registerClass(new EnumClassInfo<EntityPotionEffectEvent.Cause>(EntityPotionEffectEvent.Cause.class, "potioncause", "potion causes").user("(entity ?)?potion ?effect ?causes?").name("Potion Effect Event Cause").description("Represents the cause of an 'entity potion effect' event. For example, an arrow hitting an entity or a command being executed.").examples("on entity potion effect:\n\tif the event-potion effect cause is arrow affliction:\n\t\tmessage \"You were hit by a tipped arrow!\"\n").since("2.10"));
        Classes.registerClass(new EnumClassInfo<EntityPotionEffectEvent.Action>(EntityPotionEffectEvent.Action.class, "potionaction", "potion actions").user("(entity ?)?potion ?effect ?actions?").name("Potion Effect Event Action").description("Represents the action being performed in an 'entity potion effect' event.", "'added' indicates the entity does not already have a potion effect of the event potion effect type.", "'changed' indicates the entity already has a potion effect of the event potion effect type, but some property about the potion effect is changing.", "'cleared' indicates that the effect is being removed because all of the entity's effects are being removed.", "'removed' indicates that the event potion effect type has been specifically removed from the entity.").examples("on entity potion effect:\n\tif the event-potion effect action is removal:\n\t\tmessage \"One of your existing potion effects was removed!\"\n").since("2.14"));
        if (Skript.classExists("org.bukkit.potion.PotionEffectTypeCategory")) {
            Classes.registerClass(new EnumClassInfo<PotionEffectTypeCategory>(PotionEffectTypeCategory.class, "potioneffecttypecategory", "potion effect type categories").user("potion ?effect ?type? categor(y|ies)").name("Potion Effect Type Category").description("Represents the type of effect a potion effect type has on an entity.").since("2.14"));
            Comparators.registerComparator(PotionEffectType.class, PotionEffectTypeCategory.class, (type, category) -> Relation.get(type.getCategory() == category));
        }
        Converters.registerConverter(SkriptPotionEffect.class, PotionEffect.class, SkriptPotionEffect::asBukkitPotionEffect, 3);
        Converters.registerConverter(PotionEffect.class, SkriptPotionEffect.class, SkriptPotionEffect::fromBukkitEffect, 3);
        Converters.registerConverter(PotionEffectType.class, SkriptPotionEffect.class, SkriptPotionEffect::fromType, 3);
        Converters.registerConverter(SkriptPotionEffect.class, PotionEffectType.class, SkriptPotionEffect::potionEffectType, 3);
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, CondHasPotion::register, CondIsPoisoned::register, CondIsPotionAmbient::register, CondIsPotionInstant::register, CondPotionHasIcon::register, CondPotionHasParticles::register, EffApplyPotionEffect::register, EffPoison::register, EffPotionAmbient::register, EffPotionIcon::register, EffPotionInfinite::register, EffPotionParticles::register, EvtEntityPotion::register, ExprPotionAmplifier::register, ExprPotionDuration::register, ExprPotionEffect::register, ExprPotionEffects::register, ExprPotionEffectTypeCategory::register, ExprSecPotionEffect::register, ExprSkriptPotionEffect::register);
    }

    @Override
    public String name() {
        return "potion";
    }
}

