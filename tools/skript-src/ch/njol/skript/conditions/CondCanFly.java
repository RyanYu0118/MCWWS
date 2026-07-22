/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Player;

@Name(value="Can Fly")
@Description(value={"Whether a player is allowed to fly."})
@Example(value="player can fly")
@Since(value={"2.3"})
public class CondCanFly
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return player.getAllowFlight();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.CAN;
    }

    @Override
    protected String getPropertyName() {
        return "fly";
    }

    static {
        CondCanFly.register(CondCanFly.class, PropertyCondition.PropertyType.CAN, "fly", "players");
    }
}

