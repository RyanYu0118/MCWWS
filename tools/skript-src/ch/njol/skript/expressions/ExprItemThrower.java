/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.UUIDUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.util.coll.CollectionUtils;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Dropped Item Thrower")
@Description(value={"The uuid of the entity or player that threw/dropped the dropped item."})
@Example.Examples(value={@Example(value="set the uuid of the dropped item thrower of {_dropped item} to player\nif the uuid of the dropped item thrower of {_dropped item} is uuid of player:\n"), @Example(value="clear the item thrower of {_dropped item}")})
@Since(value={"2.11"})
public class ExprItemThrower
extends SimplePropertyExpression<Item, UUID> {
    @Override
    @Nullable
    public UUID convert(Item item) {
        return item.getThrower();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return CollectionUtils.array(OfflinePlayer.class, Entity.class, UUID.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        UUID newId = null;
        if (delta != null) {
            newId = UUIDUtils.asUUID(delta[0]);
        }
        for (Item item : (Item[])this.getExpr().getArray(event)) {
            item.setThrower(newId);
        }
    }

    @Override
    public Class<UUID> getReturnType() {
        return UUID.class;
    }

    @Override
    protected String getPropertyName() {
        return "uuid of the dropped item thrower";
    }

    static {
        Skript.registerExpression(ExprItemThrower.class, UUID.class, ExpressionType.PROPERTY, "[the] uuid of [the] [dropped] item thrower [of %itementities%]", "[the] [dropped] item thrower's uuid [of %itementities%]");
    }
}

