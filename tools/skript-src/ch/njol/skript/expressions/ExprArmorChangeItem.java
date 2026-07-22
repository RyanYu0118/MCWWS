/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Armor Change Item")
@Description(value={"Get the unequipped or equipped armor item from a 'armor change' event."})
@Example(value="on armor change\n\tbroadcast the old armor item\n")
@Events(value={"Armor Change"})
@Since(value={"2.11"})
public class ExprArmorChangeItem
extends EventValueExpression<ItemStack>
implements EventRestrictedSyntax {
    private boolean oldArmor;

    public ExprArmorChangeItem() {
        super(ItemStack.class);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.oldArmor = matchedPattern == 0;
        super.setTime(this.oldArmor ? EventValues.TIME_PAST : EventValues.TIME_FUTURE);
        return super.init(exprs, matchedPattern, isDelayed, parser);
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerArmorChangeEvent.class);
    }

    @Override
    public boolean setTime(int time) {
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.oldArmor ? "old armor item" : "new armor item";
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.event.player.PlayerArmorChangeEvent")) {
            ExprArmorChangeItem.register(ExprArmorChangeItem.class, ItemStack.class, "(old|unequipped) armo[u]r item", "(new|equipped) armo[u]r item");
        }
    }
}

