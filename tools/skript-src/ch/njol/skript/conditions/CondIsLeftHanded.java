/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.inventory.MainHand
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.MainHand;

@Name(value="Left Handed")
@Description(value={"Checks if living entities or players are left or right-handed. Armor stands are neither right nor left-handed."})
@Example(value="on damage of player:\n\tif victim is left handed:\n\t\tcancel event\n")
@Since(value={"2.8.0"})
public class CondIsLeftHanded
extends PropertyCondition<LivingEntity> {
    private static final boolean CAN_USE_ENTITIES = Skript.methodExists(Mob.class, "isLeftHanded", new Class[0]);
    private MainHand hand;

    @Override
    public boolean init(Expression[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.hand = parseResult.hasTag("left") ? MainHand.LEFT : MainHand.RIGHT;
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        if (CAN_USE_ENTITIES && livingEntity instanceof Mob) {
            Mob mob = (Mob)livingEntity;
            return mob.isLeftHanded() == (this.hand == MainHand.LEFT);
        }
        if (livingEntity instanceof HumanEntity) {
            HumanEntity humanEntity = (HumanEntity)livingEntity;
            return humanEntity.getMainHand() == this.hand;
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return (this.hand == MainHand.LEFT ? "left" : "right") + " handed";
    }

    static {
        if (CAN_USE_ENTITIES) {
            CondIsLeftHanded.register(CondIsLeftHanded.class, "(:left|right)( |-)handed", "livingentities");
        } else {
            CondIsLeftHanded.register(CondIsLeftHanded.class, "(:left|right)( |-)handed", "players");
        }
    }
}

