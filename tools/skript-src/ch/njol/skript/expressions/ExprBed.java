/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Bed")
@Description(value={"Returns the bed location of a player, i.e. the spawn point of a player if they ever slept in a bed and the bed still exists and is unobstructed however, you can set the unsafe bed location of players and they will respawn there even if it has been obstructed or doesn't exist anymore and that's the default behavior of this expression otherwise you will need to be specific i.e. <code>safe bed location</code>.", "", "NOTE: Offline players can not have their bed location changed, only online players."})
@Example.Examples(value={@Example(value="if bed of player exists:\n\tteleport player the the player's bed\nelse:\n\tteleport the player to the world's spawn point\n"), @Example(value="set the bed location of player to spawn location of world(\"world\") # unsafe/invalid bed location\nset the safe bed location of player to spawn location of world(\"world\") # safe/valid bed location\n")})
@Since(value={"2.0, 2.7 (offlineplayers, safe bed)"})
public class ExprBed
extends SimplePropertyExpression<OfflinePlayer, Location> {
    private boolean isSafe;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isSafe = parseResult.hasTag("safe");
        this.setExpr(exprs[0]);
        return true;
    }

    @Override
    @Nullable
    public Location convert(OfflinePlayer p) {
        return p.getBedSpawnLocation();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE ? CollectionUtils.array(Location.class) : null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Location loc = delta == null ? null : (Location)delta[0];
        for (OfflinePlayer p : (OfflinePlayer[])this.getExpr().getArray(e)) {
            Player op = p.getPlayer();
            if (op == null) continue;
            op.setBedSpawnLocation(loc, !this.isSafe);
        }
    }

    @Override
    protected String getPropertyName() {
        return "bed";
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    static {
        ExprBed.register(ExprBed.class, Location.class, "[(safe:(safe|valid)|(unsafe|invalid))] bed[s] [location[s]]", "offlineplayers");
    }
}

