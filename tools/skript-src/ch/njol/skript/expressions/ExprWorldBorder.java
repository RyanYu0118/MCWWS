/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="World Border")
@Description(value={"Get the border of a world or a player.", "A player's world border is not persistent. Restarts, quitting, death or changing worlds will reset the border."})
@Example(value="set {_border} to world border of player's world")
@Since(value={"2.11"})
public class ExprWorldBorder
extends SimplePropertyExpression<Object, WorldBorder> {
    @Override
    @Nullable
    public WorldBorder convert(Object object) {
        if (object instanceof World) {
            World world = (World)object;
            return world.getWorldBorder();
        }
        if (object instanceof Player) {
            Player player = (Player)object;
            return player.getWorldBorder();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET ? CollectionUtils.array(WorldBorder.class) : null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        F[] objects = this.getExpr().getArray(event);
        if (mode == Changer.ChangeMode.RESET) {
            for (Object object : objects) {
                if (object instanceof World) {
                    World world = (World)object;
                    world.getWorldBorder().reset();
                    continue;
                }
                if (!(object instanceof Player)) continue;
                Player player = (Player)object;
                player.setWorldBorder(null);
            }
            return;
        }
        WorldBorder to = (WorldBorder)delta[0];
        assert (to != null);
        for (Object object : objects) {
            if (object instanceof World) {
                World world = (World)object;
                WorldBorder worldBorder = world.getWorldBorder();
                worldBorder.setCenter(to.getCenter());
                worldBorder.setSize(to.getSize());
                worldBorder.setDamageAmount(to.getDamageAmount());
                worldBorder.setDamageBuffer(to.getDamageBuffer());
                worldBorder.setWarningDistance(to.getWarningDistance());
                worldBorder.setWarningTime(to.getWarningTime());
                continue;
            }
            if (!(object instanceof Player)) continue;
            Player player = (Player)object;
            player.setWorldBorder(to);
        }
    }

    @Override
    public Class<? extends WorldBorder> getReturnType() {
        return WorldBorder.class;
    }

    @Override
    protected String getPropertyName() {
        return "world border";
    }

    static {
        ExprWorldBorder.registerDefault(ExprWorldBorder.class, WorldBorder.class, "world[ ]border", "worlds/players");
    }
}

