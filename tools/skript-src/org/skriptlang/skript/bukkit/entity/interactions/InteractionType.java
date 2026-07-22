/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Interaction
 *  org.bukkit.entity.Interaction$PreviousInteraction
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.interactions;

import org.bukkit.entity.Interaction;
import org.jetbrains.annotations.Nullable;

public enum InteractionType {
    ATTACK,
    INTERACT,
    BOTH;


    @Nullable
    public static Interaction.PreviousInteraction getLatest(Interaction interaction) {
        Interaction.PreviousInteraction attack = interaction.getLastAttack();
        Interaction.PreviousInteraction interact = interaction.getLastInteraction();
        if (attack == null) {
            return interact;
        }
        if (interact == null) {
            return attack;
        }
        if (attack.getTimestamp() > interact.getTimestamp()) {
            return attack;
        }
        return interact;
    }
}

