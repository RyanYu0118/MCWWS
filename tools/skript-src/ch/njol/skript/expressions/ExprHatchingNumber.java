/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerEggThrowEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Hatching Number")
@Description(value={"The number of entities that will be hatched in a Player Egg Throw event.", "Please note that no more than 127 entities can be hatched at once."})
@Example(value="on player egg throw:\n\tset the hatching number to 10\n")
@Events(value={"Egg Throw"})
@Since(value={"2.7"})
public class ExprHatchingNumber
extends SimpleExpression<Byte>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerEggThrowEvent.class);
    }

    @Nullable
    protected Byte[] get(Event event) {
        if (!(event instanceof PlayerEggThrowEvent)) {
            return new Byte[0];
        }
        return new Byte[]{((PlayerEggThrowEvent)event).getNumHatches()};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) {
            return CollectionUtils.array(Number.class);
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(event instanceof PlayerEggThrowEvent) || delta == null) {
            return;
        }
        assert (delta[0] != null);
        int value = ((Number)delta[0]).intValue();
        if (mode != Changer.ChangeMode.SET) {
            if (mode == Changer.ChangeMode.REMOVE) {
                value *= -1;
            }
            value = ((PlayerEggThrowEvent)event).getNumHatches() + value;
        }
        ((PlayerEggThrowEvent)event).setNumHatches((byte)Math.min(Math.max(0, value), 127));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Byte> getReturnType() {
        return Byte.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the hatching number";
    }

    static {
        Skript.registerExpression(ExprHatchingNumber.class, Byte.class, ExpressionType.SIMPLE, "[the] hatching number");
    }
}

