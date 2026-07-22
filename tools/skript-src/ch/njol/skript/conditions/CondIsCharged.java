/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Creeper
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Wither
 *  org.bukkit.entity.WitherSkull
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;

@Name(value="Is Charged")
@Description(value={"Checks if a creeper, wither, or wither skull is charged (powered)."})
@Example(value="if the last spawned creeper is charged:\n\tbroadcast \"A charged creeper is at %location of last spawned creeper%\"\n")
@Since(value={"2.5, 2.10 (withers, wither skulls)"})
public class CondIsCharged
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        if (entity instanceof Creeper) {
            Creeper creeper = (Creeper)entity;
            return creeper.isPowered();
        }
        if (entity instanceof WitherSkull) {
            WitherSkull witherSkull = (WitherSkull)entity;
            return witherSkull.isCharged();
        }
        if (entity instanceof Wither) {
            Wither wither = (Wither)entity;
            return wither.isCharged();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "charged";
    }

    static {
        CondIsCharged.register(CondIsCharged.class, "(charged|powered)", "entities");
    }
}

