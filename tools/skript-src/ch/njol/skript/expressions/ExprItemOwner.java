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

@Name(value="Dropped Item Owner")
@Description(value={"The uuid of the owner of the dropped item.\nSetting the owner of a dropped item means only that entity or player can pick it up.\nDropping an item does not automatically make the entity or player the owner.\n"})
@Example(value="\tset the uuid of the dropped item owner of last dropped item to player\n\tif the uuid of the dropped item owner of last dropped item is uuid of player:\n")
@Since(value={"2.11"})
public class ExprItemOwner
extends SimplePropertyExpression<Item, UUID> {
    @Override
    @Nullable
    public UUID convert(Item item) {
        return item.getOwner();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return CollectionUtils.array(Entity.class, OfflinePlayer.class, UUID.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        UUID uuid = delta == null ? null : UUIDUtils.asUUID(delta[0]);
        for (Item item : (Item[])this.getExpr().getArray(event)) {
            item.setOwner(uuid);
        }
    }

    @Override
    public Class<? extends UUID> getReturnType() {
        return UUID.class;
    }

    @Override
    protected String getPropertyName() {
        return "uuid of the dropped item owner";
    }

    static {
        Skript.registerExpression(ExprItemOwner.class, UUID.class, ExpressionType.PROPERTY, "[the] uuid of [the] [dropped] item owner [of %itementities%]", "[the] [dropped] item owner's uuid [of %itementities%]");
    }
}

