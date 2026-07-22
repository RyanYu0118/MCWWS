/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.SoundUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Sound")
@Description(value={"Gets the sound that a given entity will make in a specific scenario."})
@Example.Examples(value={@Example(value="play sound (hurt sound of player) at player"), @Example(value="set {_sounds::*} to death sounds of (all mobs in radius 10 of player)")})
@Since(value={"2.10"})
@RequiredPlugins(value={"Spigot 1.19.2+"})
public class ExprEntitySound
extends SimpleExpression<String> {
    private static final Patterns<SoundType> patterns = new Patterns(new Object[][]{{"[the] (damage|hurt) sound[s] of %livingentities%", SoundType.DAMAGE}, {"%livingentities%'[s] (damage|hurt) sound[s]", SoundType.DAMAGE}, {"[the] death sound[s] of %livingentities%", SoundType.DEATH}, {"%livingentities%'[s] death sound[s]", SoundType.DEATH}, {"[the] [high:(tall|high)|(low|normal)] fall damage sound[s] [from [[a] height [of]] %-number%] of %livingentities%", SoundType.FALL}, {"%livingentities%'[s] [high:(tall|high)|low:(low|normal)] fall [damage] sound[s] [from [[a] height [of]] %-number%]", SoundType.FALL}, {"[the] swim[ming] sound[s] of %livingentities%", SoundType.SWIM}, {"%livingentities%'[s] swim[ming] sound[s]", SoundType.SWIM}, {"[the] [fast:(fast|speedy)] splash sound[s] of %livingentities%", SoundType.SPLASH}, {"%livingentities%'[s] [fast:(fast|speedy)] splash sound[s]", SoundType.SPLASH}, {"[the] eat[ing] sound[s] of %livingentities% [(with|using|[while] eating [a]) %-itemtype%]", SoundType.EAT}, {"%livingentities%'[s] eat[ing] sound[s]", SoundType.EAT}, {"[the] drink[ing] sound[s] of %livingentities% [(with|using|[while] drinking [a]) %-itemtype%]", SoundType.DRINK}, {"%livingentities%'[s] drink[ing] sound[s]", SoundType.DRINK}, {"[the] ambient sound[s] of %livingentities%", SoundType.AMBIENT}, {"%livingentities%'[s] ambient sound[s]", SoundType.AMBIENT}});
    private boolean bigOrSpeedy;
    private SoundType soundType;
    private Expression<Number> height;
    private Expression<LivingEntity> entities;
    private Expression<ItemType> item;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.soundType = patterns.getInfo(matchedPattern);
        boolean bl = this.bigOrSpeedy = parseResult.hasTag("high") || parseResult.hasTag("fast");
        if (this.soundType == SoundType.FALL) {
            this.height = exprs[0];
        }
        if (this.soundType == SoundType.EAT || this.soundType == SoundType.DRINK) {
            this.item = exprs[1];
        }
        this.entities = this.soundType == SoundType.FALL ? exprs[1] : exprs[0];
        return true;
    }

    protected String @Nullable [] get(Event event) {
        int height = this.height == null ? -1 : this.height.getOptionalSingle(event).orElse(-1).intValue();
        ItemStack defaultItem = new ItemStack(this.soundType == SoundType.EAT ? Material.COOKED_BEEF : Material.POTION);
        ItemStack item = this.item == null ? defaultItem : this.item.getOptionalSingle(event).map(ItemType::getRandom).orElse(defaultItem);
        return (String[])this.entities.stream(event).map(entity -> this.soundType.getSound((LivingEntity)entity, height, item, this.bigOrSpeedy)).filter(Objects::nonNull).distinct().map(sound -> SoundUtils.getKey(sound).getKey()).toArray(String[]::new);
    }

    @Override
    public boolean isSingle() {
        return this.entities.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Object sound = "unknown";
        switch (this.soundType.ordinal()) {
            case 0: 
            case 1: 
            case 3: 
            case 7: {
                sound = this.soundType.name().toLowerCase();
                break;
            }
            case 2: {
                if (this.height == null) {
                    sound = this.bigOrSpeedy ? "high fall damage" : "normal fall damage";
                    break;
                }
                sound = "fall damage from a height of " + this.height.toString(event, debug);
                break;
            }
            case 4: {
                sound = this.bigOrSpeedy ? "speedy splash" : "splash";
                break;
            }
            case 5: 
            case 6: {
                String action = this.soundType == SoundType.EAT ? "eating" : "drinking";
                sound = this.item == null ? action : action + " " + this.item.toString(event, debug);
            }
        }
        return (String)sound + " sound of " + this.entities.toString(event, debug);
    }

    static {
        if (Skript.methodExists(LivingEntity.class, "getDeathSound", new Class[0])) {
            Skript.registerExpression(ExprEntitySound.class, String.class, ExpressionType.COMBINED, patterns.getPatterns());
        }
    }

    public static enum SoundType {
        DAMAGE{

            @Override
            @Nullable
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                return entity.getHurtSound();
            }
        }
        ,
        DEATH{

            @Override
            @Nullable
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                return entity.getDeathSound();
            }
        }
        ,
        FALL{

            @Override
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                if (height != -1) {
                    return entity.getFallDamageSound(height);
                }
                return bigOrSpeedy ? entity.getFallDamageSoundBig() : entity.getFallDamageSoundSmall();
            }
        }
        ,
        SWIM{

            @Override
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                return entity.getSwimSound();
            }
        }
        ,
        SPLASH{

            @Override
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                return bigOrSpeedy ? entity.getSwimHighSpeedSplashSound() : entity.getSwimSplashSound();
            }
        }
        ,
        EAT{

            @Override
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                return entity.getEatingSound(item);
            }
        }
        ,
        DRINK{

            @Override
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                return entity.getDrinkingSound(item);
            }
        }
        ,
        AMBIENT{

            @Override
            @Nullable
            public Sound getSound(LivingEntity entity, int height, ItemStack item, boolean bigOrSpeedy) {
                Sound sound;
                if (entity instanceof Mob) {
                    Mob mob = (Mob)entity;
                    sound = mob.getAmbientSound();
                } else {
                    sound = null;
                }
                return sound;
            }
        };


        @Nullable
        public abstract Sound getSound(LivingEntity var1, int var2, ItemStack var3, boolean var4);
    }
}

