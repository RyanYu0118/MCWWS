/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Cancel Active Item")
@Description(value={"Interrupts the action entities may be trying to complete.", "For example, interrupting eating, or drawing back a bow."})
@Example(value="on damage of player:\n\tif the victim's active tool is a bow:\n\t\tinterrupt the usage of the player's active item\n")
@Since(value={"2.8.0"})
public class EffCancelItemUse
extends Effect {
    private Expression<LivingEntity> entities;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (LivingEntity entity : this.entities.getArray(event)) {
            entity.clearActiveItem();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "cancel the usage of " + this.entities.toString(event, debug) + "'s active item";
    }

    static {
        if (Skript.methodExists(LivingEntity.class, "clearActiveItem", new Class[0])) {
            Skript.registerEffect(EffCancelItemUse.class, "(cancel|interrupt) [the] us[ag]e of %livingentities%'[s] [active|current] item");
        }
    }
}

