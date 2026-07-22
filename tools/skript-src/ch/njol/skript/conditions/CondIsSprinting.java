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

@Name(value="Is Sprinting")
@Description(value={"Checks whether a player is sprinting."})
@Example(value="player is not sprinting")
@Since(value={"1.4.4"})
public class CondIsSprinting
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return player.isSprinting();
    }

    @Override
    protected String getPropertyName() {
        return "sprinting";
    }

    static {
        CondIsSprinting.register(CondIsSprinting.class, "sprinting", "players");
    }
}

