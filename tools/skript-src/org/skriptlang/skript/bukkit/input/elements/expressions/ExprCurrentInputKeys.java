/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerInputEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.input.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInputEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.input.InputKey;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Player Input Keys")
@Description(value={"Get the current input keys of a player."})
@Example(value="broadcast \"%player% is pressing %current input keys of player%\"")
@Since(value={"2.10"})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
public class ExprCurrentInputKeys
extends PropertyExpression<Player, InputKey> {
    private static final boolean SUPPORTS_TIME_STATES = Skript.classExists("org.bukkit.event.player.PlayerInputEvent");
    private boolean delayed;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprCurrentInputKeys.infoBuilder(ExprCurrentInputKeys.class, InputKey.class, "[current] (inputs|input keys)", "players", false).supplier(ExprCurrentInputKeys::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(expressions[0]);
        this.delayed = !isDelayed.isFalse();
        return true;
    }

    protected InputKey[] get(Event event, Player[] source) {
        Player eventPlayer = null;
        if (SUPPORTS_TIME_STATES && this.getTime() == EventValues.TIME_NOW && event instanceof PlayerInputEvent) {
            PlayerInputEvent inputEvent = (PlayerInputEvent)event;
            eventPlayer = inputEvent.getPlayer();
        }
        boolean delayed = this.delayed || Delay.isDelayed(event);
        ArrayList<InputKey> inputKeys = new ArrayList<InputKey>();
        for (Player player : source) {
            if (!delayed && player.equals((Object)eventPlayer)) {
                inputKeys.addAll(InputKey.fromInput(((PlayerInputEvent)event).getInput()));
                continue;
            }
            inputKeys.addAll(InputKey.fromInput(player.getCurrentInput()));
        }
        return inputKeys.toArray(new InputKey[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends InputKey> getReturnType() {
        return InputKey.class;
    }

    @Override
    public boolean setTime(int time) {
        if (!SUPPORTS_TIME_STATES) {
            return super.setTime(time);
        }
        return time != EventValues.TIME_FUTURE && this.setTime(time, (Class<? extends Event>)PlayerInputEvent.class);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the current input keys of " + this.getExpr().toString(event, debug);
    }
}

