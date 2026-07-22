/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionDuration;
import org.skriptlang.skript.bukkit.potion.elements.expressions.ExprPotionEffects;
import org.skriptlang.skript.bukkit.potion.util.PotionUtils;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Potion Effect of Entity/Item")
@Description(value={"An expression to obtain a specific potion effect type of an entity or item.", "When an entity is affected by a potion effect but already has a weaker version of that effect type, the weaker version becomes hidden. If the weaker version has a longer duration, it returns after the stronger version expires.", "NOTE: Hidden effects are not able to be changed."})
@Example.Examples(value={@Example(value="set {_effect} to the player's active speed effect"), @Example(value="add 10 seconds to the player's slowness effect"), @Example(value="clear the player's hidden strength effects"), @Example(value="reset the player's weakness effects"), @Example(value="delete the player's active jump boost effect")})
@RequiredPlugins(value={"Paper 1.20.4+ for hidden effects"})
@Since(value={"2.14"})
public class ExprPotionEffect
extends PropertyExpression<Object, SkriptPotionEffect> {
    private Expression<PotionEffectType> types;
    private ExprPotionEffects.State state;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprPotionEffect.infoBuilder(ExprPotionEffect.class, SkriptPotionEffect.class, "[:active|:hidden|both:(active and hidden|hidden and active)] %potioneffecttypes% [potion] effect[s]", "livingentities/itemtypes", false).supplier(ExprPotionEffect::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.types = expressions[matchedPattern % 2];
        this.setExpr(expressions[(matchedPattern + 1) % 2]);
        this.state = ExprPotionEffects.State.fromParseTag(parseResult.tags.isEmpty() ? "" : parseResult.tags.getFirst());
        if (this.state.includesHidden() && ItemType.class.isAssignableFrom(this.getExpr().getReturnType())) {
            Skript.error("Items (such as potions or stews) do not have hidden effects");
            return false;
        }
        return true;
    }

    protected SkriptPotionEffect[] get(Event event, Object[] source) {
        ArrayList<SkriptPotionEffect> potionEffects = new ArrayList<SkriptPotionEffect>();
        PotionEffectType[] types = this.types.getArray(event);
        for (Object object : source) {
            if (object instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)object;
                for (PotionEffectType type : types) {
                    PotionEffect potionEffect = livingEntity.getPotionEffect(type);
                    if (potionEffect == null) continue;
                    if (this.state.includesActive()) {
                        potionEffects.add(SkriptPotionEffect.fromBukkitEffect(potionEffect, livingEntity));
                    }
                    if (!this.state.includesHidden()) continue;
                    for (PotionEffect hiddenEffect = potionEffect.getHiddenPotionEffect(); hiddenEffect != null; hiddenEffect = hiddenEffect.getHiddenPotionEffect()) {
                        potionEffects.add(SkriptPotionEffect.fromBukkitEffect(hiddenEffect));
                    }
                }
                continue;
            }
            if (!(object instanceof ItemType)) continue;
            ItemType itemType = (ItemType)object;
            block3: for (PotionEffect effect : PotionUtils.getPotionEffects(itemType)) {
                for (PotionEffectType type : types) {
                    if (!type.equals(effect.getType())) continue;
                    potionEffects.add(SkriptPotionEffect.fromBukkitEffect(effect, itemType));
                    continue block3;
                }
            }
        }
        return potionEffects.toArray(new SkriptPotionEffect[0]);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Timespan.class);
            case Changer.ChangeMode.REMOVE -> {
                if (this.state.includesHidden()) {
                    yield CollectionUtils.array(SkriptPotionEffect[].class, Timespan.class);
                }
                yield CollectionUtils.array(Timespan.class);
            }
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        F[] holders = this.getExpr().getArray(event);
        PotionEffectType[] types = this.types.getArray(event);
        switch (mode) {
            case DELETE: 
            case RESET: {
                for (Object holder : holders) {
                    if (holder instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity)holder;
                        this.reset(entity, types);
                        continue;
                    }
                    if (!(holder instanceof ItemType)) continue;
                    ItemType itemType = (ItemType)holder;
                    PotionUtils.removePotionEffects(itemType, types);
                }
                break;
            }
            case ADD: 
            case REMOVE: {
                assert (delta != null);
                for (Object holder : holders) {
                    if (holder instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity)holder;
                        this.modify(entity, types, delta, mode);
                        continue;
                    }
                    if (!(holder instanceof ItemType)) continue;
                    ItemType itemType = (ItemType)holder;
                    Object object = delta[0];
                    if (!(object instanceof Timespan)) continue;
                    Timespan change = (Timespan)object;
                    this.modify(itemType, types, change, mode);
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    private void reset(LivingEntity entity, PotionEffectType[] types) {
        if (this.state == ExprPotionEffects.State.ACTIVE) {
            for (PotionEffectType type : types) {
                PotionEffect potionEffect = entity.getPotionEffect(type);
                if (potionEffect == null) continue;
                Deque<PotionEffect> hiddenEffects = PotionUtils.getHiddenEffects(potionEffect);
                entity.removePotionEffect(type);
                entity.addPotionEffects(hiddenEffects);
            }
        } else if (this.state == ExprPotionEffects.State.HIDDEN) {
            for (PotionEffectType type : types) {
                PotionEffect original = entity.getPotionEffect(type);
                entity.removePotionEffect(type);
                if (original == null) continue;
                entity.addPotionEffect(original);
            }
        } else {
            for (PotionEffectType type : types) {
                entity.removePotionEffect(type);
            }
        }
    }

    private void modify(LivingEntity entity, PotionEffectType[] types, Object[] delta, Changer.ChangeMode mode) {
        for (PotionEffectType type : types) {
            Deque<PotionEffect> effects;
            Deque<PotionEffect> finalEffects;
            PotionEffect potionEffect = entity.getPotionEffect(type);
            if (potionEffect == null) continue;
            boolean madeChanges = false;
            if (this.state.includesHidden()) {
                finalEffects = new ArrayDeque<PotionEffect>();
                effects = PotionUtils.getHiddenEffects(potionEffect);
            } else {
                finalEffects = PotionUtils.getHiddenEffects(potionEffect);
                effects = new ArrayDeque<PotionEffect>();
            }
            if (this.state.includesActive()) {
                effects.addLast(potionEffect);
            }
            block1: for (PotionEffect effect : effects) {
                SkriptPotionEffect skriptEffect = SkriptPotionEffect.fromBukkitEffect(effect);
                for (Object object : delta) {
                    SkriptPotionEffect base;
                    if (object instanceof Timespan) {
                        Timespan timespan = (Timespan)object;
                        ExprPotionDuration.changeSafe(skriptEffect, timespan, mode);
                        madeChanges = true;
                        continue;
                    }
                    if (!(object instanceof SkriptPotionEffect) || !(base = (SkriptPotionEffect)object).matchesQualities(effect)) continue;
                    madeChanges = true;
                    continue block1;
                }
                finalEffects.addLast(skriptEffect.asBukkitPotionEffect());
            }
            if (!madeChanges) {
                return;
            }
            if (!this.state.includesActive()) {
                effects.addLast(potionEffect);
            }
            entity.removePotionEffect(type);
            entity.addPotionEffects(finalEffects);
        }
    }

    private void modify(ItemType itemType, PotionEffectType[] types, Timespan change, Changer.ChangeMode mode) {
        block0: for (PotionEffect effect : PotionUtils.getPotionEffects(itemType)) {
            for (PotionEffectType type : types) {
                if (!type.equals(effect.getType())) continue;
                ExprPotionDuration.changeSafe(SkriptPotionEffect.fromBukkitEffect(effect, itemType), change, mode);
                continue block0;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return this.types.isSingle() && !this.state.includesHidden();
    }

    @Override
    public Class<? extends SkriptPotionEffect> getReturnType() {
        return SkriptPotionEffect.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append(("the " + this.state.displayName()).stripTrailing(), this.types);
        if (this.isSingle()) {
            builder.append((Object)"effect");
        } else {
            builder.append((Object)"effects");
        }
        builder.append("of", this.getExpr());
        return builder.toString();
    }

    @ApiStatus.Internal
    public ExprPotionEffects.State getState() {
        return this.state;
    }
}

