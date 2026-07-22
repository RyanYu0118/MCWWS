/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EquipmentSlot
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Hand Raised")
@Description(value={"Checks whether an entity has one or both of their hands raised.", "Hands are raised when an entity is using an item (eg: blocking, drawing a bow, eating)."})
@Example(value="on damage of player:\n\tif victim's main hand is raised:\n\t\tdrop player's tool at player\n\t\tset player's tool to air\n")
@Since(value={"2.8.0"})
public class CondIsHandRaised
extends Condition {
    private Expression<LivingEntity> entities;
    @Nullable
    private EquipmentSlot hand;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.setNegated(matchedPattern % 2 == 1);
        if (matchedPattern >= 4) {
            this.hand = EquipmentSlot.OFF_HAND;
        } else if (parseResult.hasTag("main")) {
            this.hand = EquipmentSlot.HAND;
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.entities.check(event, livingEntity -> livingEntity.isHandRaised() && (this.hand == null || livingEntity.getHandRaised().equals((Object)this.hand)), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.entities.toString(event, debug) + "'s  " + (this.hand == null ? "" : (this.hand == EquipmentSlot.HAND ? "main " : "off ")) + "hand" + (this.entities.isSingle() ? " is" : "s are") + (this.isNegated() ? " not " : "") + " raised";
    }

    static {
        Skript.registerCondition(CondIsHandRaised.class, "%livingentities%'[s] [:main] hand[s] (is|are) raised", "%livingentities%'[s] [:main] hand[s] (isn't|is not|aren't|are not) raised", "[:main] hand[s] of %livingentities% (is|are) raised", "[:main] hand[s] of %livingentities% (isn't|is not|aren't|are not) raised", "%livingentities%'[s] off[ |-]hand[s] (is|are) raised", "%livingentities%'[s] off[ |-]hand[s] (isn't|is not|aren't|are not) raised", "off[ |-]hand[s] of %livingentities% (is|are) raised", "off[ |-]hand[s] of %livingentities% (isn't|is not|aren't|are not) raised");
    }
}

