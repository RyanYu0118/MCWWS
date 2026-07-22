/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.BrewEvent
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.brewing.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Brewing Results")
@Description(value={"The resulting items in an 'on brew complete' event."})
@Example(value="on brew complete:\n\tset {_results::*} to the brewing results\n")
@Since(value={"2.13"})
@Events(value={"Brewing Complete"})
public class ExprBrewingResults
extends SimpleExpression<ItemStack>
implements EventRestrictedSyntax {
    private boolean delayed;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprBrewingResults.class, ItemStack.class).addPatterns("[the] brewing results")).supplier(ExprBrewingResults::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.delayed = isDelayed.isTrue();
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(BrewEvent.class);
    }

    protected ItemStack @Nullable [] get(Event event) {
        if (!(event instanceof BrewEvent)) {
            return null;
        }
        BrewEvent brewEvent = (BrewEvent)event;
        return (ItemStack[])brewEvent.getResults().toArray(ItemStack[]::new);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.delayed) {
            Skript.error("The 'brewing results' cannot be changed after the 'brewing complete' event has passed.");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(ItemType[].class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof BrewEvent)) {
            return;
        }
        BrewEvent brewEvent = (BrewEvent)event;
        ArrayList<ItemType> itemTypes = new ArrayList<ItemType>();
        if (delta != null) {
            for (Object object : delta) {
                if (!(object instanceof ItemType)) continue;
                ItemType itemType = (ItemType)object;
                itemTypes.add(itemType);
            }
        }
        List results = brewEvent.getResults();
        switch (mode) {
            case SET: {
                results.clear();
                results.addAll(itemTypes.stream().map(ItemType::getRandom).filter(itemStack -> !Objects.isNull(itemStack)).toList());
                break;
            }
            case DELETE: {
                results.clear();
                break;
            }
            case ADD: {
                results.addAll(itemTypes.stream().map(ItemType::getRandom).filter(itemStack -> !Objects.isNull(itemStack)).toList());
                break;
            }
            case REMOVE: {
                ArrayList copy = new ArrayList(results);
                for (ItemStack itemStack2 : copy) {
                    for (ItemType itemType : itemTypes) {
                        if (!itemType.isOfType(itemStack2)) continue;
                        results.remove(itemStack2);
                    }
                }
                break;
            }
        }
        if (results.size() > 3) {
            this.warning("A brewing stand can only contain 3 items; Some items will be ignored.");
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
        return "the brewing results";
    }
}

