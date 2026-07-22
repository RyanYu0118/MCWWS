/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityBreedEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.breeding.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityBreedEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Breeding Family")
@Description(value={"Represents family members within a breeding event."})
@Example(value="on breeding:\n\tsend \"When a %breeding mother% and %breeding father% love each other very much, they make a %bred offspring%\" to breeder\n")
@Since(value={"2.10"})
public class ExprBreedingFamily
extends SimpleExpression<LivingEntity> {
    private int pattern;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprBreedingFamily.class, LivingEntity.class).addPatterns("[the] breeding mother", "[the] breeding father", "[the] [bred] (offspring|child)", "[the] breeder")).supplier(ExprBreedingFamily::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)EntityBreedEvent.class)) {
            Skript.error("The 'breeding family' expression can only be used in an breed event.");
            return false;
        }
        this.pattern = matchedPattern;
        return true;
    }

    @Nullable
    protected LivingEntity[] get(Event event) {
        LivingEntity[] livingEntityArray;
        if (!(event instanceof EntityBreedEvent)) {
            return new LivingEntity[0];
        }
        EntityBreedEvent breedEvent = (EntityBreedEvent)event;
        switch (this.pattern) {
            case 0: {
                LivingEntity[] livingEntityArray2 = new LivingEntity[1];
                livingEntityArray = livingEntityArray2;
                livingEntityArray2[0] = breedEvent.getMother();
                break;
            }
            case 1: {
                LivingEntity[] livingEntityArray3 = new LivingEntity[1];
                livingEntityArray = livingEntityArray3;
                livingEntityArray3[0] = breedEvent.getFather();
                break;
            }
            case 2: {
                LivingEntity[] livingEntityArray4 = new LivingEntity[1];
                livingEntityArray = livingEntityArray4;
                livingEntityArray4[0] = breedEvent.getEntity();
                break;
            }
            case 3: {
                LivingEntity[] livingEntityArray5 = new LivingEntity[1];
                livingEntityArray = livingEntityArray5;
                livingEntityArray5[0] = breedEvent.getBreeder();
                break;
            }
            default: {
                livingEntityArray = new LivingEntity[]{};
            }
        }
        return livingEntityArray;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends LivingEntity> getReturnType() {
        return LivingEntity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "breeding family";
    }
}

