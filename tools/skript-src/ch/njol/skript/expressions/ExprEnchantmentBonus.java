/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.enchantment.PrepareItemEnchantEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Enchantment Bonus")
@Description(value={"The enchantment bonus in an enchant prepare event. This represents the number of bookshelves affecting/surrounding the enchantment table."})
@Example(value="on enchant:\n\tsend \"There are %enchantment bonus% bookshelves surrounding this enchantment table!\" to player\n")
@Events(value={"enchant prepare"})
@Since(value={"2.5"})
public class ExprEnchantmentBonus
extends SimpleExpression<Long>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PrepareItemEnchantEvent.class);
    }

    @Nullable
    protected Long[] get(Event e) {
        return new Long[]{((PrepareItemEnchantEvent)e).getEnchantmentBonus()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "enchantment bonus";
    }

    static {
        Skript.registerExpression(ExprEnchantmentBonus.class, Long.class, ExpressionType.SIMPLE, "[the] enchantment bonus");
    }
}

