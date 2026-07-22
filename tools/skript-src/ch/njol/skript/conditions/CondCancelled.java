/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Event Cancelled")
@Description(value={"Checks whether or not the event is cancelled."})
@Example(value="on click:\n\tif event is cancelled:\n\t\tbroadcast \"no clicks allowed!\"\n")
@Since(value={"2.2-dev36"})
public class CondCancelled
extends Condition {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return (e instanceof Cancellable && ((Cancellable)e).isCancelled()) ^ this.isNegated();
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.isNegated() ? "event is not cancelled" : "event is cancelled";
    }

    static {
        Skript.registerCondition(CondCancelled.class, "[the] event is cancel[l]ed", "[the] event (is not|isn't) cancel[l]ed");
    }
}

