/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.BrewingStand
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.brewing.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Brewing Stand Fuel Level")
@Description(value={"The fuel level of a brewing stand. The fuel level is decreased by one at the start of brewing each potion."})
@Example(value="set the brewing stand fuel level of {_block} to 10\nclear the brewing stand fuel level of {_block}\n")
@Since(value={"2.13"})
public class ExprBrewingFuelLevel
extends SimplePropertyExpression<Block, Integer> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprBrewingFuelLevel.infoBuilder(ExprBrewingFuelLevel.class, Integer.class, "brewing [stand] fuel (level|amount)", "blocks", true).supplier(ExprBrewingFuelLevel::new)).build());
    }

    @Override
    @Nullable
    public Integer convert(Block block) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof BrewingStand)) {
            return null;
        }
        BrewingStand brewingStand = (BrewingStand)blockState;
        return brewingStand.getFuelLevel();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int providedValue;
        int n = providedValue = delta != null ? ((Number)delta[0]).intValue() : 0;
        if (mode == Changer.ChangeMode.SET) {
            providedValue = Math2.fit(0, providedValue, Integer.MAX_VALUE);
        }
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof BrewingStand)) continue;
            BrewingStand brewingStand = (BrewingStand)blockState;
            int newValue = providedValue;
            int current = brewingStand.getFuelLevel();
            if (mode == Changer.ChangeMode.ADD) {
                newValue = Math2.fit(0, current + newValue, Integer.MAX_VALUE);
            } else if (mode == Changer.ChangeMode.REMOVE) {
                newValue = Math2.fit(0, current - newValue, Integer.MAX_VALUE);
            }
            brewingStand.setFuelLevel(newValue);
            brewingStand.update(true, false);
        }
    }

    @Override
    public Class<Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "brewing stand fuel level";
    }
}

