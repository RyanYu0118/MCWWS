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

@Name(value="Is Sneaking")
@Description(value={"Checks whether a player is sneaking."})
@Example(value="# prevent mobs from seeing sneaking players if they are at least 4 meters apart\non target:\n\ttarget is sneaking\n\tdistance of target and the entity is bigger than 4\n\tcancel the event\n")
@Since(value={"1.4.4"})
public class CondIsSneaking
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return player.isSneaking();
    }

    @Override
    protected String getPropertyName() {
        return "sneaking";
    }

    static {
        CondIsSneaking.register(CondIsSneaking.class, "sneaking", "players");
    }
}

