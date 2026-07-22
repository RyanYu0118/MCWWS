/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Effect
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
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import org.bukkit.Effect;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.particles.GameEffect;
import org.skriptlang.skript.bukkit.particles.registration.DataGameEffects;
import org.skriptlang.skript.bukkit.particles.registration.EffectInfo;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Game Effects with Data")
@Description(value={"Creates game effects that require some extra information, such as colors, particle counts, or block data.\nGame effects consist of combinations particles and/or sounds that are used in Minecraft, such as the bone meal particles, the sound of footsteps on a specific block, or the particles and sound of breaking a splash potion.\nGame effects not present here do not require data and can be found in the Game Effect type.\nData requirements vary from version to version, so these docs are only accurate for the most recent Minecraft version at time of release.\n"})
@Example(value="play compost success sound effect to player")
@Since(value={"2.14"})
public class ExprGameEffectWithData
extends SimpleExpression<GameEffect> {
    private static Patterns<EffectInfo<Effect, Object>> PATTERNS;
    private EffectInfo<Effect, Object> gameEffectInfo;
    private Expression<?>[] expressions;
    private SkriptParser.ParseResult parseResult;

    public static void register(SyntaxRegistry registry) {
        Object[][] patterns = new Object[DataGameEffects.getGameEffectInfos().size()][2];
        int i = 0;
        for (EffectInfo<Effect, ?> gameEffectInfo : DataGameEffects.getGameEffectInfos()) {
            patterns[i][0] = gameEffectInfo.pattern();
            patterns[i][1] = gameEffectInfo;
            ++i;
        }
        PATTERNS = new Patterns(patterns);
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprGameEffectWithData.class, GameEffect.class).addPatterns(PATTERNS.getPatterns())).supplier(ExprGameEffectWithData::new)).priority(SyntaxInfo.COMBINED)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.gameEffectInfo = PATTERNS.getInfo(matchedPattern);
        this.expressions = expressions;
        this.parseResult = parseResult;
        return true;
    }

    protected GameEffect @Nullable [] get(Event event) {
        GameEffect gameEffect = new GameEffect(this.gameEffectInfo.effect());
        Object data = this.gameEffectInfo.dataSupplier().getData(event, this.expressions, this.parseResult);
        if (data == null && gameEffect.getEffect() != Effect.ELECTRIC_SPARK) {
            this.error("Could not obtain required data for " + String.valueOf(gameEffect));
            return new GameEffect[0];
        }
        boolean success = gameEffect.setData(data);
        if (!success) {
            this.error("Could not obtain required data for " + String.valueOf(gameEffect));
            return new GameEffect[0];
        }
        return new GameEffect[]{gameEffect};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends GameEffect> getReturnType() {
        return GameEffect.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.gameEffectInfo.toStringFunction().toString(this.expressions, this.parseResult, new SyntaxStringBuilder(event, debug)).toString();
    }
}

