/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.util;

import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class SimpleEvent
extends SkriptEvent {
    @Override
    public boolean check(Event event) {
        return true;
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        if (args.length != 0) {
            throw new SkriptAPIException("Invalid use of SimpleEvent");
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "simple event";
    }
}

