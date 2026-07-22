/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Axolotl
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.LivingEntity;

@Name(value="Is Playing Dead")
@Description(value={"Checks to see if an axolotl is playing dead."})
@Example(value="if last spawned axolotl is playing dead:\n\tmake last spawned axolotl stop playing dead\n")
@Since(value={"2.11"})
public class CondIsPlayingDead
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        boolean bl;
        if (entity instanceof Axolotl) {
            Axolotl axolotl = (Axolotl)entity;
            bl = axolotl.isPlayingDead();
        } else {
            bl = false;
        }
        return bl;
    }

    @Override
    protected String getPropertyName() {
        return "playing dead";
    }

    static {
        PropertyCondition.register(CondIsPlayingDead.class, PropertyCondition.PropertyType.BE, "playing dead", "livingentities");
    }
}

