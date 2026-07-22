/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.stream.Stream;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Enchantment Level")
@Description(value={"The level of a particular <a href='#enchantment'>enchantment</a> on an item."})
@Example(value="player's tool is a sword of sharpness:\n\tmessage \"You have a sword of sharpness %level of sharpness of the player's tool% equipped\"\n")
@Since(value={"2.0"})
public class ExprEnchantmentLevel
extends SimpleExpression<Long> {
    private Expression<ItemType> items;
    private Expression<Enchantment> enchants;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        int i = matchedPattern < 2 ? 1 : 0;
        this.items = exprs[i];
        this.enchants = exprs[i ^ 1];
        return true;
    }

    protected Long[] get(Event e) {
        Enchantment[] enchantments = this.enchants.getArray(e);
        return (Long[])Stream.of(this.items.getArray(e)).map(ItemType::getEnchantmentTypes).flatMap(Stream::of).filter(enchantment -> CollectionUtils.contains(enchantments, enchantment.getType())).map(EnchantmentType::getLevel).map(i -> (long)i).toArray(Long[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case REMOVE: 
            case ADD: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        ItemType[] itemTypes = this.items.getArray(e);
        Enchantment[] enchantments = this.enchants.getArray(e);
        int changeValue = ((Number)delta[0]).intValue();
        for (ItemType itemType : itemTypes) {
            for (Enchantment enchantment : enchantments) {
                int newItemLevel;
                EnchantmentType enchantmentType = itemType.getEnchantmentType(enchantment);
                int oldLevel = enchantmentType == null ? 0 : enchantmentType.getLevel();
                switch (mode) {
                    case ADD: {
                        newItemLevel = oldLevel + changeValue;
                        break;
                    }
                    case REMOVE: {
                        newItemLevel = oldLevel - changeValue;
                        break;
                    }
                    case SET: {
                        newItemLevel = changeValue;
                        break;
                    }
                    default: {
                        assert (false);
                        return;
                    }
                }
                if (newItemLevel <= 0) {
                    itemType.removeEnchantments(new EnchantmentType(enchantment));
                    continue;
                }
                itemType.addEnchantments(new EnchantmentType(enchantment, newItemLevel));
            }
        }
    }

    @Override
    public boolean isSingle() {
        return this.items.isSingle() && this.enchants.isSingle();
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the level of " + this.enchants.toString(e, debug) + " of " + this.items.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprEnchantmentLevel.class, Long.class, ExpressionType.PROPERTY, "[the] [enchant[ment]] level[s] of %enchantments% (on|of) %itemtypes%", "[the] %enchantments% [enchant[ment]] level[s] (on|of) %itemtypes%", "%itemtypes%'[s] %enchantments% [enchant[ment]] level[s]", "%itemtypes%'[s] [enchant[ment]] level[s] of %enchantments%");
    }
}

