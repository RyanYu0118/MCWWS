/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.damage.DamageSource
 *  org.bukkit.entity.Damageable
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.DamageUtils;
import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import java.util.function.Consumer;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Damageable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

@Name(value="Damage/Heal/Repair")
@Description(value={"Damage, heal, or repair an entity or item.", "Servers running Spigot 1.20.4+ can optionally choose to specify a fake damage cause."})
@Example.Examples(value={@Example(value="damage player by 5 hearts"), @Example(value="damage player by 3 hearts with fake cause fall"), @Example(value="heal the player"), @Example(value="repair tool of player")})
@Since(value={"1.0, 2.10 (damage cause)"})
@RequiredPlugins(value={"Spigot 1.20.4+ (for damage cause)"})
public class EffHealth
extends Effect
implements SyntaxRuntimeErrorProducer {
    private static final boolean SUPPORTS_DAMAGE_SOURCE = Skript.classExists("org.bukkit.damage.DamageSource");
    private static final Patterns<EffectType> PATTERNS = !SUPPORTS_DAMAGE_SOURCE ? new Patterns(new Object[][]{{"damage %livingentities/itemtypes/slots% by %number% [heart[s]]", EffectType.DAMAGE}, {"heal %livingentities% [by %-number% [heart[s]]]", EffectType.HEAL}, {"repair %itemtypes/slots% [by %-number%]", EffectType.REPAIR}}) : new Patterns(new Object[][]{{"damage %livingentities/itemtypes/slots% by %number% [heart[s]]", EffectType.DAMAGE}, {"damage %livingentities% by %number% [heart[s]] with [fake] [damage] cause %damagecause%", EffectType.DAMAGE}, {"damage %livingentities% by %number% [heart[s]] (using|with) %damagesource% [as the source]", EffectType.DAMAGE}, {"heal %livingentities% [by %-number% [heart[s]]]", EffectType.HEAL}, {"repair %itemtypes/slots% [by %-number%]", EffectType.REPAIR}});
    private Expression<?> damageables;
    @Nullable
    private Expression<Number> amount = null;
    private EffectType effectType;
    @Nullable
    private Expression<?> damageCause = null;
    @Nullable
    private Expression<?> damageSource = null;
    private Node node;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.effectType = PATTERNS.getInfo(matchedPattern);
        this.damageables = exprs[0];
        this.amount = exprs[1];
        if (this.effectType == EffectType.DAMAGE && SUPPORTS_DAMAGE_SOURCE) {
            if (matchedPattern == 1) {
                this.damageCause = exprs[2];
            } else if (matchedPattern == 2) {
                this.damageSource = exprs[2];
            }
        }
        this.node = this.getParser().getNode();
        return true;
    }

    @Override
    protected void execute(Event event) {
        Double amount = null;
        if (this.amount != null) {
            Number amountPostCheck = this.amount.getSingle(event);
            if (amountPostCheck == null) {
                return;
            }
            amount = amountPostCheck.doubleValue();
        }
        Object damageData = null;
        if (this.damageCause != null) {
            damageData = this.damageCause.getSingle(event);
        } else if (this.damageSource != null) {
            damageData = this.damageSource.getSingle(event);
        }
        for (Object obj : this.damageables.getArray(event)) {
            if (obj instanceof ItemType) {
                ItemType itemType = (ItemType)obj;
                this.handleItem(itemType.getRandom(), amount, integer -> ItemUtils.setDamage(itemType, (int)integer));
                continue;
            }
            if (obj instanceof Slot) {
                Slot slot = (Slot)obj;
                ItemStack itemStack = slot.getItem();
                if (itemStack == null) continue;
                this.handleItem(itemStack, amount, integer -> {
                    ItemUtils.setDamage(itemStack, (int)integer);
                    slot.setItem(itemStack);
                });
                continue;
            }
            if (!(obj instanceof Damageable)) continue;
            Damageable damageable = (Damageable)obj;
            this.handleDamageable(damageable, amount, damageData);
        }
    }

    private void handleItem(ItemStack itemStack, @Nullable Double amount, Consumer<Integer> consumer) {
        Integer value = null;
        if (this.effectType == EffectType.DAMAGE) {
            assert (amount != null);
            value = Math2.fit(0, ItemUtils.getDamage(itemStack) + amount.intValue(), ItemUtils.getMaxDamage(itemStack));
        } else if (this.effectType == EffectType.REPAIR) {
            value = amount == null ? 0 : Math2.fit(0, ItemUtils.getDamage(itemStack) - amount.intValue(), ItemUtils.getMaxDamage(itemStack));
        }
        if (value != null) {
            consumer.accept(value);
        }
    }

    private void handleDamageable(Damageable damageable, @Nullable Double amount, @Nullable Object object) {
        if (this.effectType == EffectType.DAMAGE) {
            assert (amount != null);
            if (SUPPORTS_DAMAGE_SOURCE && object != null) {
                if (object instanceof EntityDamageEvent.DamageCause) {
                    EntityDamageEvent.DamageCause damageCause = (EntityDamageEvent.DamageCause)object;
                    HealthUtils.damage(damageable, amount, DamageUtils.getDamageSourceFromCause(damageCause));
                    return;
                }
                if (object instanceof DamageSource) {
                    DamageSource damageSource = (DamageSource)object;
                    HealthUtils.damage(damageable, amount, damageSource);
                    return;
                }
            }
            HealthUtils.damage(damageable, amount);
        } else if (this.effectType == EffectType.HEAL) {
            if (amount == null) {
                HealthUtils.heal(damageable, HealthUtils.getMaxHealth(damageable));
            } else {
                HealthUtils.heal(damageable, amount);
            }
        }
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        switch (this.effectType.ordinal()) {
            case 0: {
                assert (this.amount != null);
                builder.append("damage", this.damageables, "by", this.amount);
                if (this.damageCause != null) {
                    builder.append("with fake damage cause", this.damageCause);
                    break;
                }
                if (this.damageSource == null) break;
                builder.append("using", this.damageSource);
                break;
            }
            case 1: {
                builder.append("heal", this.damageables);
                if (this.amount == null) break;
                builder.append("by", this.amount);
                break;
            }
            case 2: {
                builder.append("repair", this.damageables);
                if (this.amount == null) break;
                builder.append("by", this.amount);
            }
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffHealth.class, PATTERNS.getPatterns());
    }

    private static enum EffectType {
        DAMAGE,
        HEAL,
        REPAIR;

    }
}

