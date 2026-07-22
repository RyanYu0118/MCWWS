/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.util;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Timespan;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.io.StreamCorruptedException;
import java.util.Deque;
import java.util.StringJoiner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionEffect;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionEffects;
import org.skriptlang.skript.bukkit.potion.util.PotionUtils;

public class SkriptPotionEffect
implements Cloneable,
YggdrasilSerializable.YggdrasilExtendedSerializable {
    private PotionEffectType potionEffectType;
    private Integer duration = null;
    private Integer amplifier = null;
    private Boolean ambient = null;
    private Boolean particles = null;
    private Boolean icon = null;
    @Nullable
    private PotionEffect lastEffect;
    @Nullable
    private LivingEntity entitySource;
    @Nullable
    private ItemType itemSource;

    @ApiStatus.Internal
    public SkriptPotionEffect() {
    }

    public static SkriptPotionEffect fromType(PotionEffectType potionEffectType) {
        return new SkriptPotionEffect().potionEffectType(potionEffectType);
    }

    public static SkriptPotionEffect fromBukkitEffect(PotionEffect potionEffect) {
        return SkriptPotionEffect.fromType(potionEffect.getType()).duration(potionEffect.getDuration()).amplifier(potionEffect.getAmplifier()).ambient(potionEffect.isAmbient()).particles(potionEffect.hasParticles()).icon(potionEffect.hasIcon());
    }

    public static SkriptPotionEffect fromBukkitEffect(PotionEffect potionEffect, LivingEntity source) {
        SkriptPotionEffect skriptPotionEffect = SkriptPotionEffect.fromBukkitEffect(potionEffect);
        skriptPotionEffect.entitySource = source;
        return skriptPotionEffect;
    }

    public static SkriptPotionEffect fromBukkitEffect(PotionEffect potionEffect, ItemType source) {
        SkriptPotionEffect skriptPotionEffect = SkriptPotionEffect.fromBukkitEffect(potionEffect);
        skriptPotionEffect.itemSource = source;
        return skriptPotionEffect;
    }

    public PotionEffectType potionEffectType() {
        return this.potionEffectType;
    }

    @Contract(value="_ -> this")
    public SkriptPotionEffect potionEffectType(PotionEffectType potionEffectType) {
        this.lastEffect = null;
        this.withSource(() -> {
            this.potionEffectType = potionEffectType;
        });
        return this;
    }

    public boolean infinite() {
        return this.duration != null && this.duration == -1;
    }

    @Contract(value="_ -> this")
    public SkriptPotionEffect infinite(boolean infinite) {
        return this.duration(infinite ? -1 : 600);
    }

    public int duration() {
        if (this.duration == null) {
            return 600;
        }
        return this.duration;
    }

    @Contract(value="_ -> this")
    public SkriptPotionEffect duration(int duration) {
        this.lastEffect = null;
        this.withSource(() -> {
            this.duration = duration;
        });
        return this;
    }

    public int amplifier() {
        if (this.amplifier == null) {
            return 0;
        }
        return this.amplifier;
    }

    @Contract(value="_ -> this")
    public SkriptPotionEffect amplifier(int amplifier) {
        this.lastEffect = null;
        this.withSource(() -> {
            this.amplifier = amplifier;
        });
        return this;
    }

    public boolean ambient() {
        if (this.ambient == null) {
            return false;
        }
        return this.ambient;
    }

    @Contract(value="_ -> this")
    public SkriptPotionEffect ambient(boolean ambient) {
        this.lastEffect = null;
        this.withSource(() -> {
            this.ambient = ambient;
        });
        return this;
    }

    public boolean particles() {
        if (this.particles == null) {
            return true;
        }
        return this.particles;
    }

    @Contract(value="_ -> this")
    public SkriptPotionEffect particles(boolean particles) {
        this.lastEffect = null;
        this.withSource(() -> {
            this.particles = particles;
        });
        return this;
    }

    public boolean icon() {
        if (this.icon == null) {
            return true;
        }
        return this.icon;
    }

    @Contract(value="_ -> this")
    public SkriptPotionEffect icon(boolean icon) {
        this.lastEffect = null;
        this.withSource(() -> {
            this.icon = icon;
        });
        return this;
    }

    public PotionEffect asBukkitPotionEffect() {
        if (this.lastEffect == null) {
            this.lastEffect = new PotionEffect(this.potionEffectType(), this.duration(), this.amplifier(), this.ambient(), this.particles(), this.icon());
        }
        return this.lastEffect;
    }

    public String toString() {
        return this.toString(0);
    }

    public String toString(int flags) {
        StringJoiner joiner = new StringJoiner(" ");
        boolean infinite = this.infinite();
        if (infinite) {
            joiner.add("infinite");
        }
        if (this.ambient()) {
            joiner.add("ambient");
        }
        joiner.add("potion effect of").add(Classes.toString(this.potionEffectType())).add(String.valueOf(this.amplifier() + 1));
        if (!this.particles()) {
            joiner.add("without particles");
        }
        if (!this.icon()) {
            joiner.add("without an icon");
        }
        if (this.duration != null && !infinite) {
            joiner.add("for").add(new Timespan(Timespan.TimePeriod.TICK, this.duration()).toString());
        }
        return joiner.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SkriptPotionEffect)) {
            return false;
        }
        SkriptPotionEffect otherPotion = (SkriptPotionEffect)other;
        return this.potionEffectType().equals(otherPotion.potionEffectType()) && this.duration() == otherPotion.duration() && this.amplifier() == otherPotion.amplifier() && this.ambient() == otherPotion.ambient() && this.particles() == otherPotion.particles() && this.icon() == otherPotion.icon();
    }

    public boolean matchesQualities(PotionEffect potionEffect) {
        return !(this.potionEffectType() != potionEffect.getType() || this.duration != null && this.duration() != potionEffect.getDuration() || this.amplifier != null && this.amplifier() != potionEffect.getAmplifier() || this.ambient != null && this.ambient() != potionEffect.isAmbient() || this.particles != null && this.particles() != potionEffect.hasParticles() || this.icon != null && this.icon() != potionEffect.hasIcon());
    }

    private void withSource(Runnable runnable) {
        Deque<PotionEffect> hiddenEffects = null;
        if (this.entitySource != null && this.entitySource.hasPotionEffect(this.potionEffectType)) {
            hiddenEffects = PotionUtils.getHiddenEffects(this.entitySource.getPotionEffect(this.potionEffectType));
            this.entitySource.removePotionEffect(this.potionEffectType);
        } else if (this.itemSource != null) {
            PotionUtils.removePotionEffects(this.itemSource, this.potionEffectType);
        }
        runnable.run();
        if (this.entitySource != null) {
            PotionEffect thisPotionEffect = this.asBukkitPotionEffect();
            if (hiddenEffects != null) {
                for (PotionEffect hiddenEffect : hiddenEffects) {
                    if (thisPotionEffect != null && (hiddenEffect.isShorterThan(thisPotionEffect) || hiddenEffect.getAmplifier() > thisPotionEffect.getAmplifier())) {
                        this.entitySource.addPotionEffect(thisPotionEffect);
                        thisPotionEffect = null;
                    }
                    this.entitySource.addPotionEffect(hiddenEffect);
                }
            }
            if (thisPotionEffect != null) {
                this.entitySource.addPotionEffect(this.asBukkitPotionEffect());
            }
        } else if (this.itemSource != null) {
            PotionUtils.addPotionEffects(this.itemSource, this.asBukkitPotionEffect());
        }
    }

    @Override
    public Fields serialize() {
        Fields fields = new Fields();
        fields.putObject("type", this.potionEffectType);
        fields.putObject("duration", this.duration);
        fields.putObject("amplifier", this.amplifier);
        fields.putObject("ambient", this.ambient);
        fields.putObject("particles", this.particles);
        fields.putObject("icon", this.icon);
        return fields;
    }

    @Override
    public void deserialize(@NotNull Fields fields) throws StreamCorruptedException {
        this.potionEffectType = fields.getObject("type", PotionEffectType.class);
        this.duration = fields.getObject("duration", Integer.class);
        this.amplifier = fields.getObject("amplifier", Integer.class);
        this.ambient = fields.getObject("ambient", Boolean.class);
        this.particles = fields.getObject("particles", Boolean.class);
        this.icon = fields.getObject("icon", Boolean.class);
    }

    public SkriptPotionEffect clone() {
        try {
            SkriptPotionEffect skriptPotionEffect = (SkriptPotionEffect)super.clone();
            skriptPotionEffect.entitySource = null;
            skriptPotionEffect.itemSource = null;
            return skriptPotionEffect;
        }
        catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static boolean isChangeable(Expression<? extends SkriptPotionEffect> expression) {
        ExprPotionEffect exprPotionEffect;
        ExprPotionEffects exprPotionEffects;
        if (expression instanceof ExprPotionEffects && (exprPotionEffects = (ExprPotionEffects)expression).getState().includesHidden() || expression instanceof ExprPotionEffect && (exprPotionEffect = (ExprPotionEffect)expression).getState().includesHidden()) {
            Skript.error("Hidden potion effects cannot be changed");
            return false;
        }
        return true;
    }
}

