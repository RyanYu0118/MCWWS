/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Enchanted")
@Description(value={"Checks whether an item is enchanted. Enchants must match the exact level by default, unless 'or better' or 'or worse' are used."})
@Example.Examples(value={@Example(value="tool of the player is enchanted with efficiency 2"), @Example(value="if player's helmet or player's boots are enchanted with protection 3 or better:"), @Example(value="if player's chestplate is enchanted with protection")})
@Since(value={"1.4.6, 2.12 ('or better')"})
public class CondIsEnchanted
extends Condition {
    private Expression<ItemType> items;
    @Nullable
    private Expression<EnchantmentType> enchs;
    private Comparison comparison;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.items = exprs[0];
        this.enchs = exprs[1];
        this.comparison = Comparison.values()[parseResult.mark];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.enchs != null) {
            EnchantmentType[] enchantments = this.enchs.getAll(event);
            boolean and = this.enchs.getAnd();
            return this.items.check(event, item -> switch (this.comparison.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> item.hasExactEnchantments(and, enchantments);
                case 2 -> item.hasEnchantmentsOrWorse(and, enchantments);
                case 1 -> item.hasEnchantmentsOrBetter(and, enchantments);
            }, this.isNegated());
        }
        return this.items.check(event, ItemType::hasEnchantments, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, this.items, "enchanted" + (String)(this.enchs == null ? "" : " with " + this.enchs.toString(event, debug)) + this.comparison.toSkriptString());
    }

    static {
        PropertyCondition.register(CondIsEnchanted.class, "enchanted [with %-enchantmenttypes% [or (1:(better|greater|higher|above)|2:(worse|lesser|lower|below))]]", "itemtypes");
    }

    private static enum Comparison {
        EXACT(""),
        AT_LEAST(" or better"),
        AT_MOST(" or worse");

        private final String toString;

        private Comparison(String toString) {
            this.toString = toString;
        }

        public String toSkriptString() {
            return this.toString;
        }
    }
}

