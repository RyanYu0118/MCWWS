/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Enderman
 *  org.bukkit.entity.Goat
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;

@Name(value="Is Screaming")
@Description(value={"Check whether a goat or enderman is screaming."})
@Example.Examples(value={@Example(value="\tif last spawned goat is not screaming:\n\t\tmake last spawned goat scream\n"), @Example(value="\tif {_enderman} is screaming:\n\t\tforce {_enderman} to stop screaming\n")})
@Since(value={"2.11"})
public class CondIsScreaming
extends PropertyCondition<LivingEntity> {
    private static final boolean SUPPORTS_ENDERMAN = Skript.methodExists(Enderman.class, "isScreaming", new Class[0]);

    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Goat) {
            Goat goat = (Goat)entity;
            return goat.isScreaming();
        }
        if (SUPPORTS_ENDERMAN && entity instanceof Enderman) {
            Enderman enderman = (Enderman)entity;
            return enderman.isScreaming();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "screaming";
    }

    static {
        CondIsScreaming.register(CondIsScreaming.class, "screaming", "livingentities");
    }
}

