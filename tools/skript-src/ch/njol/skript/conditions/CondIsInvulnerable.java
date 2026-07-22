/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;

@Name(value="Is Invulnerable")
@Description(value={"Checks whether an entity or a gamemode is invulnerable.\nFor gamemodes, Paper and Minecraft 1.20.6 are required"})
@Example.Examples(value={@Example(value="target entity is invulnerable"), @Example(value="loop all gamemodes:\n\tif loop-value is not invulnerable:\n\t\tbroadcast \"the gamemode %loop-value% is vulnerable!\"\n")})
@Since(value={"2.5, 2.10 (gamemode)"})
public class CondIsInvulnerable
extends PropertyCondition<Object> {
    private static final boolean SUPPORTS_GAMEMODE = Skript.methodExists(GameMode.class, "isInvulnerable", new Class[0]);

    @Override
    public boolean check(Object object) {
        if (object instanceof Entity) {
            Entity entity = (Entity)object;
            return entity.isInvulnerable();
        }
        if (SUPPORTS_GAMEMODE && object instanceof GameMode) {
            GameMode gameMode = (GameMode)object;
            return gameMode.isInvulnerable();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "invulnerable";
    }

    static {
        CondIsInvulnerable.register(CondIsInvulnerable.class, "(invulnerable|invincible)", "entities" + (SUPPORTS_GAMEMODE ? "/gamemodes" : ""));
    }
}

