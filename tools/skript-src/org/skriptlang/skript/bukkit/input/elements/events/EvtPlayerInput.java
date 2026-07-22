/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerInputEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.input.elements.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.EventValues;
import java.util.Set;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInputEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.input.InputKey;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EvtPlayerInput
extends SkriptEvent {
    @Nullable
    private Literal<InputKey> keysToCheck;
    private InputType type;

    public static void register(SyntaxRegistry registry) {
        if (!Skript.classExists("org.bukkit.event.player.PlayerInputEvent")) {
            return;
        }
        registry.register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(EvtPlayerInput.class, "Player Input").addEvent(PlayerInputEvent.class).addPatterns("[player] (toggle|toggling|1:press[ing]|2:release|2:releasing) of (%-inputkeys%|(an|any) input key)", "([player] %-inputkeys%|[an|player] input key) (toggle|toggling|1:press[ing]|2:release|2:releasing)")).addDescription("Called when a player sends an updated input to the server.", "Note: The input keys event value is the set of keys the player is currently pressing, not the keys that were pressed or released.").addExample("on input key press:\n\tsend \"You are pressing: %event-inputkeys%\" to player\n").addSince("2.10").addRequiredPlugins("Minecraft 1.21.3+").supplier(EvtPlayerInput::new)).build());
        EventValues.registerEventValue(PlayerInputEvent.class, InputKey[].class, event -> InputKey.fromInput(event.getInput()).toArray(new InputKey[0]));
        EventValues.registerEventValue(PlayerInputEvent.class, InputKey[].class, event -> InputKey.fromInput(event.getPlayer().getCurrentInput()).toArray(new InputKey[0]), EventValues.TIME_PAST);
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.keysToCheck = args[0];
        this.type = InputType.values()[parseResult.mark];
        return true;
    }

    @Override
    public boolean check(Event event) {
        PlayerInputEvent inputEvent = (PlayerInputEvent)event;
        Set<InputKey> previousKeys = InputKey.fromInput(inputEvent.getPlayer().getCurrentInput());
        Set<InputKey> currentKeys = InputKey.fromInput(inputEvent.getInput());
        Set<InputKey> keysToCheck = this.keysToCheck != null ? Set.of(this.keysToCheck.getAll()) : null;
        boolean and = this.keysToCheck != null && this.keysToCheck.getAnd();
        return this.type.checkInputKeys(previousKeys, currentKeys, keysToCheck, and);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"player");
        builder.append((Object)this.type.name().toLowerCase());
        builder.append(this.keysToCheck == null ? "any input key" : this.keysToCheck);
        return builder.toString();
    }

    private static enum InputType {
        TOGGLE{

            @Override
            public boolean checkKeyState(boolean inPrevious, boolean inCurrent) {
                return inPrevious != inCurrent;
            }
        }
        ,
        PRESS{

            @Override
            public boolean checkKeyState(boolean inPrevious, boolean inCurrent) {
                return !inPrevious && inCurrent;
            }
        }
        ,
        RELEASE{

            @Override
            public boolean checkKeyState(boolean inPrevious, boolean inCurrent) {
                return inPrevious && !inCurrent;
            }
        };


        public abstract boolean checkKeyState(boolean var1, boolean var2);

        public boolean checkInputKeys(Set<InputKey> previous, Set<InputKey> current, @Nullable Set<InputKey> keysToCheck, boolean and) {
            if (keysToCheck == null) {
                return switch (this.ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 0 -> true;
                    case 1 -> {
                        if (previous.size() <= current.size()) {
                            yield true;
                        }
                        yield false;
                    }
                    case 2 -> previous.size() >= current.size();
                };
            }
            for (InputKey key : keysToCheck) {
                boolean inPrevious = previous.contains((Object)key);
                boolean inCurrent = current.contains((Object)key);
                if (and && !this.checkKeyState(inPrevious, inCurrent)) {
                    return false;
                }
                if (and || !this.checkKeyState(inPrevious, inCurrent)) continue;
                return true;
            }
            return and;
        }
    }
}

