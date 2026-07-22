/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.ClientOption
 *  org.bukkit.entity.Player
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import com.destroystokyo.paper.ClientOption;
import org.bukkit.entity.Player;

@Name(value="Has Chat Filtering")
@Description(value={"Checks whether a player has chat filtering enabled."})
@Example(value="if player doesn't have chat filtering enabled:\n\tsend \"<gray>This server may contain mature chat messages. You have been warned!\" to player\n")
@Since(value={"2.10"})
public class CondChatFiltering
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return (Boolean)player.getClientOption(ClientOption.TEXT_FILTERING_ENABLED);
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "chat filtering enabled";
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.ClientOption")) {
            CondChatFiltering.register(CondChatFiltering.class, PropertyCondition.PropertyType.HAVE, "(chat|text) filtering (on|enabled)", "players");
        }
    }
}

