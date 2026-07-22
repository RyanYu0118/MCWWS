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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Last Death Location")
@Description(value={"Gets the last death location of a player, or offline player, if available.", "Can also be set, reset, and deleted if the player is online."})
@Example.Examples(value={@Example(value="set {_loc} to the last death location of player"), @Example(value="teleport player to last death location of (random element out of all players)")})
@Since(value={"2.10"})
public class ExprLastDeathLocation
extends SimplePropertyExpression<OfflinePlayer, Location> {
    @Override
    @Nullable
    public Location convert(OfflinePlayer offlinePlayer) {
        Location location;
        if (offlinePlayer instanceof Player) {
            Player player = (Player)offlinePlayer;
            location = player.getLastDeathLocation();
        } else {
            location = offlinePlayer.getLastDeathLocation();
        }
        return location;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Location.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Location location;
        Object object;
        Location loc = delta != null && (object = delta[0]) instanceof Location ? (location = (Location)object) : null;
        for (OfflinePlayer offlinePlayer : (OfflinePlayer[])this.getExpr().getArray(event)) {
            if (!(offlinePlayer instanceof Player)) continue;
            Player player = (Player)offlinePlayer;
            player.setLastDeathLocation(loc);
        }
    }

    @Override
    protected String getPropertyName() {
        return "last death location";
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    static {
        ExprLastDeathLocation.register(ExprLastDeathLocation.class, Location.class, "[last] death location[s]", "offlineplayers");
    }
}

