/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.entity.EntityEnterLoveModeEvent
 */
package org.skriptlang.skript.bukkit.breeding;

import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.breeding.elements.conditions.CondCanAge;
import org.skriptlang.skript.bukkit.breeding.elements.conditions.CondCanBreed;
import org.skriptlang.skript.bukkit.breeding.elements.conditions.CondIsAdult;
import org.skriptlang.skript.bukkit.breeding.elements.conditions.CondIsBaby;
import org.skriptlang.skript.bukkit.breeding.elements.conditions.CondIsInLove;
import org.skriptlang.skript.bukkit.breeding.elements.effects.EffAllowAging;
import org.skriptlang.skript.bukkit.breeding.elements.effects.EffBreedable;
import org.skriptlang.skript.bukkit.breeding.elements.effects.EffMakeAdultOrBaby;
import org.skriptlang.skript.bukkit.breeding.elements.events.EvtBreed;
import org.skriptlang.skript.bukkit.breeding.elements.expressions.ExprBreedingFamily;
import org.skriptlang.skript.bukkit.breeding.elements.expressions.ExprLoveTime;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

public class BreedingModule
extends HierarchicalAddonModule {
    public BreedingModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, CondCanAge::register, CondCanBreed::register, CondIsAdult::register, CondIsBaby::register, CondIsInLove::register, EffAllowAging::register, EffBreedable::register, EffMakeAdultOrBaby::register, EvtBreed::register, ExprBreedingFamily::register, ExprLoveTime::register);
        this.moduleRegistry(addon).register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(SimpleEvent.class, "Love Mode Enter").addEvent(EntityEnterLoveModeEvent.class).addPatterns("[entity] enter[s] love mode", "[entity] love mode [enter]")).addDescription("Called whenever an entity enters a state of being in love.").addExample("on love mode enter:\n\tcancel event # No one is allowed love here\n").addSince("2.10").build());
        EventValues.registerEventValue(EntityEnterLoveModeEvent.class, LivingEntity.class, EntityEnterLoveModeEvent::getEntity);
        EventValues.registerEventValue(EntityEnterLoveModeEvent.class, HumanEntity.class, EntityEnterLoveModeEvent::getHumanEntity);
    }

    @Override
    public String name() {
        return "breeding";
    }
}

