/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.potion.SuspiciousEffectEntry
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.PotionMeta
 *  org.bukkit.inventory.meta.SuspiciousStewMeta
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package org.skriptlang.skript.bukkit.potion.util;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.Timespan;
import io.papermc.paper.potion.SuspiciousEffectEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PotionUtils {
    public static final int DEFAULT_DURATION_TICKS = 600;
    public static final String DEFAULT_DURATION_STRING = new Timespan(Timespan.TimePeriod.TICK, 600L).toString();

    public static List<PotionEffect> getPotionEffects(ItemType itemType) {
        ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
        ItemMeta meta = itemType.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta)meta;
            if (potionMeta.hasCustomEffects()) {
                effects.addAll(potionMeta.getCustomEffects());
            }
            if (potionMeta.hasBasePotionType()) {
                effects.addAll(potionMeta.getBasePotionType().getPotionEffects());
            }
        } else if (meta instanceof SuspiciousStewMeta) {
            SuspiciousStewMeta stewMeta = (SuspiciousStewMeta)meta;
            effects.addAll(stewMeta.getCustomEffects());
        }
        return effects;
    }

    public static void addPotionEffects(ItemType itemType, PotionEffect ... potionEffects) {
        ItemMeta meta = itemType.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta)meta;
            for (PotionEffect potionEffect : potionEffects) {
                potionMeta.addCustomEffect(potionEffect, true);
            }
        } else if (meta instanceof SuspiciousStewMeta) {
            SuspiciousStewMeta stewMeta = (SuspiciousStewMeta)meta;
            for (PotionEffect potionEffect : potionEffects) {
                stewMeta.addCustomEffect(SuspiciousEffectEntry.create((PotionEffectType)potionEffect.getType(), (int)potionEffect.getDuration()), true);
            }
        }
        itemType.setItemMeta(meta);
    }

    public static void removePotionEffects(ItemType itemType, PotionEffectType ... potionEffectTypes) {
        ItemMeta meta = itemType.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta)meta;
            for (PotionEffectType potionEffectType : potionEffectTypes) {
                potionMeta.removeCustomEffect(potionEffectType);
            }
        } else if (meta instanceof SuspiciousStewMeta) {
            SuspiciousStewMeta stewMeta = (SuspiciousStewMeta)meta;
            for (PotionEffectType potionEffectType : potionEffectTypes) {
                stewMeta.removeCustomEffect(potionEffectType);
            }
        }
        itemType.setItemMeta(meta);
    }

    public static void clearPotionEffects(ItemType itemType) {
        ItemMeta meta = itemType.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta)meta;
            potionMeta.clearCustomEffects();
        } else if (meta instanceof SuspiciousStewMeta) {
            SuspiciousStewMeta stewMeta = (SuspiciousStewMeta)meta;
            stewMeta.clearCustomEffects();
        }
        itemType.setItemMeta(meta);
    }

    public static Deque<PotionEffect> getHiddenEffects(PotionEffect effect) {
        ArrayDeque<PotionEffect> hiddenEffects = new ArrayDeque<PotionEffect>();
        for (PotionEffect hiddenEffect = effect.getHiddenPotionEffect(); hiddenEffect != null; hiddenEffect = hiddenEffect.getHiddenPotionEffect()) {
            hiddenEffects.push(hiddenEffect);
        }
        return hiddenEffects;
    }
}

