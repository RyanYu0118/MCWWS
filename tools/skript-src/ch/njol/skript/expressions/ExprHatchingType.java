/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.EntityType
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerEggThrowEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Hatching Entity Type")
@Description(value={"The type of the entity that will be hatched in a Player Egg Throw event."})
@Example(value="on player egg throw:\n\tset the hatching entity type to a primed tnt\n")
@Events(value={"Egg Throw"})
@Since(value={"2.7"})
public class ExprHatchingType
extends SimpleExpression<EntityData<?>>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerEggThrowEvent.class);
    }

    @Nullable
    protected EntityData<?>[] get(Event event) {
        if (!(event instanceof PlayerEggThrowEvent)) {
            return new EntityData[0];
        }
        return new EntityData[]{EntityUtils.toSkriptEntityData(((PlayerEggThrowEvent)event).getHatchingType())};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
            return CollectionUtils.array(EntityData.class);
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        EntityType entityType;
        if (!(event instanceof PlayerEggThrowEvent)) {
            return;
        }
        EntityType entityType2 = entityType = delta != null ? EntityUtils.toBukkitEntityType((EntityData)delta[0]) : EntityType.CHICKEN;
        if (!entityType.isSpawnable()) {
            return;
        }
        ((PlayerEggThrowEvent)event).setHatchingType(entityType);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends EntityData<?>> getReturnType() {
        return EntityData.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the hatching entity type";
    }

    static {
        Skript.registerExpression(ExprHatchingType.class, EntityData.class, ExpressionType.SIMPLE, "[the] hatching entity [type]");
    }
}

