/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Player$Spigot
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;

@Name(value="Ping")
@Description(value={"Pings of players, as Minecraft server knows them. Note that they will almost certainly be different from the ones you'd get from using ICMP echo requests. This expression is only supported on some server software (PaperSpigot)."})
@Example(value="command /ping <player=%player%>:\n\ttrigger:\n\t\tsend \"%arg-1%'s ping is %arg-1's ping%\"\n")
@Since(value={"2.2-dev36"})
public class ExprPing
extends SimplePropertyExpression<Player, Long> {
    private static final boolean SUPPORTED = Skript.methodExists(Player.Spigot.class, "getPing", new Class[0]);

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!SUPPORTED) {
            Skript.error("The ping expression is not supported on this server software.");
            return false;
        }
        this.setExpr(exprs[0]);
        return true;
    }

    @Override
    public Long convert(Player player) {
        return player.spigot().getPing();
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "ping";
    }

    static {
        PropertyExpression.register(ExprPing.class, Long.class, "ping", "players");
    }
}

