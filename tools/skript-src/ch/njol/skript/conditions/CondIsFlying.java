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

@Name(value="Is Flying")
@Description(value={"Checks whether a player is flying."})
@Example(value="player is not flying")
@Since(value={"1.4.4"})
public class CondIsFlying
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return player.isFlying();
    }

    @Override
    protected String getPropertyName() {
        return "flying";
    }

    static {
        CondIsFlying.register(CondIsFlying.class, "flying", "players");
    }
}

