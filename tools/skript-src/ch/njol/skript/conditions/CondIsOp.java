/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.OfflinePlayer;

@Name(value="Is Operator")
@Description(value={"Checks whether a player is a server operator."})
@Example(value="player is an operator")
@Since(value={"2.7"})
public class CondIsOp
extends PropertyCondition<OfflinePlayer> {
    @Override
    public boolean check(OfflinePlayer player) {
        return player.isOp();
    }

    @Override
    protected String getPropertyName() {
        return "op";
    }

    static {
        CondIsOp.register(CondIsOp.class, "[[a] server|an] op[erator][s]", "offlineplayers");
    }
}

