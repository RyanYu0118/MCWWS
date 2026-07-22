/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="All Operators")
@Description(value={"The list of operators on the server."})
@Example(value="set {_ops::*} to all operators")
@Since(value={"2.7"})
public class ExprOps
extends SimpleExpression<OfflinePlayer> {
    private boolean nonOps;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.nonOps = parseResult.hasTag("non");
        return true;
    }

    protected OfflinePlayer[] get(Event event) {
        if (this.nonOps) {
            ArrayList<Player> nonOpsList = new ArrayList<Player>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp()) continue;
                nonOpsList.add(player);
            }
            return (OfflinePlayer[])nonOpsList.toArray(new Player[0]);
        }
        return Bukkit.getOperators().toArray(new OfflinePlayer[0]);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.nonOps) {
            return null;
        }
        switch (mode) {
            case ADD: 
            case SET: 
            case REMOVE: 
            case RESET: 
            case DELETE: {
                return CollectionUtils.array(OfflinePlayer[].class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null && mode != Changer.ChangeMode.RESET && mode != Changer.ChangeMode.DELETE) {
            return;
        }
        switch (mode) {
            case SET: {
                for (OfflinePlayer player : Bukkit.getOperators()) {
                    player.setOp(false);
                }
            }
            case ADD: {
                for (Object player : delta) {
                    ((OfflinePlayer)player).setOp(true);
                }
                break;
            }
            case REMOVE: {
                for (Object player : delta) {
                    ((OfflinePlayer)player).setOp(false);
                }
                break;
            }
            case RESET: 
            case DELETE: {
                for (OfflinePlayer player : Bukkit.getOperators()) {
                    player.setOp(false);
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.nonOps) {
            return "all non-operators";
        }
        return "all operators";
    }

    static {
        Skript.registerExpression(ExprOps.class, OfflinePlayer.class, ExpressionType.SIMPLE, "[all [[of] the]|the] [server] [:non(-| )]op[erator]s");
    }
}

