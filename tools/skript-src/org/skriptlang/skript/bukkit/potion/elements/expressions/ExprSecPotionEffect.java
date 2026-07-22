/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.expressions;

import ch.njol.skript.classes.Parser;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.util.PotionUtils;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="New Potion Effect")
@Description(value={"Create a new potion effect that can be applied to an entity or item type."})
@Example.Examples(value={@Example(value="set {_potion} to a potion effect of speed 2 for 10 minutes:\n\thide the effect's icon\n\thide the effect's particles\n"), @Example(value="add strength 5 to the potion effects of the player's tool"), @Example(value="apply invisibility to the player for 5 minutes:\n\thide the effect's particles\n"), @Example(value="add a potion effect of speed 1 to the potion effects of the player"), @Example(value="# creates a potion effect with the properties of an existing potion effect\nset {_potion} to a potion effect of slowness based on the player's speed effect\n")})
@Since(value={"2.5.2", "2.14 (syntax changes, infinite duration support)"})
public class ExprSecPotionEffect
extends SectionExpression<SkriptPotionEffect> {
    @Nullable
    private Expression<PotionEffectType> potionEffectType;
    @Nullable
    private Expression<Number> amplifier;
    @Nullable
    private Expression<Timespan> duration;
    private boolean ambient;
    private boolean infinite;
    @Nullable
    private Expression<PotionEffect> source;
    @Nullable
    private Trigger trigger;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprSecPotionEffect.class, SkriptPotionEffect.class).supplier(ExprSecPotionEffect::new)).addPatterns("[a[n]] [:ambient] potion effect of %potioneffecttype% [[of tier] %-number%] [for %-timespan%]", "[an] (infinite|permanent) [:ambient] potion effect of %potioneffecttype% [[of tier] %-number%] ", "[an] (infinite|permanent) [:ambient] %potioneffecttype% [[of tier] %-number%] [potion [effect]] ", "[a] potion effect [of %-potioneffecttype%] (from|using|based on) %potioneffect%")).build());
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprSecPotionEffect.class, SkriptPotionEffect.class).supplier(ExprSecPotionEffect::new)).priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)).addPatterns("%*potioneffecttype% <-?\\d+(_\\d+)*>")).build());
        EventValues.registerEventValue(PotionEffectSectionEvent.class, SkriptPotionEffect.class, event -> event.effect);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean delayed, SkriptParser.ParseResult parseResult, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        this.potionEffectType = expressions[0];
        if (matchedPattern == 3) {
            this.source = expressions[1];
        } else if (!parseResult.regexes.isEmpty()) {
            Parser<Number> numberParser = Classes.getParser(Number.class);
            if (numberParser == null) {
                return false;
            }
            Number number = numberParser.parse(parseResult.regexes.getFirst().group(), null);
            if (number == null) {
                return false;
            }
            this.amplifier = new SimpleLiteral<Number>(number, false);
            this.infinite = false;
            this.ambient = false;
        } else {
            this.amplifier = expressions[1];
            boolean bl = this.infinite = matchedPattern != 0;
            if (expressions.length == 3) {
                this.duration = expressions[2];
            }
            this.ambient = parseResult.hasTag("ambient");
        }
        if (sectionNode != null) {
            this.trigger = SectionUtils.loadLinkedCode("create potion effect", (beforeLoading, afterLoading) -> this.loadCode(sectionNode, "create potion effect", (Runnable)beforeLoading, (Runnable)afterLoading, (Class<? extends Event>)PotionEffectSectionEvent.class));
            return this.trigger != null;
        }
        return true;
    }

    protected SkriptPotionEffect[] get(Event event) {
        Number amplifierNumber;
        SkriptPotionEffect potionEffect = null;
        if (this.source != null) {
            PotionEffect source = this.source.getSingle(event);
            if (source == null) {
                return new SkriptPotionEffect[0];
            }
            potionEffect = SkriptPotionEffect.fromBukkitEffect(source);
        }
        PotionEffectType potionEffectType = null;
        if (this.potionEffectType != null && (potionEffectType = this.potionEffectType.getSingle(event)) == null) {
            return new SkriptPotionEffect[0];
        }
        if (potionEffect == null) {
            potionEffect = SkriptPotionEffect.fromType(potionEffectType);
        } else {
            potionEffect.potionEffectType(potionEffectType);
        }
        if (this.ambient) {
            potionEffect.ambient(true);
        }
        if (this.amplifier != null && (amplifierNumber = this.amplifier.getSingle(event)) != null) {
            potionEffect.amplifier(amplifierNumber.intValue() - 1);
        }
        if (this.duration != null) {
            Timespan timespan = this.duration.getSingle(event);
            if (timespan != null) {
                if (timespan.isInfinite()) {
                    potionEffect.infinite(true);
                } else {
                    potionEffect.duration((int)Math2.fit(0L, timespan.getAs(Timespan.TimePeriod.TICK), Integer.MAX_VALUE));
                }
            }
        } else if (this.infinite) {
            potionEffect.infinite(true);
        }
        if (this.trigger != null) {
            PotionEffectSectionEvent potionEvent = new PotionEffectSectionEvent();
            potionEvent.effect = potionEffect;
            Variables.withLocalVariables(event, potionEvent, () -> TriggerItem.walk(this.trigger, potionEvent));
        }
        return new SkriptPotionEffect[]{potionEffect};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends SkriptPotionEffect> getReturnType() {
        return SkriptPotionEffect.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.ambient) {
            builder.append((Object)"ambient");
        }
        if (this.infinite) {
            builder.append((Object)"infinite");
        }
        builder.append((Object)"potion effect");
        if (this.potionEffectType != null) {
            builder.append("of", this.potionEffectType);
        }
        if (this.amplifier != null) {
            builder.append("of tier", this.amplifier);
        }
        if (this.source != null) {
            builder.append("based on", this.source);
        } else if (!this.infinite) {
            builder.append((Object)"for");
            if (this.duration == null) {
                builder.append((Object)PotionUtils.DEFAULT_DURATION_STRING);
            } else {
                builder.append((Object)this.duration);
            }
        }
        return builder.toString();
    }

    static class PotionEffectSectionEvent
    extends Event {
        public SkriptPotionEffect effect;

        PotionEffectSectionEvent() {
        }

        @NotNull
        public HandlerList getHandlers() {
            throw new UnsupportedOperationException();
        }
    }
}

