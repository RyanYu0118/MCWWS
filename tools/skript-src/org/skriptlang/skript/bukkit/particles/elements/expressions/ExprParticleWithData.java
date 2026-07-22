/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.particles.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import org.bukkit.Particle;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;
import org.skriptlang.skript.bukkit.particles.registration.DataParticles;
import org.skriptlang.skript.bukkit.particles.registration.EffectInfo;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Particles with Data")
@Description(value={"Creates particles that require some extra information, such as colors, locations, or block data.\nParticles not present here do not require data and can be found in the Particle type.\nData requirements vary from version to version, so these docs are only accurate for the most recent Minecraft version at time of release.\nFor example, between 1.21.8 and 1.21.9, the 'flash' particle became colourable and now requires a colour data.\n"})
@Example.Examples(value={@Example(value="set {blood-effect} to a red dust particle of size 1"), @Example(value="draw 3 blue trail particles moving to player's target over 3 seconds at player")})
@Since(value={"2.14"})
public class ExprParticleWithData
extends SimpleExpression<ParticleEffect> {
    private static Patterns<EffectInfo<Particle, Object>> PATTERNS;
    private SkriptParser.ParseResult parseResult;
    private Expression<?>[] expressions;
    private EffectInfo<Particle, Object> effectInfo;
    private Expression<Number> count;

    public static void register(SyntaxRegistry registry) {
        Object[][] patterns = new Object[DataParticles.getParticleInfos().size()][2];
        int i = 0;
        for (EffectInfo<Particle, ?> particleInfo : DataParticles.getParticleInfos()) {
            patterns[i][0] = "[%-*number%|a[n]] " + particleInfo.pattern();
            patterns[i][1] = particleInfo;
            ++i;
        }
        PATTERNS = new Patterns(patterns);
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprParticleWithData.class, ParticleEffect.class).addPatterns(PATTERNS.getPatterns())).supplier(ExprParticleWithData::new)).priority(SyntaxInfo.COMBINED)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.parseResult = parseResult;
        this.expressions = Arrays.copyOfRange(expressions, 1, expressions.length);
        this.count = expressions[0];
        this.effectInfo = PATTERNS.getInfo(matchedPattern);
        return this.effectInfo != null;
    }

    protected ParticleEffect @Nullable [] get(Event event) {
        Object data = this.effectInfo.dataSupplier().getData(event, this.expressions, this.parseResult);
        if (data == null) {
            this.error("Could not obtain required data for " + ParticleEffect.toString(this.effectInfo.effect(), 0));
            return null;
        }
        ParticleEffect effect = ParticleEffect.of(this.effectInfo.effect());
        effect.data(data);
        if (this.count != null) {
            Number count = this.count.getSingle(event);
            if (count != null) {
                effect.count(Math.clamp((long)count.intValue(), 0, 16384));
            } else {
                this.warning("The 'count' value for the '" + Classes.toString(effect) + "' particle was either not set or not a number (" + this.count.toString(event, false) + "); defaulting to 1.");
            }
        }
        return new ParticleEffect[]{effect};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends ParticleEffect> getReturnType() {
        return ParticleEffect.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder ssb = new SyntaxStringBuilder(event, debug);
        if (this.count != null) {
            ssb.append((Object)this.count);
        }
        return this.effectInfo.toStringFunction().toString(this.expressions, this.parseResult, ssb).toString();
    }
}

