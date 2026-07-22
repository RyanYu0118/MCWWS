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

@Name(value="Can See Chat Colors")
@Description(value={"Checks whether a player can see chat colors."})
@Example(value="if player can see chat colors:\n\tsend \"Find the red word in <red>this<reset> message.\"\nelse:\n\tsend \"You cannot partake in finding the colored word.\"\n")
@Since(value={"2.10"})
public class CondChatColors
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return (Boolean)player.getClientOption(ClientOption.CHAT_COLORS_ENABLED);
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.CAN;
    }

    @Override
    protected String getPropertyName() {
        return "see chat colors";
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.ClientOption")) {
            CondChatColors.register(CondChatColors.class, PropertyCondition.PropertyType.CAN, "see chat colo[u]r[s|ing]", "players");
        }
    }
}

