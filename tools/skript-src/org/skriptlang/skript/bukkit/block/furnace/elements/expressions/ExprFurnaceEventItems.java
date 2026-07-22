/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.FurnaceBurnEvent
 *  org.bukkit.event.inventory.FurnaceExtractEvent
 *  org.bukkit.event.inventory.FurnaceSmeltEvent
 *  org.bukkit.event.inventory.FurnaceStartSmeltEvent
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.block.furnace.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Furnace Event Items")
@Description(value={"Represents the different items in furnace events.", "Only 'smelting item' can be changed."})
@Example.Examples(value={@Example(value="on furnace smelt:\n\tbroadcast smelted item\n\t# Or 'result'\n"), @Example(value="on furnace extract:\n\tbroadcast extracted item\n"), @Example(value="on fuel burn:\n\tbroadcast burned fuel\n"), @Example(value="on smelting start:\n\tbroadcast smelting item\n\tclear smelting item\n")})
@Events(value={"smelt", "fuel burn", "start smelt", "furnace item extract"})
@Since(value={"2.10"})
public class ExprFurnaceEventItems
extends PropertyExpression<Block, ItemStack> {
    private static final FurnaceValues[] FURNACE_VALUES = FurnaceValues.values();
    private FurnaceValues type;

    public static void register(SyntaxRegistry registry) {
        int size = FURNACE_VALUES.length;
        String[] patterns = new String[size];
        for (FurnaceValues value : FURNACE_VALUES) {
            patterns[value.ordinal()] = value.pattern;
        }
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprFurnaceEventItems.class, ItemStack.class).addPatterns(patterns)).supplier(ExprFurnaceEventItems::new)).priority(PropertyExpression.DEFAULT_PRIORITY)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.type = FURNACE_VALUES[matchedPattern];
        if (!this.getParser().isCurrentEvent(this.type.clazz)) {
            Skript.error(this.type.error);
            return false;
        }
        this.setExpr(new EventValueExpression<Block>(Block.class));
        return true;
    }

    protected ItemStack @Nullable [] get(Event event, Block[] source) {
        ItemStack[] itemStackArray = new ItemStack[1];
        itemStackArray[0] = switch (this.type.ordinal()) {
            default -> throw new MatchException(null, null);
            case 2 -> ((FurnaceStartSmeltEvent)event).getSource();
            case 3 -> ((FurnaceBurnEvent)event).getFuel();
            case 0 -> ((FurnaceSmeltEvent)event).getResult();
            case 1 -> {
                FurnaceExtractEvent extractEvent = (FurnaceExtractEvent)event;
                yield new ItemStack(extractEvent.getItemType(), extractEvent.getItemAmount());
            }
        };
        return itemStackArray;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.type != FurnaceValues.SMELTED) {
            return null;
        }
        if (mode != Changer.ChangeMode.SET && mode != Changer.ChangeMode.DELETE) {
            return null;
        }
        return CollectionUtils.array(ItemStack.class);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof FurnaceSmeltEvent)) {
            return;
        }
        FurnaceSmeltEvent smeltEvent = (FurnaceSmeltEvent)event;
        if (mode == Changer.ChangeMode.SET) {
            smeltEvent.setResult((ItemStack)delta[0]);
        } else if (mode == Changer.ChangeMode.DELETE) {
            smeltEvent.setResult(ItemStack.of((Material)Material.AIR));
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.type.toString;
    }

    static enum FurnaceValues {
        SMELTED("(smelted item|result[ item])", "smelted item", FurnaceSmeltEvent.class, "'smelted item' can only be used in a smelting event."),
        EXTRACTED("extracted item[s]", "extracted item", FurnaceExtractEvent.class, "'extracted item' can only be used in a furnace extract event."),
        SMELTING("smelting item", "smelting item", FurnaceStartSmeltEvent.class, "'smelting item' can only be used in a start smelting event"),
        BURNED("burned (fuel|item)", "burned fuel item", FurnaceBurnEvent.class, "'burned fuel' can only be used in a fuel burning event.");

        private String pattern;
        private String error;
        private String toString;
        private Class<? extends Event> clazz;

        private FurnaceValues(String pattern, String toString, Class<? extends Event> clazz, String error) {
            this.pattern = "[the] " + pattern;
            this.clazz = clazz;
            this.error = error;
            this.toString = toString;
        }
    }
}

