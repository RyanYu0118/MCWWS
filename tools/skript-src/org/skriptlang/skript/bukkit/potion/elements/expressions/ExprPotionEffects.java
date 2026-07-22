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
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.util.PotionUtils;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Potion Effects of Entity/Item")
@Description(value={"An expression to obtain the active or hidden potion effects of an entity or item.", "When an entity is affected by a potion effect but already has a weaker version of that effect type, the weaker version becomes hidden. If the weaker version has a longer duration, it returns after the stronger version expires.", "NOTE: Hidden effects are not able to be changed.", "NOTE: Clearing the base potion effects of a potion item is not possible. If you wish to do so, just set the item to a water bottle."})
@Example.Examples(value={@Example(value="set {_effects::*} to the active potion effects of the player"), @Example(value="clear the player's hidden potion effects"), @Example(value="add the potion effects of the player to the potion effects of the player's tool"), @Example(value="reset the potion effects of the player's tool"), @Example(value="remove speed and night vision from the potion effects of the player")})
@RequiredPlugins(value={"Paper 1.20.4+ for hidden effects"})
@Since(value={"2.5.2, 2.14 (active/hidden support, more change modes)"})
public class ExprPotionEffects
extends PropertyExpression<Object, SkriptPotionEffect> {
    private State state;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprPotionEffects.infoBuilder(ExprPotionEffects.class, SkriptPotionEffect.class, "[:active|:hidden|both:(active and hidden|hidden and active)] potion effects", "livingentities/itemtypes", false).supplier(ExprPotionEffects::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.state = State.fromParseTag(parseResult.tags.isEmpty() ? "" : parseResult.tags.getFirst());
        if (this.state.includesHidden() && ItemType.class.isAssignableFrom(this.getExpr().getReturnType())) {
            Skript.error("Items (such as potions or stews) do not have hidden effects");
            return false;
        }
        return true;
    }

    protected SkriptPotionEffect[] get(Event event, Object[] source) {
        ArrayList<SkriptPotionEffect> potionEffects = new ArrayList<SkriptPotionEffect>();
        for (Object object : source) {
            if (object instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)object;
                for (PotionEffect potionEffect : livingEntity.getActivePotionEffects()) {
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
            for (PotionEffect potionEffect : PotionUtils.getPotionEffects(itemType)) {
                potionEffects.add(SkriptPotionEffect.fromBukkitEffect(potionEffect, itemType));
            }
        }
        return potionEffects.toArray(new SkriptPotionEffect[0]);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            default -> throw new MatchException(null, null);
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET -> {
                if (this.state.includesHidden()) {
                    Skript.error("The hidden potion effects of an entity cannot be set or added to.");
                    yield null;
                }
                yield CollectionUtils.array(PotionEffect[].class);
            }
            case Changer.ChangeMode.REMOVE -> CollectionUtils.array(SkriptPotionEffect[].class);
            case Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET, Changer.ChangeMode.REMOVE_ALL -> CollectionUtils.array(PotionEffectType[].class);
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        F[] holders = this.getExpr().getArray(event);
        switch (mode) {
            case SET: 
            case DELETE: 
            case RESET: {
                ItemType itemType;
                LivingEntity entity;
                for (Object holder : holders) {
                    if (holder instanceof LivingEntity) {
                        entity = (LivingEntity)holder;
                        this.reset(entity);
                        continue;
                    }
                    if (!(holder instanceof ItemType)) continue;
                    itemType = (ItemType)holder;
                    PotionUtils.clearPotionEffects(itemType);
                }
                if (mode != Changer.ChangeMode.SET) break;
            }
            case ADD: {
                ItemType itemType;
                LivingEntity entity;
                assert (delta != null);
                for (Object holder : holders) {
                    if (holder instanceof LivingEntity) {
                        entity = (LivingEntity)holder;
                        for (Object object : delta) {
                            entity.addPotionEffect((PotionEffect)object);
                        }
                        continue;
                    }
                    if (!(holder instanceof ItemType)) continue;
                    itemType = (ItemType)holder;
                    for (Object object : delta) {
                        PotionUtils.addPotionEffects(itemType, (PotionEffect)object);
                    }
                }
                break;
            }
            case REMOVE: {
                assert (delta != null);
                for (Object holder : holders) {
                    if (holder instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity)holder;
                        for (Object object : delta) {
                            this.remove(entity, (SkriptPotionEffect)object);
                        }
                        continue;
                    }
                    if (!(holder instanceof ItemType)) continue;
                    ItemType itemType = (ItemType)holder;
                    for (Object object : delta) {
                        this.remove(itemType, (SkriptPotionEffect)object);
                    }
                }
                break;
            }
            case REMOVE_ALL: {
                assert (delta != null);
                for (Object holder : holders) {
                    if (holder instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity)holder;
                        for (Object object : delta) {
                            entity.removePotionEffect((PotionEffectType)object);
                        }
                        continue;
                    }
                    if (!(holder instanceof ItemType)) continue;
                    ItemType itemType = (ItemType)holder;
                    for (Object object : delta) {
                        PotionUtils.removePotionEffects(itemType, (PotionEffectType)object);
                    }
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    private void reset(LivingEntity entity) {
        Collection potionEffects = entity.getActivePotionEffects();
        if (this.state == State.ACTIVE) {
            for (PotionEffect potionEffect : potionEffects) {
                Deque<PotionEffect> hiddenEffects = PotionUtils.getHiddenEffects(potionEffect);
                entity.removePotionEffect(potionEffect.getType());
                entity.addPotionEffects(hiddenEffects);
            }
        } else if (this.state == State.HIDDEN) {
            for (PotionEffect potionEffect : potionEffects) {
                entity.removePotionEffect(potionEffect.getType());
                entity.addPotionEffect(potionEffect);
            }
        } else {
            for (PotionEffect potionEffect : potionEffects) {
                entity.removePotionEffect(potionEffect.getType());
            }
        }
    }

    private void remove(LivingEntity entity, SkriptPotionEffect potionEffect) {
        PotionEffect entityEffect = entity.getPotionEffect(potionEffect.potionEffectType());
        if (entityEffect == null) {
            return;
        }
        Deque<PotionEffect> effects = PotionUtils.getHiddenEffects(entityEffect);
        boolean madeChanges = false;
        if (this.state.includesHidden()) {
            Iterator<PotionEffect> effectsIterator = effects.iterator();
            while (effectsIterator.hasNext()) {
                if (!potionEffect.matchesQualities(effectsIterator.next())) continue;
                effectsIterator.remove();
                madeChanges = true;
            }
        }
        if (this.state == State.HIDDEN || !potionEffect.matchesQualities(entityEffect)) {
            effects.addLast(entityEffect);
        } else {
            madeChanges = true;
        }
        if (madeChanges) {
            entity.removePotionEffect(entityEffect.getType());
            entity.addPotionEffects(effects);
        }
    }

    private void remove(ItemType itemType, SkriptPotionEffect potionEffect) {
        for (PotionEffect itemEffect : PotionUtils.getPotionEffects(itemType)) {
            if (!potionEffect.matchesQualities(itemEffect)) continue;
            PotionUtils.removePotionEffects(itemType, potionEffect.potionEffectType());
            break;
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends SkriptPotionEffect> getReturnType() {
        return SkriptPotionEffect.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the " + this.state.displayName() + " potion effects of " + this.getExpr().toString(event, debug);
    }

    @ApiStatus.Internal
    public State getState() {
        return this.state;
    }

    @ApiStatus.Internal
    public static enum State {
        UNSET,
        ACTIVE,
        HIDDEN,
        BOTH;


        static State fromParseTag(String tag) {
            return switch (tag) {
                case "active" -> ACTIVE;
                case "hidden" -> HIDDEN;
                case "both" -> BOTH;
                default -> UNSET;
            };
        }

        boolean includesActive() {
            return this != HIDDEN;
        }

        public boolean includesHidden() {
            return this == HIDDEN || this == BOTH;
        }

        public String displayName() {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> "";
                case 1 -> "active";
                case 2 -> "hidden";
                case 3 -> "active and hidden";
            };
        }
    }
}

