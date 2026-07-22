/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 */
package org.skriptlang.skript.bukkit.particles.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.function.BiFunction;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.skriptlang.skript.bukkit.particles.particleeffects.DirectionalEffect;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Particle with Offset/Distribution/Velocity")
@Description(value={"Applies a specific offset to a particle.\nOffsets are treated as distributions if particle count is greater than 0.\nOffsets are treated as velocity or some other special behavior if particle count is 0.\nSetting distribution/velocity with this method may change the particle count to 1/0 respectively.\n\nMore detailed information on particle behavior can be found at <a href=\"https://docs.papermc.io/paper/dev/particles/#count-argument-behavior\">Paper's particle documentation</a>.\n"})
@Example.Examples(value={@Example(value="draw an electric spark particle with a velocity of vector(1,2,3) at player"), @Example(value="draw 12 red dust particles with a distribution of vector(1,2,1) at player's head location")})
@Since(value={"2.14"})
public class ExprParticleWithOffset
extends PropertyExpression<ParticleEffect, ParticleEffect> {
    private static final Patterns<Mode> patterns = new Patterns(new Object[][]{{"%particles% with [an] offset [of] %vector%", Mode.OFFSET}, {"%particles% with [a] distribution [of] %vector%", Mode.DISTRIBUTION}, {"%directionalparticles% with [a] velocity [of] %vector%", Mode.VELOCITY}});
    private Mode mode;
    private Expression<Vector> offset;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprParticleWithOffset.class, ParticleEffect.class).addPatterns(patterns.getPatterns())).supplier(ExprParticleWithOffset::new)).priority(SyntaxInfo.COMBINED)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.mode = patterns.getInfo(matchedPattern);
        this.setExpr(expressions[0]);
        this.offset = expressions[1];
        return true;
    }

    protected ParticleEffect[] get(Event event, ParticleEffect[] source) {
        Vector offset = this.offset.getSingle(event);
        if (offset == null) {
            return new ParticleEffect[0];
        }
        Vector3d offsetJoml = offset.toVector3d();
        ParticleEffect[] results = (ParticleEffect[])Array.newInstance(this.getExpr().getReturnType(), source.length);
        for (int i = 0; i < source.length; ++i) {
            results[i] = this.mode.apply(source[i].copy(), offsetJoml);
        }
        return results;
    }

    @Override
    public Class<? extends ParticleEffect> getReturnType() {
        return this.getExpr().getReturnType();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder ssb = new SyntaxStringBuilder(event, debug);
        ssb.append(this.getExpr(), "with");
        switch (this.mode.ordinal()) {
            case 0: {
                ssb.append((Object)"an offset");
                break;
            }
            case 1: {
                ssb.append((Object)"a distribution");
                break;
            }
            case 2: {
                ssb.append((Object)"a velocity");
            }
        }
        ssb.append("of", this.offset);
        return ssb.toString();
    }

    static enum Mode {
        OFFSET(ParticleEffect::offset),
        DISTRIBUTION(ParticleEffect::distribution),
        VELOCITY((particle, offset) -> ((DirectionalEffect)particle).velocity((Vector3d)offset));

        private final BiFunction<ParticleEffect, Vector3d, ParticleEffect> apply;

        private Mode(BiFunction<ParticleEffect, Vector3d, ParticleEffect> apply) {
            this.apply = apply;
        }

        public ParticleEffect apply(ParticleEffect particle, Vector3d offset) {
            return this.apply.apply(particle, offset);
        }
    }
}

