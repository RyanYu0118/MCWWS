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

@Name(value="Is Blocking")
@Description(value={"Checks whether a player is blocking with their shield."})
@Example(value="on damage of player:\n\tvictim is blocking\n\tdamage attacker by 0.5 hearts\n")
@Since(value={"unknown (before 2.1)"})
public class CondIsBlocking
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return player.isBlocking();
    }

    @Override
    protected String getPropertyName() {
        return "blocking";
    }

    static {
        CondIsBlocking.register(CondIsBlocking.class, "(blocking|defending) [with [a] shield]", "players");
    }
}

