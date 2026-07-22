/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.world.LootGenerateEvent
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import org.bukkit.event.Event;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Loot")
@Description(value={"The loot that will be generated in a 'loot generate' event."})
@Example(value="on loot generate:\n\tchance of %10\n\tadd 64 diamonds to loot\n\tsend \"You hit the jackpot!!\"\n")
@Since(value={"2.7"})
@RequiredPlugins(value={"MC 1.16+"})
public class ExprLoot
extends SimpleExpression<ItemStack> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprLoot.class, ItemStack.class).addPatterns("[the] loot")).supplier(ExprLoot::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)LootGenerateEvent.class)) {
            Skript.error("The 'loot' expression can only be used in a 'loot generate' event");
            return false;
        }
        return true;
    }

    @Nullable
    protected @Nullable ItemStack @Nullable [] get(Event event) {
        if (!(event instanceof LootGenerateEvent)) {
            return new ItemStack[0];
        }
        LootGenerateEvent lootEvent = (LootGenerateEvent)event;
        return lootEvent.getLoot().toArray(new ItemStack[0]);
    }

    @Override
    @Nullable
    public @Nullable Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.DELETE, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET -> CollectionUtils.array(ItemStack[].class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof LootGenerateEvent)) {
            return;
        }
        LootGenerateEvent lootEvent = (LootGenerateEvent)event;
        ArrayList<ItemStack> items = null;
        if (delta != null) {
            items = new ArrayList<ItemStack>(delta.length);
            for (Object item : delta) {
                items.add((ItemStack)item);
            }
        }
        switch (mode) {
            case ADD: {
                lootEvent.getLoot().addAll(items);
                break;
            }
            case REMOVE: {
                lootEvent.getLoot().removeAll(items);
                break;
            }
            case SET: {
                lootEvent.setLoot(items);
                break;
            }
            case DELETE: {
                lootEvent.getLoot().clear();
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the loot";
    }
}

