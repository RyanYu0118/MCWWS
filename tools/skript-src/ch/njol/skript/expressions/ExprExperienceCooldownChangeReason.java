/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerExpCooldownChangeEvent$ChangeReason
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerExpCooldownChangeEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Experience Cooldown Change Reason")
@Description(value={"The <a href='#experiencechangereason'>experience change reason</a> within an <a href='#experience%20cooldown%20change%20event'>experience cooldown change event</a>."})
@Example(value="on player experience cooldown change:\n\tif xp cooldown change reason is plugin:\n\t\t#Changed by a plugin\n\telse if xp cooldown change reason is orb pickup:\n\t\t#Changed by picking up xp orb\n")
@Since(value={"2.10"})
public class ExprExperienceCooldownChangeReason
extends EventValueExpression<PlayerExpCooldownChangeEvent.ChangeReason> {
    public ExprExperienceCooldownChangeReason() {
        super(PlayerExpCooldownChangeEvent.ChangeReason.class);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "experience cooldown change reason";
    }

    static {
        ExprExperienceCooldownChangeReason.register(ExprExperienceCooldownChangeReason.class, PlayerExpCooldownChangeEvent.ChangeReason.class, "(experience|[e]xp) cooldown change (reason|cause|type)");
    }
}

