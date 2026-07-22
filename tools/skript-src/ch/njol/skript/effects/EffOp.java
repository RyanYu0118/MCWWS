/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="op/deop")
@Description(value={"Grant/revoke a user operator state."})
@Example.Examples(value={@Example(value="op the player"), @Example(value="deop all players")})
@Since(value={"1.0"})
public class EffOp
extends Effect {
    private Expression<OfflinePlayer> players;
    private boolean op;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.op = !parseResult.expr.substring(0, 2).equalsIgnoreCase("de");
        return true;
    }

    @Override
    protected void execute(Event e) {
        for (OfflinePlayer p : this.players.getArray(e)) {
            p.setOp(this.op);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.op ? "" : "de") + "op " + this.players.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffOp.class, "[de[-]]op %offlineplayers%");
    }
}

