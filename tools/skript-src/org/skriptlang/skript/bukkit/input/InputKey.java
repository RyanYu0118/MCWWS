/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Input
 */
package org.skriptlang.skript.bukkit.input;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Input;

public enum InputKey {
    FORWARD,
    BACKWARD,
    RIGHT,
    LEFT,
    JUMP,
    SNEAK,
    SPRINT;


    public boolean check(Input input) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> input.isForward();
            case 1 -> input.isBackward();
            case 2 -> input.isRight();
            case 3 -> input.isLeft();
            case 4 -> input.isJump();
            case 5 -> input.isSneak();
            case 6 -> input.isSprint();
        };
    }

    public static Set<InputKey> fromInput(Input input) {
        EnumSet<InputKey> keys = EnumSet.noneOf(InputKey.class);
        for (InputKey key : InputKey.values()) {
            if (!key.check(input)) continue;
            keys.add(key);
        }
        return keys;
    }
}

