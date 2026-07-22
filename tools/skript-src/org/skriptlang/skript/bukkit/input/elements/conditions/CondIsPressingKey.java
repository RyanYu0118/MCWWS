/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Input
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerInputEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.input.elements.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInputEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.input.InputKey;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Is Pressing Key")
@Description(value={"Checks if a player is pressing a certain input key."})
@Example(value="on player input:\n\tif player is pressing forward movement key:\n\t\tsend \"You are moving forward!\"\n")
@Since(value={"2.10"})
@Keywords(value={"press", "input"})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
public class CondIsPressingKey
extends Condition {
    private Expression<Player> players;
    private Expression<InputKey> inputKeys;
    private boolean past;
    private boolean delayed;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(CondIsPressingKey.class).addPatterns("%players% (is|are) pressing %inputkeys%", "%players% (isn't|is not|aren't|are not) pressing %inputkeys%", "%players% (was|were) pressing %inputkeys%", "%players% (wasn't|was not|weren't|were not) pressing %inputkeys%").supplier(CondIsPressingKey::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = expressions[0];
        this.inputKeys = expressions[1];
        this.past = matchedPattern > 1;
        boolean bl = this.delayed = !isDelayed.isFalse();
        if (this.past) {
            if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerInputEvent.class)) {
                Skript.warning("Checking the past state of a player's input outside the 'player input' event has no effect.");
            } else if (this.delayed) {
                Skript.warning("Checking the past state of a player's input after the event has passed has no effect.");
            }
        }
        this.setNegated(matchedPattern == 1 || matchedPattern == 3);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Player player2;
        if (event instanceof PlayerInputEvent) {
            PlayerInputEvent inputEvent = (PlayerInputEvent)event;
            player2 = inputEvent.getPlayer();
        } else {
            player2 = null;
        }
        Player eventPlayer = player2;
        InputKey[] inputKeys = this.inputKeys.getAll(event);
        boolean and = this.inputKeys.getAnd();
        boolean delayed = this.delayed || Delay.isDelayed(event);
        return this.players.check(event, player -> {
            Input input = !delayed && !this.past && player.equals((Object)eventPlayer) ? ((PlayerInputEvent)event).getInput() : player.getCurrentInput();
            return CollectionUtils.check(inputKeys, inputKey -> inputKey.check(input), and);
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)this.players);
        if (this.past) {
            builder.append((Object)(this.players.isSingle() ? "was" : "were"));
        } else {
            builder.append((Object)(this.players.isSingle() ? "is" : "are"));
        }
        if (this.isNegated()) {
            builder.append((Object)"not");
        }
        builder.append((Object)"pressing");
        builder.append((Object)this.inputKeys);
        return builder.toString();
    }
}

