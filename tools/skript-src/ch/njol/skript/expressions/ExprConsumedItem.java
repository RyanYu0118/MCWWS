/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityShootBowEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Consumed Item")
@Description(value={"Represents the item consumed within an entity shoot bow and item consume event."})
@Example.Examples(value={@Example(value="on player or skeleton shoot projectile:\n\tif the consumed item is an arrow:\n\t\tcancel event\n\t\tsend \"You're now allowed to shoot your arrows.\" to shooter\n"), @Example(value="on player consume:\n\tif the consumed item is cooked porkchop:\n\t\tsend \"Well aren't you just a little piggy wiggly!\" to player\n\tif player has scoreboard tag \"vegetarian\":\n\t\tset the consumed item to a carrot\n")})
@Since(value={"2.11"})
public class ExprConsumedItem
extends SimpleExpression<ItemStack>
implements EventRestrictedSyntax {
    private boolean allowsSettingConsumedItem;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.allowsSettingConsumedItem = this.getParser().isCurrentEvent((Class<? extends Event>)PlayerItemConsumeEvent.class);
        return true;
    }

    protected ItemStack @Nullable [] get(Event event) {
        if (event instanceof EntityShootBowEvent) {
            EntityShootBowEvent shootBowEvent = (EntityShootBowEvent)event;
            return new ItemStack[]{shootBowEvent.getConsumable()};
        }
        if (event instanceof PlayerItemConsumeEvent) {
            PlayerItemConsumeEvent consumeEvent = (PlayerItemConsumeEvent)event;
            return new ItemStack[]{consumeEvent.getItem()};
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!this.allowsSettingConsumedItem) {
            Skript.error("You may only set the consumed item in a player consume item event.");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET -> CollectionUtils.array(ItemStack.class);
            case Changer.ChangeMode.DELETE -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        PlayerItemConsumeEvent consumeEvent;
        block3: {
            block2: {
                if (!(event instanceof PlayerItemConsumeEvent)) break block2;
                consumeEvent = (PlayerItemConsumeEvent)event;
                if (this.allowsSettingConsumedItem) break block3;
            }
            return;
        }
        consumeEvent.setItem(delta == null ? null : (ItemStack)delta[0]);
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityShootBowEvent.class, PlayerItemConsumeEvent.class);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the consumed item";
    }

    static {
        Skript.registerExpression(ExprConsumedItem.class, ItemStack.class, ExpressionType.SIMPLE, "[the] consumed item");
    }
}

