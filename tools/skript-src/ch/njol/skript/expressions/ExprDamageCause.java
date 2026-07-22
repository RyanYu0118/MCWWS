/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.registrations.EventValues;
import org.bukkit.event.entity.EntityDamageEvent;

@Name(value="Damage Cause")
@Description(value={"The <a href='#damagecause'>damage cause</a> of a damage event. Please click on the link for more information."})
@Example(value="damage cause is lava, fire or burning")
@Since(value={"2.0"})
public class ExprDamageCause
extends EventValueExpression<EntityDamageEvent.DamageCause> {
    public ExprDamageCause() {
        super(EntityDamageEvent.DamageCause.class);
    }

    @Override
    public boolean setTime(int time) {
        return time != EventValues.TIME_FUTURE;
    }

    static {
        ExprDamageCause.register(ExprDamageCause.class, EntityDamageEvent.DamageCause.class, "damage cause");
    }
}

