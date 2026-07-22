/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityTameEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTameEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Tamer")
@Description(value={"The tamer of an entity. Can only be used in entity tame events. You can use 'event-entity' to refer tamed entity itself."})
@Example(value="on tame:\n\tif the tamer is a player:\n\t\tsend \"someone tamed something!\" to console\n")
@Since(value={"2.2-dev25"})
public class ExprTamer
extends SimpleExpression<Player>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityTameEvent.class);
    }

    protected Player[] get(Event e) {
        if (!(e instanceof EntityTameEvent)) {
            return null;
        }
        return new Player[]{((EntityTameEvent)e).getOwner() instanceof Player ? (Player)((EntityTameEvent)e).getOwner() : null};
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the tamer";
    }

    static {
        Skript.registerExpression(ExprTamer.class, Player.class, ExpressionType.SIMPLE, "[the] tamer");
    }
}

