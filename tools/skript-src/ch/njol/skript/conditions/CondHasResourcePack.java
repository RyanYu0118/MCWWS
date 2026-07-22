/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Player;

@Name(value="Has Resource Pack")
@Description(value={"Checks whether the given players have a server resource pack loaded. Please note that this can't detect player's own resource pack, only the resource pack that sent by the server."})
@Example(value="if the player has a resource pack loaded:")
@Since(value={"2.4"})
public class CondHasResourcePack
extends PropertyCondition<Player> {
    @Override
    public boolean check(Player player) {
        return player.hasResourcePack();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "resource pack loaded";
    }

    static {
        if (Skript.methodExists(Player.class, "hasResourcePack", new Class[0])) {
            CondHasResourcePack.register(CondHasResourcePack.class, PropertyCondition.PropertyType.HAVE, "[a] resource pack [(loaded|installed)]", "players");
        }
    }
}

