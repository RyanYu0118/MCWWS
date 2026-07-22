/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemFactory
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Enchant/Disenchant")
@Description(value={"Enchant or disenchant an existing item. Enchanting at a specific level will act as if an enchanting table was used, and will apply the enchantments randomly chosen at that level. Treasure enchantments, like mending, can optionally be allowed. Note that enchanting a book at a specific level will turn it into an enchanted book, rather than a book with enchantments."})
@Example.Examples(value={@Example(value="enchant the player's tool with sharpness 5"), @Example(value="enchant the player's tool at level 30"), @Example(value="disenchant the player's tool")})
@Since(value={"2.0, 2.13 (at level)"})
public class EffEnchant
extends Effect {
    private static final Patterns<Operation> patterns = new Patterns(new Object[][]{{"enchant %~itemtypes% with %enchantmenttypes%", Operation.ENCHANT}, {"[naturally|randomly] enchant %~itemtypes% at level %number%[treasure:[,] allowing treasure enchant[ment]s]", Operation.ENCHANT_AT_LEVEL}, {"disenchant %~itemtypes%", Operation.DISENCHANT}});
    private Expression<ItemType> items;
    private Expression<EnchantmentType> enchantments;
    private Expression<Number> level;
    private boolean treasure;
    private Operation operation;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.items = exprs[0];
        if (!Changer.ChangerUtils.acceptsChange(this.items, Changer.ChangeMode.SET, ItemStack.class)) {
            Skript.error(String.valueOf(this.items) + " cannot be changed, thus it cannot be (dis)enchanted");
            return false;
        }
        if (matchedPattern == 0) {
            this.enchantments = exprs[1];
        } else if (matchedPattern == 1) {
            this.level = exprs[1];
            this.treasure = parseResult.hasTag("treasure");
        }
        this.operation = patterns.getInfo(matchedPattern);
        return true;
    }

    @Override
    protected void execute(Event event) {
        Function<ItemType, ItemType> changeFunction;
        switch (this.operation.ordinal()) {
            case 0: {
                EnchantmentType[] types = this.enchantments.getArray(event);
                if (types.length == 0) {
                    return;
                }
                changeFunction = item -> {
                    item.addEnchantments(types);
                    return item;
                };
                break;
            }
            case 1: {
                Number levelValue = this.level.getSingle(event);
                if (levelValue == null || levelValue.intValue() < 0) {
                    return;
                }
                ItemFactory factory = Bukkit.getItemFactory();
                changeFunction = item -> {
                    ItemStack itemstack = item.getRandom();
                    if (itemstack == null) {
                        return item;
                    }
                    itemstack = factory.enchantWithLevels(itemstack, levelValue.intValue(), this.treasure, (Random)ThreadLocalRandom.current());
                    return new ItemType(itemstack);
                };
                break;
            }
            case 2: {
                changeFunction = item -> {
                    item.clearEnchantments();
                    return item;
                };
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected operation: " + String.valueOf((Object)this.operation));
            }
        }
        this.items.changeInPlace(event, changeFunction);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (this.operation.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "enchant " + this.items.toString(event, debug) + " with " + this.enchantments.toString(event, debug);
            case 1 -> "enchant " + this.items.toString(event, debug) + " at level " + this.level.toString(event, debug) + (this.treasure ? " allowing treasure enchantments" : "");
            case 2 -> "disenchant " + this.items.toString(event, debug);
        };
    }

    static {
        Skript.registerEffect(EffEnchant.class, patterns.getPatterns());
    }

    private static enum Operation {
        ENCHANT,
        ENCHANT_AT_LEVEL,
        DISENCHANT;

    }
}

