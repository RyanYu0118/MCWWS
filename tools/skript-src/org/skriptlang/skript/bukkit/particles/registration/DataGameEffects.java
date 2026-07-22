/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.MaterialTags
 *  org.bukkit.Axis
 *  org.bukkit.Effect
 *  org.bukkit.Material
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.particles.registration;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import com.destroystokyo.paper.MaterialTags;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Axis;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.particles.registration.DataSupplier;
import org.skriptlang.skript.bukkit.particles.registration.EffectInfo;
import org.skriptlang.skript.bukkit.particles.registration.ToString;

public class DataGameEffects {
    private static final List<EffectInfo<Effect, ?>> GAME_EFFECT_INFOS = new ArrayList();

    private static <D> void registerEffect(Effect effect, String pattern, ToString toString) {
        DataGameEffects.registerEffect(effect, pattern, (event, expressions, parseResult) -> expressions[0].getSingle(event), toString);
    }

    private static <D> void registerEffect(Effect effect, String pattern, DataSupplier<D> dataSupplier, ToString toString) {
        GAME_EFFECT_INFOS.add(new EffectInfo<Effect, D>(effect, pattern, dataSupplier, toString));
    }

    public static @Unmodifiable List<EffectInfo<Effect, ?>> getGameEffectInfos() {
        if (GAME_EFFECT_INFOS.isEmpty()) {
            DataGameEffects.registerAll();
        }
        return Collections.unmodifiableList(GAME_EFFECT_INFOS);
    }

    @Nullable
    public static String toString(Effect effect, Expression<?> @NotNull [] exprs, SkriptParser.ParseResult parseResult, Event event, boolean debug) {
        for (EffectInfo<Effect, ?> info : DataGameEffects.getGameEffectInfos()) {
            if (info.effect() != effect) continue;
            return info.toStringFunction().toString(exprs, parseResult, new SyntaxStringBuilder(event, debug)).toString();
        }
        return null;
    }

    private static void registerAll() {
        DataGameEffects.registerEffect(Effect.RECORD_PLAY, "[record] song (of|using) %itemtype%", (event, expressions, parseResult) -> {
            Material material = DataSupplier.getMaterialData(event, expressions, parseResult);
            if (material == null || !MaterialTags.MUSIC_DISCS.isTagged(material)) {
                return null;
            }
            return material;
        }, (exprs, parseResult, builder) -> builder.append("record song of", exprs[0]));
        DataGameEffects.registerEffect(Effect.SMOKE, "[dispenser] black smoke effect [(in|with|using) [the] direction] %direction%", DataSupplier::getBlockFaceData, (exprs, parseResult, builder) -> builder.append("black smoke effect in direction", exprs[0]));
        DataGameEffects.registerEffect(Effect.SHOOT_WHITE_SMOKE, "[dispenser] white smoke effect [(in|with|using) [the] direction] %direction%", DataSupplier::getCartesianBlockFaceData, (exprs, parseResult, builder) -> builder.append("white smoke effect in direction", exprs[0]));
        DataGameEffects.registerEffect(Effect.STEP_SOUND, "%itemtype/blockdata% [foot]step[s] sound [effect]", DataSupplier::getBlockData, (exprs, parseResult, builder) -> builder.append(exprs[0], "footstep sound"));
        DataGameEffects.registerEffect(Effect.POTION_BREAK, "%color% [splash] potion break effect", DataSupplier::getColorData, (exprs, parseResult, builder) -> builder.append(exprs[0], "splash potion break effect"));
        DataGameEffects.registerEffect(Effect.INSTANT_POTION_BREAK, "%color% instant [splash] potion break effect", DataSupplier::getColorData, (exprs, parseResult, builder) -> builder.append(exprs[0], "instant splash potion break effect"));
        DataGameEffects.registerEffect(Effect.COMPOSTER_FILL_ATTEMPT, "compost[er] [fill[ing]] (succe(ss|ed)|1:fail[ure|ed]) sound [effect]", (event, expressions, parseResult) -> parseResult.mark == 0, (exprs, parseResult, builder) -> builder.append((Object)(parseResult.mark == 0 ? "composter filling success sound effect" : "composter filling failure sound effect")));
        DataGameEffects.registerEffect(Effect.VILLAGER_PLANT_GROW, "villager plant grow[th] effect [(with|using) %-number% particles]", DataSupplier::getNumberDefault10, (exprs, parseResult, builder) -> builder.append((Object)"villager plant growth effect").appendIf(exprs[0] != null, "with", exprs[0], "particles"));
        DataGameEffects.registerEffect(Effect.BONE_MEAL_USE, "[fake] bone meal effect [(with|using) %-number% particles]", DataSupplier::getNumberDefault10, (exprs, parseResult, builder) -> builder.append("bone meal effect with", exprs[0], "particles"));
        DataGameEffects.registerEffect(Effect.ELECTRIC_SPARK, "(electric|lightning[ rod]|copper) spark effect [(in|using|along) the (1:x|2:y|3:z) axis]", (event, expressions, parseResult) -> parseResult.mark == 0 ? null : Axis.values()[parseResult.mark - 1], (exprs, parseResult, builder) -> builder.append((Object)"electric spark effect").appendIf(parseResult.mark != 0, "along the", Axis.values()[parseResult.mark - 1], "axis"));
        DataGameEffects.registerEffect(Effect.PARTICLES_SCULK_CHARGE, "sculk (charge|spread) effect [(with|using) data %integer%]", (exprs, parseResult, builder) -> builder.append("sculk charge effect with data", exprs[0]));
        DataGameEffects.registerEffect(Effect.PARTICLES_AND_SOUND_BRUSH_BLOCK_COMPLETE, "[finish] brush[ing] %itemtype/blockdata% effect", DataSupplier::getBlockData, (exprs, parseResult, builder) -> builder.append("brushing", exprs[0], "effect"));
        DataGameEffects.registerEffect(Effect.TRIAL_SPAWNER_DETECT_PLAYER, "trial spawner detect[ing|s] [%-number%] player[s] effect", DataSupplier::getNumberDefault1, (exprs, parseResult, builder) -> builder.append((Object)"trial spawner detecting").appendIf(exprs[0] != null, (Object)exprs[0]).append((Object)"players effect"));
        DataGameEffects.registerEffect(Effect.TRIAL_SPAWNER_DETECT_PLAYER_OMINOUS, "ominous trial spawner detect[ing|s] [%-number%] player[s] effect", DataSupplier::getNumberDefault1, (exprs, parseResult, builder) -> builder.append((Object)"ominous trial spawner detecting").appendIf(exprs[0] != null, (Object)exprs[0]).append((Object)"players effect"));
        DataGameEffects.registerEffect(Effect.TRIAL_SPAWNER_SPAWN, "[:ominous] trial spawner spawn[ing] [mob] effect", DataSupplier::isOminous, (exprs, parseResult, builder) -> builder.appendIf(parseResult.hasTag("ominous"), (Object)"ominous").append((Object)"trial spawner spawning effect"));
        DataGameEffects.registerEffect(Effect.TRIAL_SPAWNER_SPAWN_MOB_AT, "[:ominous] trial spawner spawn[ing] [mob] effect with sound", DataSupplier::isOminous, (exprs, parseResult, builder) -> builder.append((Object)(parseResult.hasTag("ominous") ? "ominous trial spawner spawning mob effect with sound" : "trial spawner spawning mob effect with sound")));
        DataGameEffects.registerEffect(Effect.BEE_GROWTH, "bee growth effect [(with|using) %-number% particles]", DataSupplier::getNumberDefault10, (exprs, parseResult, builder) -> builder.append("bee [plant] grow[th] effect with", exprs[0], "particles"));
        DataGameEffects.registerEffect(Effect.VAULT_ACTIVATE, "[:ominous] [trial] vault activate effect", DataSupplier::isOminous, (exprs, parseResult, builder) -> builder.appendIf(parseResult.hasTag("ominous"), (Object)"ominous").append((Object)"trial vault activate effect"));
        DataGameEffects.registerEffect(Effect.VAULT_DEACTIVATE, "[:ominous] [trial] vault deactivate effect", DataSupplier::isOminous, (exprs, parseResult, builder) -> builder.appendIf(parseResult.hasTag("ominous"), (Object)"ominous").append((Object)"trial vault deactivate effect"));
        DataGameEffects.registerEffect(Effect.TRIAL_SPAWNER_BECOME_OMINOUS, "trial spawner become[ing] [:not] ominous effect", (event, expressions, parseResult) -> !parseResult.hasTag("not"), (exprs, parseResult, builder) -> builder.append((Object)"trial spawner becoming").appendIf(parseResult.hasTag("not"), (Object)"not").append((Object)"ominous effect"));
        DataGameEffects.registerEffect(Effect.TRIAL_SPAWNER_SPAWN_ITEM, "[:ominous] trial spawner spawn[ing] item effect", DataSupplier::isOminous, (exprs, parseResult, builder) -> builder.appendIf(parseResult.hasTag("ominous"), (Object)"ominous").append((Object)"trial spawner spawning item effect"));
        DataGameEffects.registerEffect(Effect.TURTLE_EGG_PLACEMENT, "place turtle egg effect [(with|using) %-number% particles]", DataSupplier::getNumberDefault10, (exprs, parseResult, builder) -> builder.append("place turtle egg effect with", exprs[0], "particles"));
        DataGameEffects.registerEffect(Effect.SMASH_ATTACK, "[mace] smash attack effect [(with|using) %-number% particles]", DataSupplier::getNumberDefault10, (exprs, parseResult, builder) -> builder.append("smash attack effect with", exprs[0], "particles"));
    }
}

